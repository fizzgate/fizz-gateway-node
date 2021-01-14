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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
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

public abstract class AbsFlowControlFilter extends ProxyAggrFilter {

    private static final Logger log = LoggerFactory.getLogger(AbsFlowControlFilter.class);

    protected static final String exceed          = " exceed ";
    protected static final String concurrents     = " concurrents ";
    protected static final String orQps           = " or qps ";
    protected static final String currentTimeSlot = "currentTimeSlot";
    protected static final String start           = "start";

    @NacosValue(value = "${flowControl:false}", autoRefreshed = true)
    @Value("${flowControl:false}")
    protected boolean flowControl;

    @Resource
    protected ResourceRateLimitConfigService resourceRateLimitConfigService;

    @Resource
    protected FlowStat flowStat;

    protected Mono<Void> generateExceedResponse(ServerWebExchange exchange, ResourceRateLimitConfig config) {
        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(WebUtils.getClientService(exchange)).append(Constants.Symbol.SPACE).append(WebUtils.getClientReqPath(exchange));
        b.append(exceed)                             .append(config.resource)       .append(concurrents)                         .append(config.concurrents).append(orQps).append(config.qps);
        log.warn(b.toString(), LogService.BIZ_ID, exchange.getRequest().getId());

        ServerHttpResponse resp = exchange.getResponse();
        resp.setStatusCode(HttpStatus.OK);
        resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, config.responseType);
        return resp.writeWith(Mono.just(resp.bufferFactory().wrap(config.responseContent.getBytes())));
    }

    protected void inTheEnd(ServerWebExchange exchange, String resource, long start, long currentTimeSlot, boolean success) {
        long spend = System.currentTimeMillis() - start;
        flowStat.decrConcurrentRequest(resource, currentTimeSlot);
        flowStat.addRequestRT(resource, currentTimeSlot, spend, success);
    }
}
