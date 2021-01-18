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
import we.flume.clients.log4j2appender.LogService;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.util.JacksonUtils;
import we.util.WebUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

//@Component(GlobalFlowControlFilter.GLOBAL_FLOW_CONTROL_FILTER)
//@Order(-4)
public class GlobalFlowControlFilter extends AbsFlowControlFilter {

    public static final String GLOBAL_FLOW_CONTROL_FILTER = "globalFlowControlFilter";

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        if (flowControl) {

            // Map<String, Object> traceMap = new HashMap<>();
            LogService.setBizId(exchange.getRequest().getId());
            long currentTimeSlot = flowStat.currentTimeSlotId();
            // traceMap.put("currentTimeSlot", currentTimeSlot);

            exchange.getAttributes().put(AbsFlowControlFilter.currentTimeSlot, currentTimeSlot);
            ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.GLOBAL);
            if (config.isEnable()) {
                // traceMap.put("globalConfig", "enable conns " + config.concurrents + " and incr now");
                boolean concurrentOrRpsExceed = !flowStat.incrRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, config.concurrents, config.qps);
                if (concurrentOrRpsExceed) {
                    // traceMap.put("globalConfigExceed", "true");
                    return generateExceedResponse(exchange, config);
                }
            } else {
                // traceMap.put("noGlobalConfig", "incr now");
                flowStat.incrRequest(ResourceRateLimitConfig.GLOBAL, currentTimeSlot, null, null);
            }

            // if (log.isDebugEnabled()) {
            //     log.debug(JacksonUtils.writeValueAsString(traceMap), LogService.BIZ_ID, exchange.getRequest().getId());
            // }
            // StringBuilder b = new StringBuilder();
            // WebUtils.request2stringBuilder(exchange, b);
            // b.append('\n');

            long start = System.currentTimeMillis();
            exchange.getAttributes().put(AbsFlowControlFilter.start, start);
                return chain.filter(exchange)
                        // .doOnSuccess(
                        //         r -> {
                        //             // b.append(" succ ");
                        //             // inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, true);
                        //         }
                        // )
                        // .doOnError(
                        //         t -> {
                        //             // b.append(" errs ");
                        //             // inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, false);
                        //         }
                        // )
                        // .doOnCancel(
                        //         () -> {
                        //             // b.append(" cans ");
                        //             // inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, false);
                        //         }
                        // )
                        .doFinally(
                                s -> {
                                    if (s == SignalType.ON_COMPLETE) {
                                        // b.append(" comps ");
                                        inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, true);
                                    } else {
                                        // b.append(" " + s);
                                        inTheEnd(exchange, ResourceRateLimitConfig.GLOBAL, start, currentTimeSlot, false);
                                    }
                                    // if (log.isDebugEnabled()) {
                                    //     log.debug(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());
                                    // }
                                }
                        );
        }
        return chain.filter(exchange);
    }
}
