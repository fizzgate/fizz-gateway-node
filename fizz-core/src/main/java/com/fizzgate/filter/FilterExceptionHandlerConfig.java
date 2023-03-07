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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import com.fizzgate.Fizz;
import com.fizzgate.config.SystemConfig;
import com.fizzgate.exception.ExecuteScriptException;
import com.fizzgate.exception.RedirectException;
import com.fizzgate.exception.StopAndResponseException;
import com.fizzgate.fizz.exception.FizzRuntimeException;
import com.fizzgate.legacy.RespEntity;
import com.fizzgate.util.Consts;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ThreadContext;
import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @author hongqiaowei
 */

@Configuration
public class FilterExceptionHandlerConfig {

    public static class FilterExceptionHandler implements WebExceptionHandler {

        private static final Logger LOGGER = LoggerFactory.getLogger(FilterExceptionHandler.class);
        private static final String filterExceptionHandler = "filterExceptionHandler";

        @Override
        public Mono<Void> handle(ServerWebExchange exchange, Throwable t) {
            exchange.getAttributes().put(WebUtils.ORIGINAL_ERROR, t);
            String traceId = WebUtils.getTraceId(exchange);

            ServerHttpResponse resp = exchange.getResponse();
            if (SystemConfig.FIZZ_ERR_RESP_HTTP_STATUS_ENABLE) {
                if (t instanceof ResponseStatusException) {
                    resp.setStatusCode( ((ResponseStatusException) t).getStatus() );
                } else {
                    resp.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            HttpHeaders respHeaders = resp.getHeaders();
            String value = Fizz.context.getEnvironment().getProperty(SystemConfig.FIZZ_DEDICATED_LINE_CLIENT_ENABLE);
            if (StringUtils.isNotBlank(value) && value.equals(Consts.S.TRUE)) {
                respHeaders.set(WebUtils.BODY_ENCRYPT, Consts.S.FALSE0);
            }

            if (t instanceof StopAndResponseException) {
                StopAndResponseException ex = (StopAndResponseException) t;
                if (ex.getData() != null) {
                    respHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(ex.getData().toString().getBytes())));
                }
            }
            if (t instanceof RedirectException) {
                RedirectException ex = (RedirectException) t;
                if (ex.getRedirectUrl() != null) {
                    resp.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
                    respHeaders.setLocation(URI.create(ex.getRedirectUrl()));
                    return Mono.empty();
                }
            }

            String tMsg = t.getMessage();
            if (tMsg == null) {
                tMsg = t.toString();
            }
            if (t instanceof ExecuteScriptException) {
                ExecuteScriptException ex = (ExecuteScriptException) t;
                respHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                RespEntity rs = null;
                if (ex.getStepContext() != null && ex.getStepContext().returnContext()) {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), tMsg, traceId, ex.getStepContext());
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(JacksonUtils.writeValueAsBytes(rs))));
                } else {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), tMsg, traceId);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(rs.toString().getBytes())));
                }
            }

            if (t instanceof FizzRuntimeException) {
                FizzRuntimeException ex = (FizzRuntimeException) t;
                org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
                LOGGER.error(tMsg, ex);
                respHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                RespEntity rs = null;
                if (ex.getStepContext() != null && ex.getStepContext().returnContext()) {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), tMsg, traceId, ex.getStepContext());
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(JacksonUtils.writeValueAsString(rs).getBytes())));
                } else {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), tMsg, traceId);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(rs.toString().getBytes())));
                }
            }

            Mono<Void> vm;
            Object fc = exchange.getAttribute(WebUtils.FILTER_CONTEXT);
            if (fc == null) { // t came from flow control filter
                StringBuilder b = ThreadContext.getStringBuilder();
                WebUtils.request2stringBuilder(exchange, b);
                org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
                LOGGER.error(b.toString(), t);
                String s = WebUtils.jsonRespBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), tMsg, traceId);
                respHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                vm = resp.writeWith(Mono.just(resp.bufferFactory().wrap(s.getBytes())));
            } else {
                vm = WebUtils.responseError(exchange, filterExceptionHandler, HttpStatus.INTERNAL_SERVER_ERROR.value(), tMsg, t);
            }
            return vm;
        }
    }

    @Bean
    @Order(-10)
    public FilterExceptionHandler filterExceptionHandler() {
        return new FilterExceptionHandler();
    }
}
