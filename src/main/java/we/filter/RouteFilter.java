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
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.plugin.auth.ApiConfig;
import we.plugin.auth.AuthPluginFilter;
import we.proxy.FizzWebClient;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author hongqiaowei
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RouteFilter extends ProxyAggrFilter {

    private static final Logger log = LoggerFactory.getLogger(RouteFilter.class);

    @Resource
    private FizzWebClient fizzWebClient;

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

        ServerHttpRequest clientReq = exchange.getRequest();
        HttpHeaders hdrs = new HttpHeaders();
        clientReq.getHeaders().forEach(
                (h, vs) -> {
                    hdrs.addAll(h, vs);
                }
        );
        Map<String, String> appendHeaders = WebUtils.getAppendHeaders(exchange);
        if (appendHeaders != null) {
            appendHeaders.forEach(
                    (h, v) -> {
                        List<String> vs = hdrs.get(h);
                        if (vs != null && !vs.isEmpty()) {
                            vs.clear();
                            vs.add(v);
                        } else {
                            hdrs.add(h, v);
                        }
                    }
            );
        }

        ApiConfig ac = null;
        Object authRes = WebUtils.getFilterResultDataItem(exchange, AuthPluginFilter.AUTH_PLUGIN_FILTER, AuthPluginFilter.RESULT);
        if (authRes instanceof ApiConfig) {
            ac = (ApiConfig) authRes;
        }

        String relativeUri = WebUtils.getRelativeUri(exchange);
        if (ac == null || ac.type == ApiConfig.DIRECT_PROXY_MODE) {
            return send(exchange, WebUtils.getServiceId(exchange), relativeUri, hdrs);
        } else {
            String realUri;
            String backendUrl = ac.getNextBackendUrl();
            int acpLen = ac.path.length();
            if (acpLen == 1) {
                realUri = backendUrl + relativeUri;
            } else {
                realUri = backendUrl + relativeUri.substring(acpLen);
            }
            relativeUri.substring(acpLen);
            return fizzWebClient.send(clientReq.getId(), clientReq.getMethod(), realUri, hdrs, clientReq.getBody()).flatMap(genServerResponse(exchange));
        }
    }

    private Mono<Void> send(ServerWebExchange exchange, String service, String relativeUri, HttpHeaders hdrs) {
        ServerHttpRequest clientReq = exchange.getRequest();
        return fizzWebClient.proxySend2service(clientReq.getId(), clientReq.getMethod(), service, relativeUri, hdrs, clientReq.getBody()).flatMap(genServerResponse(exchange));
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
                                    || k.equals(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS) || k.equals(HttpHeaders.ACCESS_CONTROL_MAX_AGE)) {
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
}
