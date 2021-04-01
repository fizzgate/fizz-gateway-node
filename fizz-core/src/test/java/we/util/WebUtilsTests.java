package we.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class WebUtilsTests {

    @Test
    void getClientReqPathPrefixTest() throws JsonProcessingException {
        WebUtils.setGatewayPrefix("/_proxytestx");
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/_proxytest/xservice/ybiz?a=b").build();
        MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);
        String clientService = WebUtils.getClientService(mockExchange);
        assertEquals("xservice", clientService);
        String clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);
        String clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/_proxytest/", clientReqPathPrefix);

        WebUtils.setGatewayPrefix("/prox");
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/prox/test/ybiz").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("test", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);

        WebUtils.setGatewayPrefix("");
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/aservice/ybiz1").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("aservice", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz1", clientReqPath);
    }
}
