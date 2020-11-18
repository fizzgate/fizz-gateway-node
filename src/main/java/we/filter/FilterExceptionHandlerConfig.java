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

import java.net.URI;

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
import we.exception.RedirectException;
import we.exception.StopAndResponseException;
import we.util.WebUtils;

/**
 * @author hongqiaowei
 */

@Configuration
public class FilterExceptionHandlerConfig {

    public static class FilterExceptionHandler implements WebExceptionHandler {
        private static final String filterExceptionHandler = "filterExceptionHandler";
        @Override
        public Mono<Void> handle(ServerWebExchange exchange, Throwable t) {
        	if (t instanceof StopAndResponseException) {
                StopAndResponseException ex = (StopAndResponseException) t;
                if (ex.getData() != null) {
                    ServerHttpResponse resp = exchange.getResponse();
                    resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    return resp.writeWith(Mono.just(resp.bufferFactory().wrap(ex.getData().toString().getBytes())));
                }
            }
        	if (t instanceof RedirectException) {
        		RedirectException ex = (RedirectException) t;
                if (ex.getRedirectUrl() != null) {
                    ServerHttpResponse resp = exchange.getResponse();
                    resp.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
                    resp.getHeaders().setLocation(URI.create(ex.getRedirectUrl()));
                    return Mono.empty();
                }
            }
        	Mono<Void> vm = WebUtils.responseError(exchange, filterExceptionHandler, HttpStatus.INTERNAL_SERVER_ERROR.value(), t.getMessage(), t);
        	return vm;
        }
    }

    @Bean
    @Order(-2)
    public FilterExceptionHandler filterExceptionHandler() {
        return new FilterExceptionHandler();
    }
}
