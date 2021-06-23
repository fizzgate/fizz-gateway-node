package we.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Mono;
import we.controller.FlowControlController;
import we.stats.FlowStat;
import we.stats.ResourceTimeWindowStat;
import we.stats.TimeWindowStat;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.JacksonUtils;
import we.util.ReflectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class FlowControlFilterTests {

    @Test
    void flowControlControllerTest() throws InterruptedException {
        WebTestClient client = WebTestClient.bindToController(new FlowControlController()).build();
        client.get().uri("/admin/flowStat/globalConcurrentsRps")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/json;charset=UTF-8")
                    // .expectBody().json("{\"concurrents\":0, \"rps\":0}")
                    .expectBody(String.class).value(
                                                    v -> {
                                                        HashMap<String, Integer> m = JacksonUtils.readValue(v, HashMap.class);
                                                        assertEquals(m.get("concurrents"), 0);
                                                    }
                                             )
                    ;
        Thread.sleep(3000);
    }

    // @Test
    void flowControlFilterTest() throws NoSuchFieldException, InterruptedException {

        FlowControlFilter filter = new FlowControlFilter();
        FlowControlFilterProperties flowControlFilterProperties = new FlowControlFilterProperties();
        ReflectionUtils.set(flowControlFilterProperties, "flowControl", true);
        ReflectionUtils.set(filter, "flowControlFilterProperties", flowControlFilterProperties);

        FlowStat flowStat = new FlowStat();
        ReflectionUtils.set(filter, "flowStat", flowStat);

        ResourceRateLimitConfigService resourceRateLimitConfigService = new ResourceRateLimitConfigService();
        Map<String, ResourceRateLimitConfig> map = resourceRateLimitConfigService.getResourceRateLimitConfigMap();

        ResourceRateLimitConfig config = JacksonUtils.readValue("{\"concurrents\":66,\"enable\":1,\"id\":1,\"isDeleted\":0,\"resource\":\"_global\",\"type\":1}", ResourceRateLimitConfig.class);
        map.put(ResourceRateLimitConfig.NODE_RESOURCE, config);

        config = JacksonUtils.readValue("{\"concurrents\":33,\"enable\":1,\"id\":2,\"isDeleted\":0, \"service\":\"xservice\", \"path\":\"/ypath\", \"type\":4}", ResourceRateLimitConfig.class);
        map.put(config.getResourceId(), config);

        ReflectionUtils.set(filter, "resourceRateLimitConfigService", resourceRateLimitConfigService);

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
                                            .webFilter(filter)
                                            .build();

        client.get().uri("/proxy/xservice/ypath").exchange();
        Thread.sleep(1000);
        long currentTimeSlot = flowStat.currentTimeSlotId();
        long startTimeSlot = currentTimeSlot - 10 * 1000;

        // System.err.println(JacksonUtils.writeValueAsString(flowStat.resourceStats));

        String xservice = ResourceRateLimitConfig.buildResourceId(null, null, null, "xservice", null);
        List<ResourceTimeWindowStat> resourceTimeWindowStats = flowStat.getResourceTimeWindowStats(xservice, startTimeSlot, currentTimeSlot, 10);
        TimeWindowStat win = resourceTimeWindowStats.get(0).getWindows().get(0);
        assertEquals(win.getCompReqs(), 1);

        String xserviceYpath = ResourceRateLimitConfig.buildResourceId(null, null, null, "xservice", "/ypath");
        resourceTimeWindowStats = flowStat.getResourceTimeWindowStats(xserviceYpath, startTimeSlot, currentTimeSlot, 10);
        win = resourceTimeWindowStats.get(0).getWindows().get(0);
        assertEquals(win.getCompReqs(), 1);
    }
}
