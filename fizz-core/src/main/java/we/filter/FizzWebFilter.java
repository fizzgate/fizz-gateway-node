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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.util.Constants;
import we.util.Utils;
import we.util.WebUtils;

/**
 * @author hongqiaowei
 */

public abstract class FizzWebFilter implements WebFilter {

    private static final String admin    = "admin";
    private static final String actuator = "actuator";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getAttribute(FizzLogFilter.ADMIN_REQUEST) == null) {
            return doFilter(exchange, chain);
        } else {
            return chain.filter(exchange);
        }
    }

    public abstract Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain);
}
