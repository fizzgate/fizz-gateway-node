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

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.util.WebUtils;

/**
 * @author hongqiaowei
 */

@Component(ServiceFlowControlFilter.SERVICE_FLOW_CONTROL_FILTER)
@Order(-3)
public class ServiceFlowControlFilter extends AbsFlowControlFilter {

    public static final String SERVICE_FLOW_CONTROL_FILTER = "serviceFlowControlFilter";

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        if (flowControl) {
            long currentTimeSlot = exchange.getAttribute(AbsFlowControlFilter.currentTimeSlot);
            String service = WebUtils.getClientService(exchange);
            ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(service);

            if (config == null) {
                config = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.SERVICE_DEFAULT);
            }
            if (config == null || !config.isEnable()) {
                flowStat.incrRequest(service, currentTimeSlot, null, null);
            } else {
                boolean concurrentOrRpsExceed = !flowStat.incrRequest(service, currentTimeSlot, config.concurrents, config.qps);
                if (concurrentOrRpsExceed) {
                    return generateExceedResponse(exchange, config);
                }
            }

            long start = exchange.getAttribute(AbsFlowControlFilter.start);
            return chain.filter(exchange)
                    // .doOnSuccess(
                    //         r -> {
                    //             inTheEnd(exchange, service, start, currentTimeSlot, true);
                    //         }
                    // )
                    // .doOnError(
                    //         t -> {
                    //             inTheEnd(exchange, service, start, currentTimeSlot, false);
                    //         }
                    // )
                    // .doOnCancel(
                    //         () -> {
                    //             inTheEnd(exchange, service, start, currentTimeSlot, false);
                    //         }
                    // )
                    .doFinally(
                            s -> {
                                if (s == SignalType.ON_COMPLETE) {
                                    inTheEnd(exchange, service, start, currentTimeSlot, true);
                                } else {
                                    inTheEnd(exchange, service, start, currentTimeSlot, false);
                                }
                            }
                    );
        }
        return chain.filter(exchange);
    }
}
