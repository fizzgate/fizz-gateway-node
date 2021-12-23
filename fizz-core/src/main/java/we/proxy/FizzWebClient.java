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
import reactor.util.retry.Retry;
import we.config.ProxyWebClientConfig;
import we.config.SystemConfig;
import we.exception.ExternalService4xxException;
import we.fizz.exception.FizzRuntimeException;
import we.flume.clients.log4j2appender.LogService;
import we.service_registry.RegistryCenterService;
import we.util.Consts;
import we.util.ThreadContext;
import we.util.WebUtils;

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

    @Resource
    private RegistryCenterService registryCenterService;

    @Resource(name = ProxyWebClientConfig.proxyWebClient)
    private WebClient webClient;

    public Mono<ClientResponse> send(String traceId,
                                  HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body) {

        return send(traceId, method, uriOrSvc, headers, body, 0, 0, 0);
    }

    public Mono<ClientResponse> send(String traceId,
                                  HttpMethod method, String uriOrSvc, @Nullable HttpHeaders headers, @Nullable Object body,
    		                           long timeout, long numRetries, long retryInterval) {

        String s = extractServiceOrAddress(uriOrSvc);
       
        Mono<ClientResponse> cr = Mono.just(Consts.S.EMPTY).flatMap(dummy -> {
            if (isService(s)) {
                String path = uriOrSvc.substring(uriOrSvc.indexOf(Consts.S.FORWARD_SLASH, 10));
                String uri = null;
                int commaPos = s.indexOf(Consts.S.COMMA);
                if (commaPos > -1) {
                    String rc  = s.substring(0, commaPos);
                    String svc = s.substring(commaPos + 1);
                    String instance = registryCenterService.getInstance(rc, svc);
                    uri = ThreadContext.getStringBuilder().append(Consts.S.HTTP_PROTOCOL_PREFIX).append(instance).append(path).toString();
                } else {
                    uri = discoveryClientUriSelector.getNextUri(s, path);
                }
                return send2uri(traceId, method, uri, headers, body, timeout);
            } else {
                return send2uri(traceId, method, uriOrSvc, headers, body, timeout);
            }
        });
       
        if (numRetries > 0) {
			cr = cr.flatMap(resp->{
				// Do not retry on 4xx client error
				if (resp.statusCode().is4xxClientError()) {
					return Mono.error(new ExternalService4xxException());
				}
				return Mono.just(resp);
			}).retryWhen(
                Retry.fixedDelay(numRetries, Duration.ofMillis(retryInterval > 0 ? retryInterval : 0))
                     .filter(throwable -> !(throwable instanceof ExternalService4xxException))
                     .onRetryExhaustedThrow(
                         (retryBackoffSpec, retrySignal) -> {
                             throw new FizzRuntimeException("External service failed to process after max retries");
                         }
                     )
            );
        }
        return cr;
    }

    public Mono<ClientResponse> send2service(@Nullable String traceId,
                                                    HttpMethod method, String service, String relativeUri, @Nullable HttpHeaders headers, @Nullable Object body) {

        return send2service(traceId, method, service, relativeUri, headers, body, 0, 0, 0);
    }

    public Mono<ClientResponse> send2service(@Nullable String traceId,
                                                   HttpMethod method,  String service,  String relativeUri,  @Nullable HttpHeaders headers,  @Nullable Object body,
                                                         long timeout, long numRetries, long retryInterval) {

    	Mono<ClientResponse> cr = Mono.just(Consts.S.EMPTY).flatMap(dummy -> {
            String uri = null;
            int commaPos = service.indexOf(Consts.S.COMMA);
            if (commaPos > -1) {
                String rc = service.substring(0, commaPos);
                String s  = service.substring(commaPos + 1);
                String instance = registryCenterService.getInstance(rc, s);
                uri = ThreadContext.getStringBuilder().append(Consts.S.HTTP_PROTOCOL_PREFIX).append(instance).append(relativeUri).toString();
            } else {
                uri = discoveryClientUriSelector.getNextUri(service, relativeUri);
            }
            return send2uri(traceId, method, uri, headers, body, timeout);
    	});
        if (numRetries > 0) {
            cr = cr.flatMap(resp -> {
                // Do not retry on 4xx client error
                if (resp.statusCode().is4xxClientError()) {
                    return Mono.error(new ExternalService4xxException());
                }
                return Mono.just(resp);
            }).retryWhen(
                Retry.fixedDelay(numRetries, Duration.ofMillis(retryInterval > 0 ? retryInterval : 0))
                     .filter(throwable -> !(throwable instanceof ExternalService4xxException))
                     .onRetryExhaustedThrow(
                         (retryBackoffSpec, retrySignal) -> {
                             throw new FizzRuntimeException("External service failed to process after max retries");
                         }
                     )
            );
        }
        return cr;
    }

    public Mono<ClientResponse> send2uri(@Nullable String traceId, HttpMethod method, String uri, @Nullable HttpHeaders headers, @Nullable Object body) {
        return send2uri(traceId, method, uri, headers, body, 0);
    }

    public Mono<ClientResponse> send2uri(@Nullable String traceId,
                                                HttpMethod method, String uri, @Nullable HttpHeaders headers, @Nullable Object body,
                                                     long timeout) {

        if (log.isDebugEnabled()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            WebUtils.request2stringBuilder(traceId, method, uri, headers, null, b);
            log.debug(b.toString(), LogService.BIZ_ID, traceId);
        }

        WebClient.RequestBodySpec req = webClient.method(method).uri(uri).headers(
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
			} else {
				req.bodyValue(body);
			}
        }

        Mono<ClientResponse> cr = req.exchange();
        if (timeout == 0) {
            if (systemConfig.getRouteTimeout() != 0) {
                timeout = systemConfig.getRouteTimeout();
            }
        }
        if (timeout > 0) {
            cr = cr.timeout(Duration.ofMillis(timeout));
        }

        return cr;
    }

    private void setHostHeader(String uri, HttpHeaders headers) {
        boolean domain = false;
        int begin = uri.indexOf(Consts.S.FORWARD_SLASH) + 2;
        int end = uri.indexOf(Consts.S.FORWARD_SLASH, begin);
        for (int i = begin; i < end; i++) {
            char c = uri.charAt(i);
            if (  (47 < c && c < 58) || c == Consts.S.DOT || c == Consts.S.COLON  ) {
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
        int i = uriOrSvc.indexOf(Consts.S.FORWARD_SLASH, 9);
        if (i > 0) {
            end = i;
        }
        return uriOrSvc.substring(start, end);
    }

    private boolean isService(String s) {
        if (  StringUtils.indexOfAny(s, Consts.S.DOT, Consts.S.COLON) > 0  ||  StringUtils.startsWith(s, localhost)  ) {
            return false;
        } else {
            return true;
        }
    }

}
