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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.util.Constants;
import we.util.ThreadContext;
import we.util.WebUtils;

/**
 * @author hongqiaowei
 */

@Component
@Order(0)
public class FizzLogFilter implements WebFilter {

    private static final Logger LOGGER        = LoggerFactory.getLogger(FizzLogFilter.class);

    private static final String resp          = "\nresponse ";

    private static final String in            = " in ";

    private static final String admin         = "admin";

    private static final String actuator      = "actuator";

    public  static final String ADMIN_REQUEST = "$a";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        long startTime = System.currentTimeMillis();

        String path = exchange.getRequest().getPath().value();
        int secFS = path.indexOf(Constants.Symbol.FORWARD_SLASH, 1);
        if (secFS == -1) {
            return WebUtils.responseError(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "request path should like /optional-prefix/service-name/real-biz-path");
        }
        String s = path.substring(1, secFS);
        if (s.equals(admin) || s.equals(actuator)) {
            exchange.getAttributes().put(ADMIN_REQUEST, Constants.Symbol.EMPTY);
        }

        return chain.filter(exchange).doAfterTerminate(
                () -> {
                    if (LOGGER.isInfoEnabled()) {
                        StringBuilder b = ThreadContext.getStringBuilder();
                        WebUtils.request2stringBuilder(exchange, b);
                        b.append(resp).append(exchange.getResponse().getStatusCode())
                                .append(in)  .append(System.currentTimeMillis() - startTime);
                        LOGGER.info(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());
                    }
                }
        );
    }
}
