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

package we.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.auth.ApiConfig;
import we.proxy.FizzWebClient;
import we.proxy.Route;
import we.proxy.dubbo.ApacheDubboGenericService;
import we.proxy.dubbo.DubboInterfaceDeclaration;
import we.util.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author hongqiaowei
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RouteFilter extends FizzWebFilter {

    private static final Logger log = LoggerFactory.getLogger(RouteFilter.class);

    @Resource
    private FizzWebClient             fizzWebClient;

    @Resource
    private ApacheDubboGenericService dubboGenericService;

    @Resource
    private SystemConfig              systemConfig;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        FilterResult pfr = WebUtils.getPrevFilterResult(exchange);
        if (pfr.success) {
            return doFilter0(exchange, chain);
        } else {
            Mono<Void> resp = WebUtils.getDirectResponse(exchange);
            if (resp == null) { // should not reach here
                ServerHttpRequest clientReq = exchange.getRequest();
                String traceId = WebUtils.getTraceId(exchange);
                String msg = pfr.id + " fail";
                if (pfr.cause == null) {
                    log.error(msg, LogService.BIZ_ID, traceId);
                } else {
                    log.error(msg, LogService.BIZ_ID, traceId, pfr.cause);
                }
                HttpStatus s = HttpStatus.INTERNAL_SERVER_ERROR;
                if (!SystemConfig.FIZZ_ERR_RESP_HTTP_STATUS_ENABLE) {
                    s = HttpStatus.OK;
                }
                return WebUtils.buildJsonDirectResponseAndBindContext(exchange, s, null, WebUtils.jsonRespBody(s.value(), msg, traceId));
            } else {
                return resp;
            }
        }
    }

    private Mono<Void> doFilter0(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest req = exchange.getRequest();
        String traceId = WebUtils.getTraceId(exchange);
        Route route = exchange.getAttribute(WebUtils.ROUTE);
        HttpHeaders hdrs = null;

        if (route != null && route.type != ApiConfig.Type.DUBBO) {
            hdrs = WebUtils.mergeAppendHeaders(exchange);
        }

        if (route == null) {
            String pathQuery = WebUtils.getClientReqPathQuery(exchange);
            return fizzWebClient.send2service(traceId, req.getMethod(), WebUtils.getClientService(exchange), pathQuery, hdrs, req.getBody(), 0, 0, 0)
                                .flatMap(genServerResponse(exchange));

        } else if (route.type == ApiConfig.Type.SERVICE_DISCOVERY) {
            String pathQuery = getBackendPathQuery(req, route);
            return fizzWebClient.send2service(traceId, route.method, route.backendService, pathQuery, hdrs, req.getBody(), route.timeout, route.retryCount, route.retryInterval)
                                .flatMap(genServerResponse(exchange));

        } else if (route.type == ApiConfig.Type.REVERSE_PROXY) {
            String uri = ThreadContext.getStringBuilder().append(route.nextHttpHostPort)
                                                         .append(getBackendPathQuery(req, route))
                                                         .toString();
            return fizzWebClient.send(traceId, route.method, uri, hdrs, req.getBody(), route.timeout, route.retryCount, route.retryInterval)
                                .flatMap(genServerResponse(exchange));

        } else {
            return dubboRpc(exchange, route);
        }
    }

    private String getBackendPathQuery(ServerHttpRequest request, Route route) {
        String qry = route.query;
        if (qry == null) {
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            if (queryParams.isEmpty()) {
                return route.backendPath;
            } else {
                return route.backendPath + Consts.S.QUESTION + WebUtils.toQueryString(queryParams);
            }
        } else {
            return route.backendPath + Consts.S.QUESTION + qry;
        }
    }

    private Function<ClientResponse, Mono<? extends Void>> genServerResponse(ServerWebExchange exchange) {
        return remoteResp -> {
            ServerHttpResponse clientResp = exchange.getResponse();
            clientResp.setStatusCode(remoteResp.statusCode());
            HttpHeaders clientRespHeaders = clientResp.getHeaders();
            HttpHeaders remoteRespHeaders = remoteResp.headers().asHttpHeaders();
            remoteRespHeaders.entrySet().forEach(
                    h -> {
                        String k = h.getKey();
                        if (clientRespHeaders.containsKey(k)) {
                            if (k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)
                                    || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                                    || k.equals(HttpHeaders.ACCESS_CONTROL_MAX_AGE)
                                    || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)) {
                            } else {
                                clientRespHeaders.put(k, h.getValue());
                            }
                        } else {
                            clientRespHeaders.put(k, h.getValue());
                        }
                    }
            );
            if (log.isDebugEnabled()) {
                StringBuilder b = ThreadContext.getStringBuilder();
                String traceId = WebUtils.getTraceId(exchange);
                WebUtils.response2stringBuilder(traceId, remoteResp, b);
                log.debug(b.toString(), LogService.BIZ_ID, traceId);
            }
            return clientResp.writeWith(remoteResp.body(BodyExtractors.toDataBuffers()))
                    .doOnError(throwable -> cleanup(remoteResp)).doOnCancel(() -> cleanup(remoteResp));
        };
    }

    private void cleanup(ClientResponse clientResponse) {
        if (clientResponse != null) {
            clientResponse.bodyToMono(Void.class).subscribe();
        }
    }

    private Mono<Void> dubboRpc(ServerWebExchange exchange, Route route) {
        final String[] ls = {null};
        return DataBufferUtils.join(exchange.getRequest().getBody()).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER)
                .flatMap(
                        b -> {
                            HashMap<String, Object> parameters = null;
                            if (b != NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
                                String json = b.toString(StandardCharsets.UTF_8).trim();
                                ls[0] = json;
                                NettyDataBufferUtils.release(b);
                                if (json.charAt(0) == '[') {
                                    ArrayList<Object> lst = JacksonUtils.readValue(json, ArrayList.class);
                                    parameters = new HashMap<>();
                                    for (int i = 0; i < lst.size(); i++) {
                                        parameters.put("p" + (i + 1), lst.get(i));
                                    }
                                } else {
                                    parameters = JacksonUtils.readValue(json, HashMap.class);
                                }
                            }

                            DubboInterfaceDeclaration declaration = new DubboInterfaceDeclaration();
                            declaration.setServiceName(route.backendService);
                            declaration.setVersion(route.rpcVersion);
                            declaration.setGroup(route.rpcGroup);
                            declaration.setMethod(route.rpcMethod);
                            declaration.setParameterTypes(route.rpcParamTypes);
                            int t = 20_000;
                            if (route.timeout != 0) {
                                t = (int) route.timeout;
                            }
                            declaration.setTimeout(t);

                            Map<String, String> attachments = Collections.singletonMap(systemConfig.fizzTraceIdHeader(), WebUtils.getTraceId(exchange));
                            return dubboGenericService.send(parameters, declaration, attachments);
                        }
                )
                .flatMap(
                        dubboRpcResponseBody -> {
                            Mono<Void> m = WebUtils.responseJson(exchange, HttpStatus.OK, null, JacksonUtils.writeValueAsString(dubboRpcResponseBody));
                            return m;
                        }
                )
                .doOnError(
                        t -> {
                            StringBuilder b = ThreadContext.getStringBuilder();
                            WebUtils.request2stringBuilder(exchange, b);
                            if (ls[0] != null) {
                                b.append('\n').append(ls[0]);
                            }
                            log.error(b.toString(), LogService.BIZ_ID, WebUtils.getTraceId(exchange), t);
                        }
                )
                ;
    }
}
