package com.fizzgate.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fizzgate.util.WebUtils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/_proxytestx/test/ybiz").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("test", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/_proxytestx/", clientReqPathPrefix);

        WebUtils.setGatewayPrefix("/prox");
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/prox/test/ybiz").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("test", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/prox/", clientReqPathPrefix);
        
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/_proxytest/xservice/ybiz?a=b").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("xservice", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/_proxytest/", clientReqPathPrefix);

        WebUtils.setGatewayPrefix("");
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/aservice/ybiz1").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("aservice", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz1", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/", clientReqPathPrefix);
        
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/_proxytest/xservice/ybiz?a=b").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("xservice", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/_proxytest/", clientReqPathPrefix);
        
        WebUtils.setGatewayPrefix("/");
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/aservice/ybiz1").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("aservice", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz1", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/", clientReqPathPrefix);
        
        mockRequest = MockServerHttpRequest.get("http://127.0.0.1:8600/_proxytest/xservice/ybiz?a=b").build();
        mockExchange = MockServerWebExchange.from(mockRequest);
        clientService = WebUtils.getClientService(mockExchange);
        assertEquals("xservice", clientService);
        clientReqPath = WebUtils.getClientReqPath(mockExchange);
        assertEquals("/ybiz", clientReqPath);
        clientReqPathPrefix = WebUtils.getClientReqPathPrefix(mockExchange);
        assertEquals("/_proxytest/", clientReqPathPrefix);
    }

    @Test
    void toQueryStringTest() {
        MultiValueMap<String, String> mvm = new LinkedMultiValueMap<>();
        List<String> v0 = Stream.of("", "v0").collect(Collectors.toList());
        v0.add(null);
        mvm.put("k0", v0);
        List<String> v1 = Stream.of("v1").collect(Collectors.toList());
        mvm.put("k1", v1);
        String s = WebUtils.toQueryString(mvm);
        assertEquals("k0=&k0=v0&k0&k1=v1", s);
    }
}
