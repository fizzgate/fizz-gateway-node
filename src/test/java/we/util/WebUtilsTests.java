package we.util;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import we.proxy.CallbackReplayReq;
import we.proxy.ServiceInstance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class WebUtilsTests {

    @Test
    void getClientReqPathPrefixTest() throws JsonProcessingException {
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/proxytest/xservice/ybiz").build();
        MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);
        String clientService = WebUtils.getClientService(mockExchange);
        assertEquals(clientService, "xservice");
        String clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals(clientReqPathPrefix, "/proxytest/");

        MockServerHttpRequest mr = MockServerHttpRequest.get("http://127.0.0.1:8600/proxytest/test/ybiz").build();
        MockServerWebExchange me = MockServerWebExchange.from(mr);
        String cs = WebUtils.getClientService(me);
        // System.err.println(cs);
        String crpp = WebUtils.getClientReqPathPrefix(me);
        // System.err.println(crpp);


        // HttpHeaders httpHeaders = new HttpHeaders();
        // httpHeaders.add("h0", "v0");
        // List<String> values = Arrays.asList("v11", "v12");
        // httpHeaders.addAll("h1", values);
        //
        // String s = JSON.toJSONString(JSON.toJSONString(httpHeaders));
        // System.err.println("s: " + s);
        // Map<String, List<String>> m = (Map<String, List<String>>) JSON.parse(JSON.parse(s).toString());
        // System.err.println("m: " + m);


        // ServiceInstance si1 = new ServiceInstance("127", 80);
        // ServiceInstance si2 = new ServiceInstance("128", 90);
        // Map<String, ServiceInstance> receivers = new HashMap<>();
        // receivers.put("s1", si1);
        // receivers.put("s2", si2);
        // String receiversStr = JSON.toJSONString(JSON.toJSONString(receivers));
        // System.err.println("receivers: " + receiversStr);
        // CallbackReplayReq req = new CallbackReplayReq();
        // req.setReceivers(receiversStr);
        // System.err.println("s2 ip: " + req.receivers.get("s2").ip);
        //
        // String x = JSON.parseObject(receiversStr, String.class);
        // System.err.println("x: " + x);
    }
}
