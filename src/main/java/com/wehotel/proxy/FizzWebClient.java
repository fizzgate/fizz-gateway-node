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
package com.wehotel.proxy;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Applications;
import com.wehotel.flume.clients.log4j2appender.LogService;
import com.wehotel.config.AggrWebClientConfig;
import com.wehotel.config.ProxyWebClientConfig;
import com.wehotel.util.Constants;
import com.wehotel.util.ThreadContext;
import com.wehotel.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lancer
 */

@Service
public class FizzWebClient {

    private static final Logger log = LoggerFactory.getLogger(FizzWebClient.class);

    private static final String aggrSend     = "$aggrSend";

    private static final String localhost    = "localhost";

    @Resource
    private EurekaClient eurekaClient;

    @Resource(name = ProxyWebClientConfig.proxyWebClient)
    private WebClient proxyWebClient;

    @Resource(name = AggrWebClientConfig.aggrWebClient)
    private WebClient aggrWebClient;

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

        ThreadContext.set(aggrSend, Constants.Symbol.EMPTY); // TODO will be remove in future
        CallBackendConfig cbc = null;
        if (timeout != null) {
            cbc = new CallBackendConfig(timeout);
        }
        return aggrResolveAddressSend(aggrService, aggrMethod, aggrPath, originReqIdOrBizId, method, uriOrSvc, headers, body, cbc);
    }

    public Mono<ClientResponse> aggrSend(String aggrService, HttpMethod aggrMethod, String aggrPath, @Nullable String originReqIdOrBizId,
                                         HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body) {

        ThreadContext.set(aggrSend, Constants.Symbol.EMPTY); // TODO will be remove in future
        return aggrResolveAddressSend(aggrService, aggrMethod, aggrPath, originReqIdOrBizId, method, uriOrSvc, headers, body, null);
    }

    private Mono<ClientResponse> aggrResolveAddressSend(String aggrService, HttpMethod aggrMethod, String aggrPath, @Nullable String originReqIdOrBizId,
                                                        HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body, @Nullable CallBackendConfig cbc) {

        String s = extractServiceOrAddress(uriOrSvc);
        if (isService(s)) {
            String path = uriOrSvc.substring(uriOrSvc.indexOf(Constants.Symbol.FORWARD_SLASH, 10));
            return send2service(originReqIdOrBizId, method, s, path, headers, body, cbc);
        } else {
            return send2uri(originReqIdOrBizId, method, uriOrSvc, headers, body, cbc);
        }
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
        InstanceInfo inst = roundRobinChoose1instFrom(service);
        String uri = buildUri(inst, relativeUri);
        return send2uri(originReqIdOrBizId, method, uri, headers, body, cbc);
    }

    public Mono<ClientResponse> send2uri(@Nullable String originReqIdOrBizId, HttpMethod method, String uri,
                                         @Nullable HttpHeaders headers, @Nullable Object body, @Nullable Long timeout) {

        CallBackendConfig cbc = null;
        if (timeout != null) {
            cbc = new CallBackendConfig(timeout);
        }
        return send2uri(originReqIdOrBizId, method, uri, headers, body, cbc);
    }

    private static final String r = "R";

    private Mono<ClientResponse> send2uri(@Nullable String originReqIdOrBizId, HttpMethod method, String uri,
                                          @Nullable HttpHeaders headers, @Nullable Object body, @Nullable CallBackendConfig cbc) {

        if (originReqIdOrBizId == null) { // should not execute this
            if (headers == null) {
                originReqIdOrBizId = r + ThreadLocalRandom.current().nextInt(1_000, 10_000);
            } else {
                originReqIdOrBizId = r + headers.hashCode();
            }
        }
        final String reqId = originReqIdOrBizId;

        if (log.isDebugEnabled()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            WebUtils.request2stringBuilder(reqId, method, uri, headers, null, b);
            log.debug(b.toString(), LogService.BIZ_ID, reqId);
        }

        if (cbc == null) {
            cbc = CallBackendConfig.DEFAULT;
        }

        // TODO remove this, and all event loop share one web client or one event loop one web client in future
        WebClient.RequestBodySpec req = (ThreadContext.remove(aggrSend) == null ? proxyWebClient : aggrWebClient).method(method).uri(uri).headers(
                hdrs -> {
                    if (headers != null) {
                        headers.forEach(
                                (h, vs) -> {
                                    hdrs.addAll(h, vs);
                                }
                        );
                    }
                }
        );

        if (body != null) {
            if (body instanceof Flux) {
                Flux<DataBuffer> db = (Flux<DataBuffer>) body;
                req.body(BodyInserters.fromDataBuffers(db));
            } else if (body instanceof String) {
                String s = (String) body;
                req.body(Mono.just(s), String.class);
            } else {
                req.bodyValue(body);
            }
        }

        Mono<ClientResponse> rm = req.exchange().name(reqId)
                .doOnRequest(i -> {})
                .doOnSuccess(r -> {})
                /*.doOnError(
                        t -> {
                            Schedulers.parallel().schedule(() -> {
                                log.error("", LogService.BIZ_ID, reqId, t);
                            });
                        }
                )
                .timeout(Duration.ofMillis(cbc.timeout))*/
                ;

        if (log.isDebugEnabled()) {
            rm = rm.log();
        }
        return rm;

        // TODO 请求完成后，做metric, 以反哺后续的请求转发
    }

    private String buildUri(InstanceInfo inst, String path) {
        StringBuilder b = ThreadContext.getStringBuilder();
        return b.append(Constants.Symbol.HTTP_PROTOCOL_PREFIX).append(inst.getIPAddr()).append(Constants.Symbol.COLON).append(inst.getPort()).append(path).toString();
    }

    private InstanceInfo roundRobinChoose1instFrom(String service) {
        List<InstanceInfo> insts = eurekaClient.getInstancesByVipAddress(service, false);
        if (insts == null || insts.isEmpty()) {
            throw new RuntimeException("eureka no " + service, null, false, false) {};
        }
        Applications apps = eurekaClient.getApplications();
        int index = (int) (apps.getNextIndex(service.toUpperCase(), false).incrementAndGet() % insts.size());
        return insts.get(index);
    }

    private String extractServiceOrAddress(String uriOrSvc) {
        return uriOrSvc.substring(7, uriOrSvc.indexOf(Constants.Symbol.FORWARD_SLASH, 10));
    }

    private boolean isService(String s) {
        if (s.indexOf(Constants.Symbol.DOT) > 0 || s.equals(localhost)) {
            return false;
        } else {
            return true;
        }
    }
}
