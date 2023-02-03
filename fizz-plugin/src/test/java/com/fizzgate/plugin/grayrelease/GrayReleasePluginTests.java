package com.fizzgate.plugin.grayrelease;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fizzgate.filter.AggregateFilter;
import com.fizzgate.filter.FilterResult;
import com.fizzgate.fizz.ConfigLoader;
import com.fizzgate.plugin.FizzPluginFilterChain;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.plugin.grayrelease.GrayReleasePlugin;
import com.fizzgate.proxy.Route;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ReflectionUtils;
import com.fizzgate.util.WebUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GrayReleasePluginTests {

    /**
     * service discovery backend
     */
    @Test
    public void simpleTest() {
        final Route[] changedRoute = new Route[1];
        WebTestClient client = WebTestClient.bindToWebHandler(
                                                    exchange -> {
                                                        ServerHttpResponse r = exchange.getResponse();
                                                        r.setStatusCode(HttpStatus.OK);
                                                        r.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                                                        return r.writeWith(Mono.just(r.bufferFactory().wrap("this is web handler response".getBytes())));
                                                    }
                                            )
                                            .webFilter(
                                                    (exchange, chain) -> {

                                                        GrayReleasePlugin grayReleasePlugin = new GrayReleasePlugin();
                                                        Map<String, Object> config = new HashMap<>();
                                                        config.put("triggerCondition", "     method == 'post'               " +
                                                                                       " and matches('path','^/apath/x*')   " +
                                                                                       " and clientIpInRange('11.238.145.180', '11.238.145.182') " +
                                                                                       " and exist('body.tools.gun') ");
                                                        config.put("routeType", Integer.parseInt(String.valueOf(ApiConfig.Type.SERVICE_DISCOVERY)));
                                                        config.put("routeConfig",
                                                                                    "type        : http       \n " +
                                                                                    "serviceName : bservice   \n " +
                                                                                    "path        : /bpath/{$1}   ");

                                                        // exchange.getAttributes().put("pcsit@", Collections.emptyIterator());
                                                        Route route = new Route().path("/apath/**");
                                                        changedRoute[0] = route;
                                                        exchange.getAttributes().put(WebUtils.ROUTE, route);
                                                        exchange.getAttributes().put(WebUtils.IGNORE_PLUGIN, Consts.S.EMPTY);
                                                        exchange.getAttributes().put(FizzPluginFilterChain.WEB_FILTER_CHAIN, chain);
                                                        exchange.getAttributes().put("oi@", "11.238.145.181");

                                                        return grayReleasePlugin.filter(exchange, config);
                                                    }
                                            )
                                            .build();

        client.post()
              .uri("/proxy/aservice/apath/xxx")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue("{\"user\":\"henry\",\"tools\":{\"gun\":\"ak\"}}")
              .exchange()
              .expectBody(String.class).value(
                        v -> {
                            // System.err.println("body:\n" + v);
                        }
              );

        Assertions.assertEquals("bservice",   changedRoute[0].backendService);
        Assertions.assertEquals("/bpath/xxx", changedRoute[0].backendPath);
    }

    @Test
    public void reverseProxyBackendTest() {

        final Route[] changedRoute = new Route[1];

        Map<String, Object> config = new HashMap<>();
        config.put("triggerCondition", " method == 'get' ");
        config.put("routeType", Integer.parseInt(String.valueOf(ApiConfig.Type.REVERSE_PROXY)));
        config.put("routeConfig",
                                    "serviceName : http://1.2.3.4:8080,http://1.2.3.5:8080   \n " +
                                    "path        : /a/b/c   \n" +
                                    "query       : name1=value1&name2=value2   ");

        WebTestClient client = WebTestClient.bindToWebHandler(
                                                    exchange -> {
                                                        ServerHttpResponse r = exchange.getResponse();
                                                        r.setStatusCode(HttpStatus.OK);
                                                        r.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                                                        return r.writeWith(Mono.just(r.bufferFactory().wrap("this is web handler response".getBytes())));
                                                    }
                                            )
                                            .webFilter(
                                                    (exchange, chain) -> {

                                                        GrayReleasePlugin grayReleasePlugin = new GrayReleasePlugin();

                                                        Route route = new Route().path("/apath/**");
                                                        changedRoute[0] = route;
                                                        exchange.getAttributes().put(WebUtils.ROUTE, route);
                                                        exchange.getAttributes().put(WebUtils.IGNORE_PLUGIN, Consts.S.EMPTY);
                                                        exchange.getAttributes().put(FizzPluginFilterChain.WEB_FILTER_CHAIN, chain);
                                                        exchange.getAttributes().put("oi@", "11.238.145.181");

                                                        return grayReleasePlugin.filter(exchange, config);
                                                    }
                                            )
                                            .build();

        client.get()
              .uri("/proxy/aservice/apath/xxx")
              .exchange();
        Assertions.assertEquals("/a/b/c?name1=value1&name2=value2",   changedRoute[0].getBackendPathQuery());
        Assertions.assertEquals("http://1.2.3.4:8080",   changedRoute[0].nextHttpHostPort);

        client.get()
              .uri("/proxy/aservice/apath/xxx")
              .exchange();
        Assertions.assertEquals("http://1.2.3.5:8080",   changedRoute[0].nextHttpHostPort);

        client.get()
              .uri("/proxy/aservice/apath/xxx")
              .exchange();
        Assertions.assertEquals("http://1.2.3.4:8080",   changedRoute[0].nextHttpHostPort);
    }

    @Test
    public void aggregateBackendTest() {
        AggregateFilter aggregateFilter = new AggregateFilter();
        ConfigLoader configLoader = mock(ConfigLoader.class);
        when(
                configLoader.matchAggregateResource("GET", "/_proxytest/bservice/bpath/xxx")
        )
        .thenReturn(null);
        ReflectionUtils.set(aggregateFilter, "configLoader", configLoader);

        WebTestClient client = WebTestClient.bindToWebHandler(
                                                    exchange -> {
                                                        ServerHttpResponse r = exchange.getResponse();
                                                        r.setStatusCode(HttpStatus.OK);
                                                        return r.writeWith(Mono.just(r.bufferFactory().wrap("this is web handler response".getBytes())));
                                                    }
                                            )
                                            .webFilter(
                                                    (exchange, chain) -> {

                                                        GrayReleasePlugin grayReleasePlugin = new GrayReleasePlugin();
                                                        Map<String, Object> config = new HashMap<>();
                                                        config.put("triggerCondition", " method == 'get' ");
                                                        config.put("routeType", Integer.parseInt(String.valueOf(ApiConfig.Type.SERVICE_AGGREGATE)));
                                                        config.put("routeConfig",
                                                                                    "type        : http       \n " +
                                                                                    "serviceName : bservice   \n " +
                                                                                    "path        : /bpath/{$1}   ");

                                                        Route route = new Route().path("/apath/**");
                                                        exchange.getAttributes().put(WebUtils.ROUTE, route);
                                                        exchange.getAttributes().put(WebUtils.IGNORE_PLUGIN, Consts.S.EMPTY);
                                                        exchange.getAttributes().put(FizzPluginFilterChain.WEB_FILTER_CHAIN, chain);
                                                        exchange.getAttributes().put("oi@", "11.238.145.181");

                                                        Map<String, Object> filterContext = new HashMap<>();
                                                        exchange.getAttributes().put(WebUtils.FILTER_CONTEXT, filterContext);
                                                        filterContext.put(WebUtils.PREV_FILTER_RESULT, FilterResult.SUCCESS("x"));

                                                        return grayReleasePlugin.filter(exchange, config);
                                                    },
                                                    aggregateFilter
                                            )
                                            .build();

        client.get()
              .uri("/_proxytest/aservice/apath/xxx")
              .exchange()
              .expectBody(String.class).value(
                  v -> {
                      Map<String, Object> bodyMap = JacksonUtils.readValue(v, new TypeReference<Map<String, Object>>(){});
                      Assertions.assertEquals(bodyMap.get("message"), "API not found in aggregation: /_proxytest/bservice/bpath/xxx");
                  }
              );
    }
}
