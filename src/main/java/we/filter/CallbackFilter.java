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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.proxy.FizzWebClient;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@Component(CallbackFilter.CALLBACK_FILTER)
@Order(20)
public class CallbackFilter extends ProxyAggrFilter {

    private static final Logger log             = LoggerFactory.getLogger(CallbackFilter.class);

    public  static final String CALLBACK_FILTER = "callbackFilter";

    @Resource
    private FizzWebClient fizzWebClient;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange);
    }
}
