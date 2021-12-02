package we.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;
import we.Fizz;
import we.FizzAppContext;
import we.config.SystemConfig;
import we.filter.PreprocessFilter;
import we.plugin.auth.*;
import we.plugin.stat.StatPluginFilter;
import we.plugin.stat.StatPluginFilterProperties;
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
        StatPluginFilterProperties statPluginFilterProperties = new StatPluginFilterProperties();
        statPluginFilterProperties.setStatOpen(false);
        ReflectionUtils.set(statPluginFilter, "statPluginFilterProperties", statPluginFilterProperties);

        preprocessFilter = new PreprocessFilter();

        apiConfigService = new ApiConfigService();
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setAggregateTestAuth(false);
        ReflectionUtils.set(apiConfigService, "systemConfig", systemConfig);

        ApiConfigServiceProperties apiConfigServiceProperties = new ApiConfigServiceProperties();
        apiConfigServiceProperties.setNeedAuth(false);
        ReflectionUtils.set(apiConfigService, "apiConfigServiceProperties", apiConfigServiceProperties);

        GatewayGroupService gatewayGroupService = new GatewayGroupService();
        ReflectionUtils.set(apiConfigService, "gatewayGroupService", gatewayGroupService);

        ReflectionUtils.set(preprocessFilter, "statPluginFilter", statPluginFilter);
        ReflectionUtils.set(authPluginFilter, "apiConfigService", apiConfigService);
        ReflectionUtils.set(preprocessFilter, "authPluginFilter", authPluginFilter);
        ReflectionUtils.set(preprocessFilter, "gatewayGroupService", gatewayGroupService);

        Fizz.context = mock(ConfigurableApplicationContext.class);
        FizzAppContext.appContext = Fizz.context;
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
                            return Mono.empty();
                        }
                );
            }
        };

        when(Fizz.context.getBean(plugin, FizzPluginFilter.class)).thenReturn(fizzPlugin);
        when(Fizz.context.getBean(plugin0, FizzPluginFilter.class)).thenReturn(fizzPlugin0);
        when(FizzAppContext.appContext.getBeansOfType(FixedPluginFilter.class)).thenReturn(Collections.emptyMap());

        ApiConfig apiConfig = new ApiConfig();
        apiConfig.service           = "xservice";
        apiConfig.path              = "/ypath";
        apiConfig.backendPath       = apiConfig.path;
        apiConfig.fizzMethod        = HttpMethod.GET;
        apiConfig.firstGatewayGroup = GatewayGroup.DEFAULT;
        apiConfig.pluginConfigs     = new ArrayList<>();

        PluginConfig pc = new PluginConfig();
        pc.plugin       = "fizzPlugin";
        apiConfig.pluginConfigs.add(pc);

        PluginConfig pc0 = new PluginConfig();
        pc0.plugin       = "fizzPlugin0";
        apiConfig.pluginConfigs.add(pc0);

        apiConfigService.updateServiceConfigMap(apiConfig, apiConfigService.serviceConfigMap);

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
                .webFilter(new WebFilter() {
                    @Override
                    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
                        exchange.getAttributes().put("oi@", "6.6.6.6");
                        return chain.filter(exchange);
                    }
                })
                .webFilter(preprocessFilter)
                .build();

        WebTestClient.ResponseSpec exchange = client.get().uri("/proxy/xservice/ypath").exchange();
        exchange.expectHeader().valueEquals("22bb", "xx");
    }
}
