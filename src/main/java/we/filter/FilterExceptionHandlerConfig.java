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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import we.exception.ExecuteScriptException;
import we.exception.RedirectException;
import we.exception.StopAndResponseException;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.util.JacksonUtils;
import we.util.ThreadContext;
import we.util.WebUtils;

import java.net.URI;

/**
 * @author hongqiaowei
 */

@Configuration
public class FilterExceptionHandlerConfig {

    public static class FilterExceptionHandler implements WebExceptionHandler {
        private static final Logger log = LoggerFactory.getLogger(FilterExceptionHandler.class);
        private static final String filterExceptionHandler = "filterExceptionHandler";
        @Override
        public Mono<Void> handle(ServerWebExchange exchange, Throwable t) {
            ServerHttpResponse resp = exchange.getResponse();
        	if (t instanceof StopAndResponseException) {
                StopAndResponseException ex = (StopAndResponseException) t;
                if (ex.getData() != null) {
                    resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(ex.getData().toString().getBytes())));
                }
            }
            if (t instanceof RedirectException) {
                RedirectException ex = (RedirectException) t;
                if (ex.getRedirectUrl() != null) {
                    resp.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
                    resp.getHeaders().setLocation(URI.create(ex.getRedirectUrl()));
                    return Mono.empty();
                }
            }
            if (t instanceof ExecuteScriptException) {
                ExecuteScriptException ex = (ExecuteScriptException) t;
                resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                RespEntity rs = null;
                String reqId = exchange.getRequest().getId();
                if (ex.getStepContext() != null && ex.getStepContext().returnContext()) {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), reqId, ex.getStepContext());
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(JacksonUtils.writeValueAsString(rs).getBytes())));
                } else {
                    rs = new RespEntity(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), reqId);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(rs.toString().getBytes())));
                }
            }
            Mono<Void> vm;
            Object fc = exchange.getAttributes().get(WebUtils.FILTER_CONTEXT);
            if (fc == null) { // t came from flow control filter
                StringBuilder b = ThreadContext.getStringBuilder();
                WebUtils.request2stringBuilder(exchange, b);
                log.error(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId(), t);
                String s = RespEntity.toJson(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), exchange.getRequest().getId());
                vm = resp.writeWith(Mono.just(resp.bufferFactory().wrap(s.getBytes())));
            } else {
                vm = WebUtils.responseError(exchange, filterExceptionHandler, HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), t);
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
