/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;
import we.FizzAppContext;
import we.filter.PreprocessFilter;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.AuthPluginFilter;
import we.plugin.stat.StatPluginFilter;
import we.util.ReactorUtils;
import we.util.ReflectionUtils;
import we.util.WebUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author hongqiaowei
 */

public class PluginTests {

    StatPluginFilter statPluginFilter;

    AuthPluginFilter authPluginFilter;

    PreprocessFilter preprocessFilter;

    ApiConfigService apiConfigService;

    // @BeforeEach
    void beforeEach() {
        authPluginFilter = new AuthPluginFilter();
        statPluginFilter = new StatPluginFilter();
        preprocessFilter = new PreprocessFilter();
        apiConfigService = new ApiConfigService();
        ReflectionUtils.set(preprocessFilter, "statPluginFilter", statPluginFilter);
        ReflectionUtils.set(authPluginFilter, "apiConfigService", apiConfigService);
        ReflectionUtils.set(preprocessFilter, "authPluginFilter", authPluginFilter);

        FizzAppContext.appContext = mock(ConfigurableApplicationContext.class);
    }

    // @Test
    void legacyPluginFilterTest() {

        String plugin = "legacyPlugin";
        PluginFilter legacyPlugin = new PluginFilter() {
            @Override
            public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {
                return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, plugin, Collections.singletonMap("123", "456"));
            }
        };

        String plugin0 = "legacyPlugin0";
        PluginFilter legacyPlugin0 = new PluginFilter() {
            @Override
            public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {
                String v = (String) WebUtils.getFilterResultDataItem(exchange, plugin, "123");
                return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, plugin0, Collections.singletonMap(v, "789"));
            }
        };

        when(FizzAppContext.appContext.getBean(plugin, FizzPluginFilter.class)).thenReturn(legacyPlugin);
        when(FizzAppContext.appContext.getBean(plugin0, FizzPluginFilter.class)).thenReturn(legacyPlugin0);

        WebTestClient client = WebTestClient
                .bindToWebHandler(
                        new WebHandler() {
                            @Override
                            public Mono<Void> handle(ServerWebExchange exchange) {
                                ServerHttpResponse resp = exchange.getResponse();
                                resp.setStatusCode(HttpStatus.OK);
                                HttpHeaders headers = resp.getHeaders();
                                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                                String v = (String) WebUtils.getFilterResultDataItem(exchange, plugin0, "456");
                                headers.add(v, "abc");
                                return resp.writeWith(Mono.just(resp.bufferFactory().wrap("server response".getBytes())));
                            }
                        }
                )
                .webFilter(preprocessFilter)
                .build();

        WebTestClient.ResponseSpec exchange = client.get().uri("/proxy/xservice/ypath").exchange();
        exchange.expectHeader().valueEquals("789", "abc");
    }

    // @Test
    void fizzPluginFilterTest() {
        String plugin = "fizzPlugin";
        FizzPluginFilter fizzPlugin = new FizzPluginFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
                exchange.getAttributes().put("11", "22");
                Mono next = FizzPluginFilterChain.next(exchange);
                return next.defaultIfEmpty(ReactorUtils.NULL).flatMap(
                        v -> {
                            return Mono.empty();
                        }
                );
            }
        };

        String plugin0 = "fizzPlugin0";
        FizzPluginFilter fizzPlugin0 = new FizzPluginFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
                exchange.getAttributes().put("aa", "bb");
                Mono next = FizzPluginFilterChain.next(exchange);
                return next.defaultIfEmpty(ReactorUtils.NULL).flatMap(
                        v -> {
                            String val = (String) exchange.getAttributes().get("11");
                            System.err.println(val + " === ");
                            return Mono.empty();
                        }
                );
            }
        };

        when(FizzAppContext.appContext.getBean(plugin, FizzPluginFilter.class)).thenReturn(fizzPlugin);
        when(FizzAppContext.appContext.getBean(plugin0, FizzPluginFilter.class)).thenReturn(fizzPlugin0);

        WebTestClient client = WebTestClient
                .bindToWebHandler(
                        new WebHandler() {
                            @Override
                            public Mono<Void> handle(ServerWebExchange exchange) {
                                ServerHttpResponse resp = exchange.getResponse();
                                resp.setStatusCode(HttpStatus.OK);
                                HttpHeaders headers = resp.getHeaders();
                                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                                String v1 = exchange.getAttribute("11");
                                String v2 = exchange.getAttribute("aa");
                                headers.add(v1 + v2, "xx");
                                return resp.writeWith(Mono.just(resp.bufferFactory().wrap("server response".getBytes())));
                            }
                        }
                )
                .webFilter(preprocessFilter)
                .build();

        WebTestClient.ResponseSpec exchange = client.get().uri("/proxy/xservice/ypath").exchange();
        exchange.expectHeader().valueEquals("22bb", "xx");
    }

    // @Test
    void legacyPluginFilter_Mix_FizzPluginFilterTest() {
        if (true) {
            ApiConfig ac = new ApiConfig();
            ac.type = ApiConfig.Type.SERVICE_DISCOVERY;
            ac.service = "xservice";
            ac.backendService = "xservice";
            ac.path = "/ypath";
            ac.backendPath = "/ypath";
            ac.pluginConfigs = new ArrayList<>();

            // PluginConfig pc = new PluginConfig();
            // pc.plugin = "legacyPlugin";
            // ac.pluginConfigs.add(pc);
            // PluginConfig pc0 = new PluginConfig();
            // pc0.plugin = "legacyPlugin0";
            // ac.pluginConfigs.add(pc0);

            PluginConfig pc = new PluginConfig();
            pc.plugin = "fizzPlugin";
            ac.pluginConfigs.add(pc);
            PluginConfig pc0 = new PluginConfig();
            pc0.plugin = "fizzPlugin0";
            ac.pluginConfigs.add(pc0);

            // return Mono.just(ac);
        }
    }
}
