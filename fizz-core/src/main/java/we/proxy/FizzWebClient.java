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

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
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
import we.flume.clients.log4j2appender.LogService;
import we.util.Constants;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author hongqiaowei
 */

@Service
public class FizzWebClient {

    private static final Logger log = LoggerFactory.getLogger(FizzWebClient.class);

    private static final String aggrSend     = "$aggrSend";

    private static final String localhost    = "localhost";

    private static final String host         = "Host";

    @Resource
    private DiscoveryClientUriSelector discoveryClientUriSelector;

    @Resource(name = ProxyWebClientConfig.proxyWebClient)
    private WebClient proxyWebClient;

    // @Resource(name = AggrWebClientConfig.aggrWebClient)
    // private WebClient aggrWebClient;

    @NacosValue(value = "${fizz-web-client.timeout:-1}")
    @Value("${fizz-web-client.timeout:-1}")
    private long timeout = -1;

    @PostConstruct
    public void afterPropertiesSet() {
        if (timeout != -1) {
            CallBackendConfig.DEFAULT.timeout = timeout;
        }
        log.info("fizz web client timeout is " + CallBackendConfig.DEFAULT.timeout);
    }

    public Mono<ClientResponse> aggrSend(String aggrService, HttpMethod aggrMethod, String aggrPath, @Nullable String originReqIdOrBizId,
                                         HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body, @Nullable Long timeout) {

        // ThreadContext.set(aggrSend, Constants.Symbol.EMPTY); // TODO will be remove in future
        CallBackendConfig cbc = null;
        // if (timeout != null) {
        //     cbc = new CallBackendConfig(timeout);
        // }
        return aggrResolveAddressSend(aggrService, aggrMethod, aggrPath, originReqIdOrBizId, method, uriOrSvc, headers, body, cbc);
    }

    public Mono<ClientResponse> aggrSend(String aggrService, HttpMethod aggrMethod, String aggrPath, @Nullable String originReqIdOrBizId,
                                         HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body) {

        // ThreadContext.set(aggrSend, Constants.Symbol.EMPTY); // TODO will be remove in future
        return aggrResolveAddressSend(aggrService, aggrMethod, aggrPath, originReqIdOrBizId, method, uriOrSvc, headers, body, null);
    }

    public Mono<ClientResponse> send(String reqId, HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body) {
        return send(reqId, method, uriOrSvc, headers, body, null);
    }

    public Mono<ClientResponse> send(String reqId, HttpMethod method, String uriOrSvc, HttpHeaders headers, Object body, CallBackendConfig cbc) {
        String s = extractServiceOrAddress(uriOrSvc);
        if (isService(s)) {
            String path = uriOrSvc.substring(uriOrSvc.indexOf(Constants.Symbol.FORWARD_SLASH, 10));
            return send2service(reqId, method, s, path, headers, body, cbc);
        } else {
            return send2uri(reqId, method, uriOrSvc, headers, body, cbc);
        }
    }

    private Mono<ClientResponse> aggrResolveAddressSend(String aggrService, HttpMethod aggrMethod, String aggrPath, @Nullable String originReqIdOrBizId,
                                                        HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body, @Nullable CallBackendConfig cbc) {

        return send(originReqIdOrBizId, method, uriOrSvc, headers, body, cbc);
    }

    public Mono<ClientResponse> proxySend2service(@Nullable String originReqIdOrBizId, HttpMethod method, String service, String relativeUri,
                                                  @Nullable HttpHeaders headers, @Nullable Object body) {

        return send2service(originReqIdOrBizId, method, service, relativeUri, headers, body, null);
    }

    public Mono<ClientResponse> send2service(@Nullable String originReqIdOrBizId, HttpMethod method, String service, String relativeUri,
                                             @Nullable HttpHeaders headers, @Nullable Object body, @Nullable CallBackendConfig cbc) {

        // TODO this the future
        // if (cbc == null) {
        //     InstanceInfo inst = roundRobinChoose1instFrom(service);
        //     String uri = buildUri(inst, relativeUri);
        //     return send2uri(originReqIdOrBizId, method, uri, headers, body, null);
        // } else {
        //     List<InstanceInfo> insts = eurekaClient.getInstancesByVipAddress(service, false);
        //     // TODO 据callBackendConfig, 结合insts的实际metric, 从insts中选择合适的一个，转发请求过去
        // }
        // what about multiple nginx instance

        // current
        String uri = discoveryClientUriSelector.getNextUri(service, relativeUri);
        return send2uri(originReqIdOrBizId, method, uri, headers, body, cbc);
    }

    public Mono<ClientResponse> send2uri(@Nullable String originReqIdOrBizId, HttpMethod method, String uri,
                                         @Nullable HttpHeaders headers, @Nullable Object body, @Nullable Long timeout) {

        CallBackendConfig cbc = null;
        // if (timeout != null) {
        //     cbc = new CallBackendConfig(timeout);
        // }
        return send2uri(originReqIdOrBizId, method, uri, headers, body, cbc);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Mono<ClientResponse> send2uri(@Nullable String originReqIdOrBizId, HttpMethod method, String uri,
                                          @Nullable HttpHeaders headers, @Nullable Object body, @Nullable CallBackendConfig cbc) {

        if (log.isDebugEnabled()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            WebUtils.request2stringBuilder(originReqIdOrBizId, method, uri, headers, null, b);
            log.debug(b.toString(), LogService.BIZ_ID, originReqIdOrBizId);
        }

        // if (cbc == null) {
        //     cbc = CallBackendConfig.DEFAULT;
        // }

        // TODO remove this, and all event loop share one web client or one event loop one web client in future
        // WebClient.RequestBodySpec req = (ThreadContext.remove(aggrSend) == null ? proxyWebClient : aggrWebClient).method(method).uri(uri).headers(
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

        if (body != null) {
			if (body instanceof BodyInserter) {
				req.body((BodyInserter) body);
			} else if (body instanceof Flux) {
				Flux<DataBuffer> db = (Flux<DataBuffer>) body;
				req.body(BodyInserters.fromDataBuffers(db));
			} else if (body instanceof String) {
				String s = (String) body;
				req.body(Mono.just(s), String.class);
			} else {
				req.bodyValue(body);
			}
        }

        return req.exchange()
                /*
                .name(reqId)
                .doOnRequest(i -> {})
                .doOnSuccess(r -> {})
                .doOnError(
                        t -> {
                            Schedulers.parallel().schedule(() -> {
                                log.error("", LogService.BIZ_ID, reqId, t);
                            });
                        }
                )
                .timeout(Duration.ofMillis(cbc.timeout))
                */
                ;

        // if (log.isDebugEnabled()) {
        //     rm = rm.log();
        // }

        // TODO 请求完成后，做metric, 以反哺后续的请求转发
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
