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
import we.stats.ratelimit.ResourceRateLimitConfig;

/**
 * @author hongqiaowei
 */

@Component(GlobalFlowControlFilter.GLOBAL_FLOW_CONTROL_FILTER)
@Order(-4)
public class GlobalFlowControlFilter extends AbsFlowControlFilter {

    public static final String GLOBAL_FLOW_CONTROL_FILTER = "globalFlowControlFilter";

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        if (flowControl) {
            long currentTimeSlot = flowStat.currentTimeSlotId();
            exchange.getAttributes().put(AbsFlowControlFilter.currentTimeSlot, currentTimeSlot);
            ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.GLOBAL);
            if (config.isEnable()) {
                boolean concurrentOrRpsExceed = !flowStat.incrRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, config.concurrents, config.qps);
                if (concurrentOrRpsExceed) {
                    return generateExceedResponse(exchange, config);
                }
            } else {
                flowStat.incrRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, null, null);
            }

            long start = System.currentTimeMillis();
            exchange.getAttributes().put(AbsFlowControlFilter.start, start);
                return chain.filter(exchange)
                        .doOnSuccess(
                                r -> {
                                    inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, true);
                                }
                        )
                        .doOnError(
                                t -> {
                                    inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, false);
                                }
                        )
                        .doOnCancel(
                                () -> {
                                    inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, false);
                                }
                        );
        }
        return chain.filter(exchange);
    }
}
