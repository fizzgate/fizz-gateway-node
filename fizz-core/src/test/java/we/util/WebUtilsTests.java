/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
