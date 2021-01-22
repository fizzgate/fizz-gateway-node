package we.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class WebUtilsTests {

    @Test
    void getClientReqPathPrefixTest() {
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/proxytest/xservice/ybiz").build();
        MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);
        String clientService = WebUtils.getClientService(mockExchange);
        assertEquals(clientService, "xservice");
        String clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals(clientReqPathPrefix, "/proxytest/");

        MockServerHttpRequest mr = MockServerHttpRequest.get("http://127.0.0.1:8600/proxytest/test/ybiz").build();
        MockServerWebExchange me = MockServerWebExchange.from(mr);
        String cs = WebUtils.getClientService(me);
        System.err.println(cs);
        String crpp = WebUtils.getClientReqPathPrefix(me);
        System.err.println(crpp);
    }
}
