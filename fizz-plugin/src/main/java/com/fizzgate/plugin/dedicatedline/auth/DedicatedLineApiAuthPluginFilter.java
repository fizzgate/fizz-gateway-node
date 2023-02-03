/*
 *  Copyright (C) 2021 the original author or authors.
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

package com.fizzgate.plugin.dedicatedline.auth;

import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fizzgate.dedicated_line.DedicatedLineService;
import com.fizzgate.plugin.FizzPluginFilter;
import com.fizzgate.plugin.FizzPluginFilterChain;
import com.fizzgate.util.Consts;
import com.fizzgate.util.ReactorUtils;
import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author Francis Dong
 */
@ConditionalOnBean(DedicatedLineService.class)
@Component(DedicatedLineApiAuthPluginFilter.DEDICATED_LINE_API_AUTH_PLUGIN_FILTER)
public class DedicatedLineApiAuthPluginFilter implements FizzPluginFilter {

    private static final Logger log = LoggerFactory.getLogger(DedicatedLineApiAuthPluginFilter.class);

    @Resource
    private DedicatedLineService dedicatedLineService;

    public static final String DEDICATED_LINE_API_AUTH_PLUGIN_FILTER = "dedicatedLineApiAuthPlugin";

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        String traceId = WebUtils.getTraceId(exchange);
        try {
            String dedicatedLineId = WebUtils.getDedicatedLineId(exchange);
            String service = WebUtils.getClientService(exchange);
            String path = WebUtils.getClientReqPath(exchange);
            HttpMethod method = exchange.getRequest().getMethod();
            if (dedicatedLineService.auth(dedicatedLineId, method, service, path)) {
                // Go to next plugin
                Mono next = FizzPluginFilterChain.next(exchange);
                return next.defaultIfEmpty(ReactorUtils.NULL).flatMap(nil -> {
                    doAfter();
                    return Mono.empty();
                });
            } else {
                // Auth failed
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setCacheControl("no-store");
                response.getHeaders().setExpires(0);
                String respJson = WebUtils.jsonRespBody(HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.getReasonPhrase(), traceId);
                return WebUtils.response(exchange, HttpStatus.UNAUTHORIZED, null, respJson);
            }
        } catch (Exception e) {
            // log.error("{} {} exception", traceId, DEDICATED_LINE_API_AUTH_PLUGIN_FILTER, LogService.BIZ_ID, traceId, e);
            ThreadContext.put(Consts.TRACE_ID, traceId);
            log.error("{} {} exception", traceId, DEDICATED_LINE_API_AUTH_PLUGIN_FILTER, e);
            String respJson = WebUtils.jsonRespBody(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), traceId);
            return WebUtils.response(exchange, HttpStatus.INTERNAL_SERVER_ERROR, null, respJson);
        }
    }

    public void doAfter() {
    }
}
