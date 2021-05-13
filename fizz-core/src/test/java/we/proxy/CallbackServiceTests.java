package we.proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import reactor.core.publisher.Mono;
import we.fizz.AggregateService;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.CallbackConfig;
import we.plugin.auth.Receiver;
import we.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author hongqiaowei
 */

public class CallbackServiceTests {

    FizzWebClient    mockFizzWebClient;

    AggregateService mockAggregateService;

    ApiConfigService mockApiConfigService;

    CallbackService  callbackService;

    @BeforeEach
    void beforeEach() {
        mockFizzWebClient    = mock(FizzWebClient.class);
        mockAggregateService = mock(AggregateService.class);
        mockApiConfigService = mock(ApiConfigService.class);
        callbackService      = new CallbackService();

        ReflectionUtils.set(callbackService, "fizzWebClient",    mockFizzWebClient);
        ReflectionUtils.set(callbackService, "aggregateService", mockAggregateService);
        ReflectionUtils.set(callbackService, "apiConfigService", mockApiConfigService);
    }

    @Test
    void requestBackendsTest() throws InterruptedException {
        MockServerHttpRequest request = MockServerHttpRequest.get("http://127.0.0.1:8600/proxy/xservice/ybiz").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add("h1", "v1");
        DataBuffer body = null;

        CallbackConfig callbackConfig = new CallbackConfig();
        callbackConfig.receivers = new ArrayList<>();
        Receiver r1 = new Receiver();
        r1.service = "s1";
        r1.type = ApiConfig.Type.SERVICE_DISCOVERY;
        r1.path = "p1";
        callbackConfig.receivers.add(r1);
        Receiver r2 = new Receiver();
        r2.service = "s2";
        r2.type = ApiConfig.Type.SERVICE_DISCOVERY;
        r2.path = "p2";
        callbackConfig.receivers.add(r2);

        ServerHttpRequest req = exchange.getRequest();
        String reqId = req.getId();

        when(mockFizzWebClient.proxySend2service(reqId, HttpMethod.GET, "s1", "p1", headers, body))
        .thenReturn(
                Mono.just(
                        ClientResponse.create(HttpStatus.GONE, ExchangeStrategies.withDefaults())
                                      .header("FIZZ-RSV", "s1-rsv-value")
                                      .body("s1 resp")
                                      .build()
                )
        );

        when(mockFizzWebClient.proxySend2service(reqId, HttpMethod.GET, "s2", "p2", headers, body))
        .thenReturn(
                Mono.just(
                        ClientResponse.create(HttpStatus.FOUND, ExchangeStrategies.withDefaults())
                                      .header("FIZZ-RSV", "s2-rsv-value")
                                      .body("s2 resp")
                                      .build()
                )
        );

        Mono<Void> vm = callbackService.requestBackends(exchange, headers, body, callbackConfig, Collections.EMPTY_MAP);
        vm.subscribe();
        Thread.sleep(2000);

        // if test pass, there will be a '{request id} response 410 GONE' log in console
    }
}
