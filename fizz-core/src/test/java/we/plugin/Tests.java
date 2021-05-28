package we.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;
import we.filter.PreprocessFilter;
import we.plugin.auth.AuthPluginFilter;
import we.plugin.stat.StatPluginFilter;
import we.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author hongqiaowei
 */

public class Tests {

    StatPluginFilter statPluginFilter;

    AuthPluginFilter authPluginFilter;

    PreprocessFilter preprocessFilter;

    @BeforeEach
    void beforeEach() {
        authPluginFilter = new AuthPluginFilter();
        statPluginFilter = new StatPluginFilter();
        preprocessFilter = new PreprocessFilter();
        ReflectionUtils.set(preprocessFilter, "statPluginFilter", statPluginFilter);
        ReflectionUtils.set(preprocessFilter, "authPluginFilter", authPluginFilter);
    }

    @Test
    void legacyPluginFilterTest() {

        WebTestClient client = WebTestClient.bindToWebHandler(
                new WebHandler() {
                    @Override
                    public Mono<Void> handle(ServerWebExchange exchange) {
                        ServerHttpResponse resp = exchange.getResponse();
                        resp.setStatusCode(HttpStatus.OK);
                        resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                        return resp.writeWith(Mono.just(resp.bufferFactory().wrap("{\"hello\":\"world\"}".getBytes())));
                    }
                }
        )

        .webFilter(preprocessFilter)
        .build();



        client.get().uri("/proxy/xservice/ypath").exchange();
        // Thread.sleep(1000);
        // long currentTimeSlot = flowStat.currentTimeSlotId();
        // long startTimeSlot = currentTimeSlot - 10 * 1000;
        // List<ResourceTimeWindowStat> resourceTimeWindowStats = flowStat.getResourceTimeWindowStats("xservice", startTimeSlot, currentTimeSlot, 10);
        // TimeWindowStat win = resourceTimeWindowStats.get(0).getWindows().get(0);
        // assertEquals(win.getCompReqs(), 1);
    }

    @Test
    void fizzPluginFilterTest() {

    }

    @Test
    void legacyPluginFilter_Mix_FizzPluginFilterTest() {

    }
}
