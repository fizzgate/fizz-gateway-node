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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import com.alibaba.nacos.api.config.annotation.NacosValue;

import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import we.flume.clients.log4j2appender.LogService;
import we.stats.BlockType;
import we.stats.FlowStat;
import we.stats.IncrRequestResult;
import we.stats.ResourceConfig;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.WebUtils;

/**
 * @author hongqiaowei
 */

@Component(FlowControlFilter.FLOW_CONTROL_FILTER)
@Order(-10)
public class FlowControlFilter extends FizzWebFilter {

	public static final String FLOW_CONTROL_FILTER = "flowControlFilter";

	private static final Logger log = LoggerFactory.getLogger(FlowControlFilter.class);

	@NacosValue(value = "${flowControl:false}", autoRefreshed = true)
	@Value("${flowControl:false}")
	private boolean flowControl;

	@Resource
	private ResourceRateLimitConfigService resourceRateLimitConfigService;

	// @Resource
	@Autowired(required = false)
	private FlowStat flowStat;

	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

		if (flowControl) {
			String service = WebUtils.getClientService(exchange);
//			String reqPath = WebUtils.getClientReqPath(exchange);
			long currentTimeSlot = flowStat.currentTimeSlotId();
			ResourceRateLimitConfig globalConfig = resourceRateLimitConfigService
					.getResourceRateLimitConfig(ResourceRateLimitConfig.GLOBAL);
			ResourceRateLimitConfig serviceConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(service);
			if (serviceConfig == null) {
				serviceConfig = resourceRateLimitConfigService
						.getResourceRateLimitConfig(ResourceRateLimitConfig.SERVICE_DEFAULT);
			}

			// global
			List<ResourceConfig> resourceConfigs = new ArrayList<>();
			ResourceConfig globalResCfg = new ResourceConfig(ResourceRateLimitConfig.GLOBAL, 0, 0);
			if (globalConfig != null && globalConfig.isEnable()) {
				globalResCfg.setMaxCon(globalConfig.concurrents);
				globalResCfg.setMaxQPS(globalConfig.qps);
			}
			resourceConfigs.add(globalResCfg);

			// service
			ResourceConfig serviceResCfg = new ResourceConfig(service, 0, 0);
			if (serviceConfig != null && serviceConfig.isEnable()) {
				serviceResCfg.setMaxCon(serviceConfig.concurrents);
				serviceResCfg.setMaxQPS(serviceConfig.qps);
			}
			resourceConfigs.add(serviceResCfg);

			IncrRequestResult result = flowStat.incrRequest(resourceConfigs, currentTimeSlot);

			if (result != null && !result.isSuccess()) {
				if (BlockType.CONCURRENT_REQUEST == result.getBlockType()) {
					log.info("exceed {} flow limit, blocked by maximum concurrent requests",
							result.getBlockedResourceId(), LogService.BIZ_ID, exchange.getRequest().getId());
				} else {
					log.info("exceed {} flow limit, blocked by maximum QPS", result.getBlockedResourceId(),
							LogService.BIZ_ID, exchange.getRequest().getId());
				}

//				ResourceRateLimitConfig config = result.getBlockedResourceId().equals(globalConfig.resource)
//						? globalConfig
//						: serviceConfig;

				ServerHttpResponse resp = exchange.getResponse();
				resp.setStatusCode(HttpStatus.OK);
				resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, globalConfig.responseType);
				return resp.writeWith(Mono.just(resp.bufferFactory().wrap(globalConfig.responseContent.getBytes())));
			} else {
				long start = System.currentTimeMillis();
				return chain.filter(exchange).doFinally(s -> {
					long rt = System.currentTimeMillis() - start;
					if (s == SignalType.ON_COMPLETE) {
						flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, true);
					} else {
						flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, false);
					}
				});
			}
		}

		return chain.filter(exchange);
	}
}
