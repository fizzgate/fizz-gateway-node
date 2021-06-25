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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.AppService;
import we.stats.BlockType;
import we.stats.FlowStat;
import we.stats.IncrRequestResult;
import we.stats.ResourceConfig;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.Constants;
import we.util.JacksonUtils;
import we.util.ThreadContext;
import we.util.WebUtils;

/**
 * @author hongqiaowei
 */

@Component(FlowControlFilter.FLOW_CONTROL_FILTER)
@Order(-10)
public class FlowControlFilter extends FizzWebFilter {

	public static final String FLOW_CONTROL_FILTER = "flowControlFilter";

	private static final Logger log = LoggerFactory.getLogger(FlowControlFilter.class);

	private static final String admin         = "admin";

	private static final String actuator      = "actuator";

	public  static final String ADMIN_REQUEST = "$a";

	@Resource
	private FlowControlFilterProperties flowControlFilterProperties;

	@Resource
	private ResourceRateLimitConfigService resourceRateLimitConfigService;

	@Autowired(required = false)
	private FlowStat flowStat;

	@Resource
	private ApiConfigService apiConfigService;

	@Resource
	private AppService appService;

	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

		String path = exchange.getRequest().getPath().value();
		int secFS = path.indexOf(Constants.Symbol.FORWARD_SLASH, 1);
		if (secFS == -1) {
			return WebUtils.responseError(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "request path should like /optional-prefix/service-name/real-biz-path");
		}
		String service = path.substring(1, secFS);
		boolean adminReq = false, proxyTestReq = false;
		if (service.equals(admin) || service.equals(actuator)) {
			adminReq = true;
			exchange.getAttributes().put(ADMIN_REQUEST, Constants.Symbol.EMPTY);
		} else if (service.equals(SystemConfig.DEFAULT_GATEWAY_TEST)) {
			proxyTestReq = true;
		} else {
			service = WebUtils.getClientService(exchange);
		}

		if (flowControlFilterProperties.isFlowControl() && !adminReq && !proxyTestReq) {
			LogService.setBizId(exchange.getRequest().getId());
			if (!apiConfigService.serviceConfigMap.containsKey(service)) {
				String json = RespEntity.toJson(HttpStatus.FORBIDDEN.value(), "no service " + service, exchange.getRequest().getId());
				return WebUtils.buildJsonDirectResponse(exchange, HttpStatus.FORBIDDEN, null, json);
			}
			String app = WebUtils.getAppId(exchange);
			if (app != null && !appService.getAppMap().containsKey(app)) {
				String json = RespEntity.toJson(HttpStatus.FORBIDDEN.value(), "no app " + app, exchange.getRequest().getId());
				return WebUtils.buildJsonDirectResponse(exchange, HttpStatus.FORBIDDEN, null, json);
			}
			path = WebUtils.getClientReqPath(exchange);
			String ip = WebUtils.getOriginIp(exchange);

			long currentTimeSlot = flowStat.currentTimeSlotId();
			List<ResourceConfig> resourceConfigs = getFlowControlConfigs(app, ip, null, service, path);
			IncrRequestResult result = flowStat.incrRequest(resourceConfigs, currentTimeSlot);

			if (result != null && !result.isSuccess()) {
				String blockedResourceId = result.getBlockedResourceId();
				if (BlockType.CONCURRENT_REQUEST == result.getBlockType()) {
					log.info("exceed {} flow limit, blocked by maximum concurrent requests", blockedResourceId, LogService.BIZ_ID, exchange.getRequest().getId());
				} else {
					log.info("exceed {} flow limit, blocked by maximum QPS", blockedResourceId, LogService.BIZ_ID, exchange.getRequest().getId());
				}

				ResourceRateLimitConfig c = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.NODE_RESOURCE);
				String rt = c.responseType, rc = c.responseContent;
				c = resourceRateLimitConfigService.getResourceRateLimitConfig(blockedResourceId);
				if (c != null) {
					if (StringUtils.isNotBlank(c.responseType)) {
						rt = c.responseType;
					}
					if (StringUtils.isNotBlank(c.responseContent)) {
						rc = c.responseContent;
					}
				}

				ServerHttpResponse resp = exchange.getResponse();
				resp.setStatusCode(HttpStatus.OK);
				resp.getHeaders().add(HttpHeaders.CONTENT_TYPE, rt);
				return resp.writeWith(Mono.just(resp.bufferFactory().wrap(rc.getBytes())));

			} else {
				long start = System.currentTimeMillis();
				return chain.filter(exchange).doFinally(s -> {
					long rt = System.currentTimeMillis() - start;
					if (s == SignalType.ON_ERROR || exchange.getResponse().getStatusCode().is5xxServerError()) {
						flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, false);
					} else {
						flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, true);
					}
				});
			}
		}

		return chain.filter(exchange);
	}

	private List<ResourceConfig> getFlowControlConfigs(String app, String ip, String node, String service, String path) {
		if (log.isDebugEnabled()) {
			log.debug("get flow control config by app={}, ip={}, node={}, service={}, path={}", app, ip, node, service, path);
		}
		List<ResourceConfig> resourceConfigs = new ArrayList<>(9);
		StringBuilder b = ThreadContext.getStringBuilder();

		checkRateLimitConfigAndAddTo(resourceConfigs, b, null, null, ResourceRateLimitConfig.NODE, null, null, null);
		checkRateLimitConfigAndAddTo(resourceConfigs, b, null, null, null, service, null, ResourceRateLimitConfig.SERVICE_DEFAULT);
		checkRateLimitConfigAndAddTo(resourceConfigs, b, null, null, null, service, path, null);

		if (app != null) {
			checkRateLimitConfigAndAddTo(resourceConfigs, b, app, null, null, null, null, ResourceRateLimitConfig.APP_DEFAULT);
			checkRateLimitConfigAndAddTo(resourceConfigs, b, app, null, null, service, null, null);
			checkRateLimitConfigAndAddTo(resourceConfigs, b, app, null, null, service, path, null);
		}

		if (ip != null) {
			checkRateLimitConfigAndAddTo(resourceConfigs, b, null, ip, null, null, null, null);
			checkRateLimitConfigAndAddTo(resourceConfigs, b, null, ip, null, service, null, null);
			checkRateLimitConfigAndAddTo(resourceConfigs, b, null, ip, null, service, path, null);
		}

		if (log.isDebugEnabled()) {
			log.debug("resource configs: " + JacksonUtils.writeValueAsString(resourceConfigs));
		}
		return resourceConfigs;
	}

	private void checkRateLimitConfigAndAddTo(List<ResourceConfig> resourceConfigs, StringBuilder b, String app, String ip, String node, String service, String path, String defaultRateLimitConfigId) {
		ResourceRateLimitConfig.buildResourceIdTo(b, app, ip, node, service, path);
		String resourceId = b.toString();
		checkRateLimitConfigAndAddTo(resourceConfigs, resourceId, defaultRateLimitConfigId);
		b.delete(0, b.length());
	}

	private void checkRateLimitConfigAndAddTo(List<ResourceConfig> resourceConfigs, String resource, String defaultRateLimitConfigId) {
		ResourceConfig rc = null;
		ResourceRateLimitConfig rateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(resource);
		if (rateLimitConfig != null && rateLimitConfig.isEnable()) {
			rc = new ResourceConfig(resource, rateLimitConfig.concurrents, rateLimitConfig.qps);
			resourceConfigs.add(rc);
		} else {
			String node = ResourceRateLimitConfig.getNode(resource);
			if (node != null && node.equals(ResourceRateLimitConfig.NODE)) {
				rc = new ResourceConfig(resource, 0, 0);
			}
			if (defaultRateLimitConfigId != null) {
				if (defaultRateLimitConfigId.equals(ResourceRateLimitConfig.SERVICE_DEFAULT)) {
					rc = new ResourceConfig(resource, 0, 0);
					rateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.SERVICE_DEFAULT_RESOURCE);
					if (rateLimitConfig != null && rateLimitConfig.isEnable()) {
						rc.setMaxCon(rateLimitConfig.concurrents);
						rc.setMaxQPS(rateLimitConfig.qps);
					}
				}
				if (defaultRateLimitConfigId.equals(ResourceRateLimitConfig.APP_DEFAULT)) {
					rateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.APP_DEFAULT_RESOURCE);
					if (rateLimitConfig != null && rateLimitConfig.isEnable()) {
						rc = new ResourceConfig(resource, rateLimitConfig.concurrents, rateLimitConfig.qps);
					}
				}
			}
			if (rc != null) {
				resourceConfigs.add(rc);
			}
		}
	}
}
