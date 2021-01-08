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

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.stats.FlowStat;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.Constants;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@Component(FlowControlFilter.FLOW_CONTROL_FILTER)
@Order(-1)
public class FlowControlFilter extends ProxyAggrFilter {

    public  static final String FLOW_CONTROL_FILTER = "flowControlFilter";

    private static final Logger log                 = LoggerFactory.getLogger(FlowControlFilter.class);

    private static final String exceed              = " exceed ";
    private static final String concurrents         = " concurrents ";
    private static final String orQps               = " or qps ";

    @NacosValue(value = "${flowControl:false}", autoRefreshed = true)
    @Value("${flowControl:false}")
    private boolean flowControl;

    @Resource
    private ResourceRateLimitConfigService resourceRateLimitConfigService;

    private FlowStat flowStat = new FlowStat();

    public FlowStat getFlowStat() {
        return flowStat;
    }

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

        if (flowControl) {
            long currentTimeSlot = flowStat.currentTimeSlotId();
            ResourceRateLimitConfig rlc = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.GLOBAL);
            ResourceRateLimitConfig globalConfig = rlc;

            boolean concurrentOrRpsExceed = false;
            if (rlc.isEnable()) {
                concurrentOrRpsExceed = !flowStat.incrRequest(rlc.resource, currentTimeSlot, rlc.concurrents, rlc.qps);
            }

            if (!concurrentOrRpsExceed) {
                String reqPath = WebUtils.getClientReqPath(exchange);
                rlc = resourceRateLimitConfigService.getResourceRateLimitConfig(reqPath);
                if (rlc == null) {
                    String service = WebUtils.getClientService(exchange);
                    rlc = resourceRateLimitConfigService.getResourceRateLimitConfig(service);
                    if (rlc == null) {
                        rlc = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.SERVICE_DEFAULT);
                        if (rlc == null || !rlc.isEnable()) {
                        } else {
                            concurrentOrRpsExceed = !flowStat.incrRequest(service, currentTimeSlot, rlc.concurrents, rlc.qps);
                        }
                    } else {
                        concurrentOrRpsExceed = !flowStat.incrRequest(service, currentTimeSlot, rlc.concurrents, rlc.qps);
                    }
                } else { // should not reach here for now
                    concurrentOrRpsExceed = !flowStat.incrRequest(reqPath, currentTimeSlot, rlc.concurrents, rlc.qps);
                }
            }

            if (log.isDebugEnabled()) {
                log.debug(WebUtils.getClientReqPath(exchange) + " apply rate limit rule: " + rlc, LogService.BIZ_ID, exchange.getRequest().getId());
            }

            if (concurrentOrRpsExceed) {

                StringBuilder b = ThreadContext.getStringBuilder();
                b.append(WebUtils.getClientService(exchange)).append(Constants.Symbol.SPACE).append(WebUtils.getClientReqPath(exchange));
                b.append(exceed).append(rlc.resource).append(concurrents).append(rlc.concurrents).append(orQps).append(rlc.qps);
                log.warn(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());

                ServerHttpResponse resp = exchange.getResponse();
                resp.setStatusCode(HttpStatus.OK);
                resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, globalConfig.responseType);
                return resp.writeWith(Mono.just(resp.bufferFactory().wrap(globalConfig.responseContent.getBytes())));

            } else {

                long start = System.currentTimeMillis();
                ResourceRateLimitConfig rlcCopy = rlc;
                return chain.filter(exchange)
                        .doAfterTerminate(
                                () -> {
                                    inTheEnd(start, globalConfig, rlcCopy, currentTimeSlot, true);
                                }
                        )
                        .doOnError(
                                t -> {
                                    inTheEnd(start, globalConfig, rlcCopy, currentTimeSlot, false);
                                }
                        );
            }
        }
        return chain.filter(exchange);
    }

    private void inTheEnd(long start, ResourceRateLimitConfig globalConfig, ResourceRateLimitConfig apiOrServiceConfig, long currentTimeSlot, boolean success) {
        long spend = System.currentTimeMillis() - start;
        if (globalConfig.isEnable()) {
            flowStat.decrConcurrentRequest(globalConfig.resource, currentTimeSlot);
            flowStat.addRequestRT(globalConfig.resource, currentTimeSlot, spend, success);
        }
        if (globalConfig != apiOrServiceConfig) {
            flowStat.decrConcurrentRequest(apiOrServiceConfig.resource, currentTimeSlot);
            flowStat.addRequestRT(apiOrServiceConfig.resource, currentTimeSlot, spend, success);
        }
    }
}
