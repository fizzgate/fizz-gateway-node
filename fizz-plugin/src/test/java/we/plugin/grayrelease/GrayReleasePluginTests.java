package we.plugin.grayrelease;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import we.plugin.FizzPluginFilterChain;
import we.proxy.Route;
import we.util.Consts;
import we.util.WebUtils;

import java.util.HashMap;
import java.util.Map;

public class GrayReleasePluginTests {

    @Test
    public void simpleTest() {
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
                                                        config.put("routeType", 2);
                                                        config.put("routeConfig",
                                                                                    "type        : http       \n " +
                                                                                    "serviceName : bservice   \n " +
                                                                                    "path        : /bpath/{$1}   ");

                                                        // exchange.getAttributes().put("pcsit@", Collections.emptyIterator());
                                                        Route route = new Route().path("/apath/**");
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
                            System.err.println("body:\n" + v);
                        }
              );
    }
}
