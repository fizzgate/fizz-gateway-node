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

package we.proxy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.ProxyWebClientConfig;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.util.*;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * @author hongqiaowei
 */

@Service
public class FizzWebClient {

    private static final Logger log = LoggerFactory.getLogger(FizzWebClient.class);

    private static final String localhost    = "localhost";

    private static final String host         = "Host";

    @Resource
    private SystemConfig systemConfig;

    @Resource
    private DiscoveryClientUriSelector discoveryClientUriSelector;

    @Resource(name = ProxyWebClientConfig.proxyWebClient)
    private WebClient proxyWebClient;

    public Mono<ClientResponse> send(String reqId, HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body) {
        return send(reqId, method, uriOrSvc, headers, body, 0);
    }

    public Mono<ClientResponse> send(String reqId, HttpMethod method, String uriOrSvc, HttpHeaders headers, Object body, long timeout) {
        String s = extractServiceOrAddress(uriOrSvc);
        if (isService(s)) {
            String path = uriOrSvc.substring(uriOrSvc.indexOf(Constants.Symbol.FORWARD_SLASH, 10));
            return send2service(reqId, method, s, path, headers, body, timeout);
        } else {
            return send2uri(reqId, method, uriOrSvc, headers, body, timeout);
        }
    }

    public Mono<ClientResponse> send2service(@Nullable String clientReqId, HttpMethod method, String service, String relativeUri,
                                             @Nullable HttpHeaders headers, @Nullable Object body) {

        return send2service(clientReqId, method, service, relativeUri, headers, body, 0);
    }

    public Mono<ClientResponse> send2service(@Nullable String clientReqId, HttpMethod method, String service, String relativeUri,
                                             @Nullable HttpHeaders headers, @Nullable Object body, long timeout) {

        String uri = discoveryClientUriSelector.getNextUri(service, relativeUri);
        return send2uri(clientReqId, method, uri, headers, body, timeout);
    }

    public Mono<ClientResponse> send2uri(@Nullable String clientReqId, HttpMethod method, String uri, @Nullable HttpHeaders headers, @Nullable Object body) {
        return send2uri(clientReqId, method, uri, headers, body, 0);
    }

    public Mono<ClientResponse> send2uri(@Nullable String clientReqId, HttpMethod method, String uri, @Nullable HttpHeaders headers, @Nullable Object body, long timeout) {

        if (log.isDebugEnabled()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            WebUtils.request2stringBuilder(clientReqId, method, uri, headers, null, b);
            log.debug(b.toString(), LogService.BIZ_ID, clientReqId);
        }

        WebClient.RequestBodySpec req = proxyWebClient.method(method).uri(uri).headers(
                hdrs -> {
                    if (headers != null) {
                        headers.forEach(
                                (h, vs) -> {
                                    hdrs.addAll(h, vs);
                                }
                        );
                    }
                    setHostHeader(uri, hdrs);
                }
        );

        boolean b = false;
        DataBuffer d = null;
        if (body != null) {
			if (body instanceof BodyInserter) {
				req.body((BodyInserter) body);
			} else if (body instanceof Flux) {
				Flux<DataBuffer> db = (Flux<DataBuffer>) body;
				req.body(BodyInserters.fromDataBuffers(db));
			} else {
                if (body instanceof ConvertedRequestBodyDataBufferWrapper) {
                    d = ((ConvertedRequestBodyDataBufferWrapper) body).body;
                    body = d;
                    b = true;
                }
				req.bodyValue(body);
			}
        }
        boolean finalB = b;
        DataBuffer finalD = d;

        Mono<ClientResponse> cr = req.exchange();
        if (timeout == 0) {
            if (systemConfig.getRouteTimeout() != 0) {
                timeout = systemConfig.getRouteTimeout();
            }
        }
        if (timeout != 0) {
            cr = cr.timeout(Duration.ofMillis(timeout));
        }

        return cr.doFinally(
                    s -> {
                        if (finalB) {
                            boolean release = NettyDataBufferUtils.release(clientReqId, finalD);
                            if (log.isDebugEnabled()) {
                                log.debug("release converted request body databuffer " + release, LogService.BIZ_ID, clientReqId);
                            }
                        }
                    }
        );
    }

    private void setHostHeader(String uri, HttpHeaders headers) {
        boolean domain = false;
        int begin = uri.indexOf(Constants.Symbol.FORWARD_SLASH) + 2;
        int end = uri.indexOf(Constants.Symbol.FORWARD_SLASH, begin);
        for (int i = begin; i < end; i++) {
            char c = uri.charAt(i);
            if (  (47 < c && c < 58) || c == Constants.Symbol.DOT || c == Constants.Symbol.COLON  ) {
            } else {
                domain = true;
                break;
            }
        }
        if (domain) {
            List<String> lst = Collections.singletonList(uri.substring(begin, end));
            headers.put(host, lst);
        }
    }

    public String extractServiceOrAddress(String uriOrSvc) {
        char c4 = uriOrSvc.charAt(4);
        int start = 7, end = uriOrSvc.length();
        if (c4 == 's' || c4 == 'S') {
            start = 8;
        }
        int i = uriOrSvc.indexOf(Constants.Symbol.FORWARD_SLASH, 9);
        if (i > 0) {
            end = i;
        }
        return uriOrSvc.substring(start, end);
    }

    private boolean isService(String s) {
        if (StringUtils.indexOfAny(s, Constants.Symbol.DOT, Constants.Symbol.COLON) > 0
                || StringUtils.startsWith(s, localhost)) {
            return false;
        } else {
            return true;
        }
    }

}
