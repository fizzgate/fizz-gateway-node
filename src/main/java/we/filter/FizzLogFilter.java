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

import we.flume.clients.log4j2appender.LogService;
import we.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author lancer
 */
@Component
@Order(0)
public class FizzLogFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzLogFilter.class);

    private static final String resp   = "\nresponse ";

    private static final String in     = " in ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange).doAfterTerminate(
                () -> {
                    StringBuilder b = new StringBuilder(160);
                    WebUtils.request2stringBuilder(exchange, b);
                    b.append(resp).append(exchange.getResponse().getStatusCode())
                     .append(in)  .append(System.currentTimeMillis() - startTime);
                    LOGGER.info(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());
                }
        );
    }
}
