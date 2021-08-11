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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.constants.CommonConstants;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.plugin.auth.ApiConfig;
import we.proxy.FizzWebClient;
import we.proxy.dubbo.ApacheDubboGenericService;
import we.proxy.dubbo.DubboInterfaceDeclaration;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * @author hongqiaowei
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RouteFilter extends FizzWebFilter {

    private static final Logger log = LoggerFactory.getLogger(RouteFilter.class);

    @Resource
    private FizzWebClient fizzWebClient;

    @Resource
    private ApacheDubboGenericService dubboGenericService;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        FilterResult pfr = WebUtils.getPrevFilterResult(exchange);
        if (pfr.success) {
            return doFilter0(exchange, chain);
        } else {
            Mono<Void> resp = WebUtils.getDirectResponse(exchange);
            if (resp == null) { // should not reach here
                ServerHttpRequest clientReq = exchange.getRequest();
                String rid = clientReq.getId();
                String msg = pfr.id + " fail";
                if (pfr.cause == null) {
                    log.error(msg, LogService.BIZ_ID, rid);
                } else {
                    log.error(msg, LogService.BIZ_ID, rid, pfr.cause);
                }
                return WebUtils.buildJsonDirectResponseAndBindContext(exchange, HttpStatus.OK, null, RespEntity.toJson(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, rid));
            } else {
                return resp;
            }
        }
    }

    private Mono<Void> doFilter0(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest req = exchange.getRequest();
        String rid = req.getId();
        ApiConfig ac = WebUtils.getApiConfig(exchange);
        HttpHeaders hdrs = null;
        if (ac.type != ApiConfig.Type.DUBBO) {
            hdrs = WebUtils.mergeAppendHeaders(exchange);
        }

        if (ac == null) {
            String pathQuery = WebUtils.getClientReqPathQuery(exchange);
            return send(exchange, WebUtils.getClientService(exchange), pathQuery, hdrs);

        } else if (ac.type == ApiConfig.Type.SERVICE_DISCOVERY) {
            String pathQuery = WebUtils.appendQuery(WebUtils.getBackendPath(exchange), exchange);
            return send(exchange, WebUtils.getBackendService(exchange), pathQuery, hdrs);

        } else if (ac.type == ApiConfig.Type.REVERSE_PROXY) {
            String uri = ThreadContext.getStringBuilder().append(ac.getNextHttpHostPort())
                                                         .append(WebUtils.appendQuery(WebUtils.getBackendPath(exchange), exchange))
                                                         .toString();
            Object requestBody = WebUtils.getConvertedRequestBody(exchange);
            if (requestBody == null) {
                requestBody = WebUtils.getRequestBody(exchange);
            }
            return fizzWebClient.send(rid, req.getMethod(), uri, hdrs, requestBody).flatMap(genServerResponse(exchange));

        } else if (ac.type == ApiConfig.Type.DUBBO) {
            return dubboRpc(exchange, ac);

        } else {
            String err = "cant handle api config type " + ac.type;
            StringBuilder b = ThreadContext.getStringBuilder();
            WebUtils.request2stringBuilder(exchange, b);
            log.error(b.append(Constants.Symbol.LF).append(err).toString(), LogService.BIZ_ID, rid);
            return WebUtils.buildJsonDirectResponseAndBindContext(exchange, HttpStatus.OK, null, RespEntity.toJson(HttpStatus.INTERNAL_SERVER_ERROR.value(), err, rid));
        }
    }

    private Mono<Void> send(ServerWebExchange exchange, String service, String relativeUri, HttpHeaders hdrs) {
        ServerHttpRequest clientReq = exchange.getRequest();
        Object requestBody = WebUtils.getConvertedRequestBody(exchange);
        if (requestBody == null) {
            requestBody = WebUtils.getRequestBody(exchange);
        }
        return fizzWebClient.send2service(clientReq.getId(), clientReq.getMethod(), service, relativeUri, hdrs, requestBody).flatMap(genServerResponse(exchange));
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
                String rid = exchange.getRequest().getId();
                WebUtils.response2stringBuilder(rid, remoteResp, b);
                log.debug(b.toString(), LogService.BIZ_ID, rid);
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

    private Mono<Void> dubboRpc(ServerWebExchange exchange, ApiConfig ac) {

        DataBuffer b = WebUtils.getRequestBody(exchange);
        HashMap<String, Object> parameters = null;
        String json = Constants.Symbol.EMPTY;
        if (b != null) {
            json = b.toString(StandardCharsets.UTF_8).trim();
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
        String finalJson = json;

        DubboInterfaceDeclaration declaration = new DubboInterfaceDeclaration();
        declaration.setServiceName(ac.backendService);
        declaration.setVersion(ac.rpcVersion);
        declaration.setGroup(ac.rpcGroup);
        declaration.setMethod(ac.rpcMethod);
        declaration.setParameterTypes(ac.rpcParamTypes);
        int t = 20_000;
        if (ac.timeout != 0) {
            t = (int) ac.timeout;
        }
        declaration.setTimeout(t);

        Map<String, String> attachments = Collections.singletonMap(CommonConstants.HEADER_TRACE_ID, WebUtils.getTraceId(exchange));
        return dubboGenericService.send(parameters, declaration, attachments)
        .flatMap(
                dubboRpcResponseBody -> {
                    Mono<Void> m = WebUtils.buildJsonDirectResponse(exchange, HttpStatus.OK, null, JacksonUtils.writeValueAsString(dubboRpcResponseBody));
                    return m;
                }
        )
        .doOnError(
                e -> {
                    StringBuilder sb = ThreadContext.getStringBuilder();
                    WebUtils.request2stringBuilder(exchange, sb);
                    sb.append('\n').append(finalJson);
                    log.error(sb.toString(), LogService.BIZ_ID, exchange.getRequest().getId(), e);
                }
        )
        ;
    }
}
