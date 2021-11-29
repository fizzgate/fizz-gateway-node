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

package we.dedicatedline;

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.proxy.FizzWebClient;
import we.util.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author hongqiaowei
 */

class DedicatedLineHttpHandler implements HttpHandler {

    private static final String      disconnected_client_log_category = "DisconnectedClient";

    private static final Logger      log                              = LoggerFactory.getLogger(DedicatedLineHttpHandler.class);

    private static final Logger      lostClientLog                    = LoggerFactory.getLogger(disconnected_client_log_category);

    private static final Set<String> disconnected_client_exceptions   = new HashSet<>(Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));

    private static final String      symmetricEncryptor               = "symmEncpT";

    private static final String      symmetricDecryptor               = "symmDecpT";

    private WebSessionManager           sessionManager;
    private ServerCodecConfigurer       serverCodecConfigurer;
    private LocaleContextResolver       localeContextResolver;
    private ForwardedHeaderTransformer  forwardedHeaderTransformer;
    private boolean                     enableLoggingRequestDetails = false;

    private SystemConfig                systemConfig;
    private FizzWebClient               fizzWebClient;
    private DedicatedLineInfoService    dedicatedLineInfoService;

    public DedicatedLineHttpHandler(ReactiveWebServerApplicationContext applicationContext, WebSessionManager sessionManager, ServerCodecConfigurer codecConfigurer,
                                    LocaleContextResolver localeContextResolver, ForwardedHeaderTransformer forwardedHeaderTransformer) {

        this.sessionManager             = sessionManager;
        this.serverCodecConfigurer      = codecConfigurer;
        this.localeContextResolver      = localeContextResolver;
        this.forwardedHeaderTransformer = forwardedHeaderTransformer;

        systemConfig             = applicationContext.getBean(SystemConfig.class);
        fizzWebClient            = applicationContext.getBean(FizzWebClient.class);
        dedicatedLineInfoService = applicationContext.getBean(DedicatedLineInfoService.class);
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
        if (forwardedHeaderTransformer != null) {
            try {
                request = forwardedHeaderTransformer.apply(request);
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to apply forwarded headers to {}", formatRequest(request), t);
                }
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return response.setComplete();
            }
        }

        DefaultServerWebExchange exchange = new DefaultServerWebExchange(request, response, sessionManager, serverCodecConfigurer, localeContextResolver);
        String logPrefix = exchange.getLogPrefix();

        URI    requestURI = request.getURI();
        String path       = requestURI.getPath();
        int    secFS      = path.indexOf(Consts.S.FORWARD_SLASH, 1);
        String service    = path.substring(1, secFS);
        DedicatedLineInfo dedicatedLineInfo = dedicatedLineInfoService.get(service);
        if (dedicatedLineInfo == null) {
            log.warn("{}{} service no dedicated line info", logPrefix, service);
            return WebUtils.response(response, HttpStatus.FORBIDDEN, null, logPrefix + ' ' + service + " service no dedicated line info");
        }

        String targetUrl = constructTargetUrl(requestURI, path, dedicatedLineInfo.url);
        HttpHeaders writableHttpHeaders = signAndSetHeaders(request.getHeaders(), dedicatedLineInfo.pairCodeId, dedicatedLineInfo.secretKey);

        int requestTimeout = systemConfig.fizzDedicatedLineClientRequestTimeout();
        int retryCount     = systemConfig.fizzDedicatedLineClientRequestRetryCount();
        int retryInterval  = systemConfig.fizzDedicatedLineClientRequestRetryInterval();

        try {
            Flux<DataBuffer> dataBufferFlux = request.getBody();
            Flux<DataBuffer> bodyFlux = dataBufferFlux;
            if (systemConfig.fizzDedicatedLineClientRequestCrypto() && request.getMethod() != HttpMethod.GET) {
                bodyFlux = encrypt(dataBufferFlux, dedicatedLineInfo.requestCryptoKey);
                writableHttpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
            }

            Mono<ClientResponse> remoteResponseMono = fizzWebClient.send( request.getId(), request.getMethod(), targetUrl, writableHttpHeaders, bodyFlux,
                                                                          requestTimeout, retryCount, retryInterval );

            Mono<Void> respMono = remoteResponseMono.flatMap(
                                                            remoteResp -> {
                                                                response.setStatusCode(remoteResp.statusCode());
                                                                HttpHeaders respHeaders = response.getHeaders();
                                                                HttpHeaders remoteRespHeaders = remoteResp.headers().asHttpHeaders();
                                                                respHeaders.putAll(remoteRespHeaders);
                                                                if (log.isDebugEnabled()) {
                                                                    StringBuilder sb = ThreadContext.getStringBuilder();
                                                                    WebUtils.response2stringBuilder(logPrefix, remoteResp, sb);
                                                                    log.debug(sb.toString());
                                                                }
                                                                Flux<DataBuffer> remoteRespBody = remoteResp.body(BodyExtractors.toDataBuffers());

                                                                if (systemConfig.fizzDedicatedLineClientRequestCrypto()) {
                                                                    if (response.getStatusCode() == HttpStatus.OK) {
                                                                        String v = respHeaders.getFirst(WebUtils.BODY_ENCRYPT);
                                                                        if (org.apache.commons.lang3.StringUtils.isBlank(v) || v.equals(Consts.S.TRUE1)) {
                                                                            respHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                                                                            return response.writeWith (decrypt(remoteRespBody, dedicatedLineInfo.requestCryptoKey));
                                                                        }
                                                                    }
                                                                }
                                                                return response.writeWith (remoteRespBody)
                                                                               .doOnError (   throwable -> cleanup(remoteResp)               )
                                                                               .doOnCancel(          () -> cleanup(remoteResp)               );
                                                            }
                                                    );

            return respMono.doOnSuccess  ( v -> logResponse(exchange)              )
                           .onErrorResume( t -> handleUnresolvedError(exchange, t) );
                         //.then         ( Mono.defer(response::setComplete)       );

        } catch (Throwable t) {
            log.error(logPrefix + "500 Server Error for " + formatRequest(request), t);
            return WebUtils.response(response, HttpStatus.INTERNAL_SERVER_ERROR, null, logPrefix + ' ' + Utils.getMessage(t));
        }
    }

    private Flux<DataBuffer> encrypt(Flux<DataBuffer> bodyFlux, String cryptoKey) {
        return NettyDataBufferUtils.join(bodyFlux).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                                   .flatMap(
                                           body -> {
                                               if (body == NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                                   return Mono.empty();
                                               } else {
                                                   byte[] bytes = null;
                                                   if (body instanceof PooledDataBuffer) {
                                                       try {
                                                           bytes = NettyDataBufferUtils.copyBytes(body);
                                                       } finally {
                                                           NettyDataBufferUtils.release(body);
                                                       }
                                                   } else {
                                                       bytes = body.asByteBuffer().array();
                                                   }
                                                   SymmetricEncryptor encryptor = (SymmetricEncryptor) ThreadContext.get(symmetricEncryptor);
                                                   if (encryptor == null) {
                                                       encryptor = new SymmetricEncryptor(SymmetricAlgorithm.AES, cryptoKey);
                                                       ThreadContext.set(symmetricEncryptor, encryptor);
                                                   } else {
                                                       if (!encryptor.secretKey.equals(cryptoKey)) {
                                                           encryptor = new SymmetricEncryptor(SymmetricAlgorithm.AES, cryptoKey);
                                                           ThreadContext.set(symmetricEncryptor, encryptor);
                                                       }
                                                   }
                                                   DataBuffer from = NettyDataBufferUtils.from(encryptor.encrypt(bytes));
                                                   return Mono.just(from);
                                               }
                                           }
                                   )
                                   .flux();
    }

    private Flux<DataBuffer> decrypt(Flux<DataBuffer> bodyFlux, String cryptoKey) {
        return NettyDataBufferUtils.join(bodyFlux).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                                   .flatMap(
                                           body -> {
                                               if (body == NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                                   return Mono.empty();
                                               } else {
                                                   byte[] bytes = null;
                                                   if (body instanceof PooledDataBuffer) {
                                                       try {
                                                           bytes = NettyDataBufferUtils.copyBytes(body);
                                                       } finally {
                                                           NettyDataBufferUtils.release(body);
                                                       }
                                                   } else {
                                                       bytes = body.asByteBuffer().array();
                                                   }
                                                   SymmetricDecryptor decryptor = (SymmetricDecryptor) ThreadContext.get(symmetricDecryptor);
                                                   if (decryptor == null) {
                                                       decryptor = new SymmetricDecryptor(SymmetricAlgorithm.AES, cryptoKey);
                                                       ThreadContext.set(symmetricDecryptor, decryptor);
                                                   } else {
                                                       if (!decryptor.secretKey.equals(cryptoKey)) {
                                                           decryptor = new SymmetricDecryptor(SymmetricAlgorithm.AES, cryptoKey);
                                                           ThreadContext.set(symmetricDecryptor, decryptor);
                                                       }
                                                   }
                                                   DataBuffer from = NettyDataBufferUtils.from(decryptor.decrypt(bytes));
                                                   return Mono.just(from);
                                               }
                                           }
                                   )
                                   .flux();
    }

    private String constructTargetUrl(URI requestURI, String path, String serverAddress) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(serverAddress).append(path);
        String qry = requestURI.getQuery();
        if (StringUtils.hasText(qry)) {
            if (org.apache.commons.lang3.StringUtils.indexOfAny(qry, Consts.S.LEFT_BRACE, Consts.S.FORWARD_SLASH, Consts.S.HASH) > 0) {
                qry = requestURI.getRawQuery();
            }
            b.append(Consts.S.QUESTION).append(qry);
        }
        return b.toString();
    }

    private HttpHeaders signAndSetHeaders(HttpHeaders headers, String pairCodeId, String secretKey) {
        String timestamp  = String.valueOf(System.currentTimeMillis());
        String sign       = DedicatedLineUtils.sign(pairCodeId, timestamp, secretKey);

        HttpHeaders writableHttpHeaders = HttpHeaders.writableHttpHeaders(headers);
        writableHttpHeaders.set(SystemConfig.FIZZ_DL_ID,     pairCodeId);
        writableHttpHeaders.set(SystemConfig.FIZZ_DL_TS,     timestamp);
        writableHttpHeaders.set(SystemConfig.FIZZ_DL_SIGN,   sign);
        writableHttpHeaders.set(SystemConfig.FIZZ_DL_CLIENT, systemConfig.fizzDedicatedLineClientId());
        return writableHttpHeaders;
    }

    private void cleanup(ClientResponse clientResponse) {
        if (clientResponse != null) {
            clientResponse.bodyToMono(Void.class).subscribe();
        }
    }

    private void logResponse(ServerWebExchange exchange) {
        WebUtils.traceDebug(log, traceOn -> {
            HttpStatus status = exchange.getResponse().getStatusCode();
            return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK") + (traceOn ? ", headers=" + formatHeaders(exchange.getResponse().getHeaders()) : "");
        });
    }

    private String formatHeaders(HttpHeaders responseHeaders) {
        return this.enableLoggingRequestDetails ?
                responseHeaders.toString() : responseHeaders.isEmpty() ? "{}" : "{masked}";
    }

    private String formatRequest(ServerHttpRequest request) {
        String rawQuery = request.getURI().getRawQuery();
        String query    = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
        return "HTTP " + request.getMethod() + " \"" + request.getPath() + query + "\"";
    }

    private Mono<Void> handleUnresolvedError(ServerWebExchange exchange, Throwable t) {
        ServerHttpRequest   request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String logPrefix = exchange.getLogPrefix();

        if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
            log.error(logPrefix + "500 Server Error for " + formatRequest(request), t);
            return WebUtils.response(response, null, null, logPrefix + ' ' + Utils.getMessage(t));

        } else if (isDisconnectedClientError(t)) {
            if (lostClientLog.isTraceEnabled()) {
                lostClientLog.trace(logPrefix + "Client went away", t);
            } else if (lostClientLog.isDebugEnabled()) {
                lostClientLog.debug(logPrefix + "Client went away: " + t + " (stacktrace at TRACE level for '" + disconnected_client_log_category + "')");
            }
            return WebUtils.response(response, null, null, logPrefix + ' ' + Utils.getMessage(t));

        } else {
            // After the response is committed, propagate errors to the server...
            log.error(logPrefix + "Error [" + t + "] for " + formatRequest(request) + ", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
            return Mono.error(t);
        }
    }

    private boolean isDisconnectedClientError(Throwable t) {
        String message = NestedExceptionUtils.getMostSpecificCause(t).getMessage();
        if (message != null) {
            String text = message.toLowerCase();
            if (text.contains("broken pipe") || text.contains("connection reset by peer")) {
                return true;
            }
        }
        return disconnected_client_exceptions.contains(t.getClass().getSimpleName());
    }
}
