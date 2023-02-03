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

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fizzgate.util.WebUtils;

import reactor.core.publisher.Mono;

/**
 * @author hongqiaowei
 */

public abstract class FizzWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (WebUtils.isAdminReq(exchange) || WebUtils.isFizzReq(exchange) || WebUtils.isFavReq(exchange)) {
            return chain.filter(exchange);
        } else {
            return doFilter(exchange, chain);
        }
    }

    public abstract Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain);
}
