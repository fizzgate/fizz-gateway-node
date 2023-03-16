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

package com.fizzgate.filter;

import com.fizzgate.config.SystemConfig;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.proxy.FizzWebClient;
import com.fizzgate.proxy.Route;
import com.fizzgate.proxy.dubbo.ApacheDubboGenericService;
import com.fizzgate.proxy.dubbo.DubboInterfaceDeclaration;
import com.fizzgate.service_registry.RegistryCenterService;
import com.fizzgate.stats.FlowStat;
import com.fizzgate.stats.ResourceConfig;
import com.fizzgate.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
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

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author hongqiaowei
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RouteFilter extends FizzWebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteFilter.class);

    @Resource
    private FizzWebClient             fizzWebClient;

    @Resource
    private ApacheDubboGenericService dubboGenericService;

    @Resource
    private SystemConfig              systemConfig;

    @Resource
    private FlowControlFilter         flowControlFilter;

    @Resource(name = "stringRedisTemplate2")
    private StringRedisTemplate       stringRedisTemplate;

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
                org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
                String msg = traceId + ' ' + pfr.id + " fail";
                if (pfr.cause == null) {
                    LOGGER.error(msg);
                } else {
                    LOGGER.error(msg, pfr.cause);
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

        String traceId = WebUtils.getTraceId(exchange);
        if (LOGGER.isDebugEnabled()) {
            org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
            LOGGER.debug("route filter start");
        }

        ServerHttpRequest req = exchange.getRequest();
        Route route = exchange.getAttribute(WebUtils.ROUTE);
        HttpHeaders hdrs = null;

        if (route != null && route.type != ApiConfig.Type.DUBBO) {
            hdrs = WebUtils.mergeAppendHeaders(exchange);
            WebUtils.setXForwardedFor(exchange, hdrs);
        }

        if (route == null) {
            Map.Entry<String, List<String>> pathQueryTemplate = WebUtils.getClientReqPathQueryTemplate(exchange).entrySet().iterator().next();
            return fizzWebClient.send2service(traceId, req.getMethod(), WebUtils.getClientService(exchange), pathQueryTemplate.getKey(), hdrs, req.getBody(), 0, 0, 0, pathQueryTemplate.getValue().toArray(new String[0]))
                                .flatMap(genServerResponse(exchange));

        } else if (route.type == ApiConfig.Type.SERVICE_DISCOVERY) {
            Map.Entry<String, List<String>> pathQueryTemplate = getBackendPathQueryTemplate(req, route).entrySet().iterator().next();
            String svc = RegistryCenterService.getServiceNameSpace(route.registryCenter, route.backendService);
            return fizzWebClient.send2service(traceId, route.method, svc, pathQueryTemplate.getKey(), hdrs, req.getBody(), route.timeout, route.retryCount, route.retryInterval, pathQueryTemplate.getValue().toArray(new String[0]))
                                .flatMap(genServerResponse(exchange));

        } else if (route.type == ApiConfig.Type.REVERSE_PROXY) {
            Map.Entry<String, List<String>> pathQueryTemplate = getBackendPathQueryTemplate(req, route).entrySet().iterator().next();
            String uri = ThreadContext.getStringBuilder().append(route.nextHttpHostPort)
                                                         .append(pathQueryTemplate.getKey())
                                                         .toString();
            return fizzWebClient.send(traceId, route.method, uri, hdrs, req.getBody(), route.timeout, route.retryCount, route.retryInterval, pathQueryTemplate.getValue().toArray(new String[0]))
                                .flatMap(genServerResponse(exchange));

        } else {
            return dubboRpc(exchange, route);
        }
    }

    private Map<String, List<String>> getBackendPathQueryTemplate(ServerHttpRequest request, Route route) {
        String qry = route.query;
        if (qry == null) {
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            if (queryParams.isEmpty()) {
                return Collections.singletonMap(route.backendPath, Collections.emptyList());
            } else {
                Map<String, List<String>> queryStringTemplate = WebUtils.toQueryStringTemplate(queryParams);
                Map.Entry<String, List<String>> entry = queryStringTemplate.entrySet().iterator().next();
                qry = route.backendPath + Consts.S.QUESTION + entry.getKey();
                return Collections.singletonMap(qry, entry.getValue());
            }
        } else {
            return Collections.singletonMap(route.backendPath + Consts.S.QUESTION + qry, Collections.emptyList());
        }
    }

    private Function<ClientResponse, Mono<? extends Void>> genServerResponse(ServerWebExchange exchange) {
        return remoteResp -> {
            String traceId = WebUtils.getTraceId(exchange);
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
            if (LOGGER.isDebugEnabled()) {
                StringBuilder b = ThreadContext.getStringBuilder();
                WebUtils.response2stringBuilder(traceId, remoteResp, b);
                org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
                LOGGER.debug(b.toString());
            }
            return clientResp.writeWith(remoteResp.body(BodyExtractors.toDataBuffers()))
                             .doOnError(
                                     t -> {
                                         org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
                                         flowStatHandle(exchange);
                                         LOGGER.error("fsde", t);
                                         cleanup(remoteResp);
                                     }
                             )
                             .doOnCancel(
                                     () -> {
                                         flowStatHandle(exchange);
                                         cleanup(remoteResp);
                                     }
                             );
        };
    }

    private void flowStatHandle(ServerWebExchange exchange) {
        List<ResourceConfig> resourceConfigs = exchange.getAttribute("flowStatResources");
        if (resourceConfigs != null && !resourceConfigs.isEmpty()) {
            FlowStat flowStat = exchange.getAttribute("flowStat");
            Long currentTimeSlot = exchange.getAttribute("currentTimeSlot");
            Long start = exchange.getAttribute("start");
            long rt = System.currentTimeMillis() - start;
            flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, false, HttpStatus.INTERNAL_SERVER_ERROR);

            if ((flowControlFilter.isFlowControlDebug() || flowControlFilter.isCloseDebugNotPassing30s()) && flowControlFilter.isIncludeCurrentNode()) {
                String traceId = WebUtils.getTraceId(exchange);
                for (ResourceConfig resourceConfig : resourceConfigs) {
                    String key = "flowstatdebug";
                    String field = traceId + ":" + resourceConfig.getResourceId();
                    stringRedisTemplate.opsForHash().delete(key, field);
                    LOGGER.debug("flowstat delete2 field: " + field);
                }
            }

            exchange.getAttributes().put("routeFilterHandle", Consts.S.EMPTY);

            /* if (LOGGER.isDebugEnabled()) {
                List<String> rids = resourceConfigs.stream().map(ResourceConfig::getResourceId).collect(Collectors.toList());
                LOGGER.debug("flow stat handle {}", rids);
            } */
        }
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
                            org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, WebUtils.getTraceId(exchange));
                            LOGGER.error(b.toString(), t);
                        }
                )
                ;
    }
}
