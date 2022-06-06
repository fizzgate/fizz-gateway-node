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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import we.config.SystemConfig;
import we.monitor.FizzMonitorService;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.AppService;
import we.stats.BlockType;
import we.stats.FlowStat;
import we.stats.IncrRequestResult;
import we.stats.ResourceConfig;
import we.stats.circuitbreaker.CircuitBreakManager;
import we.stats.circuitbreaker.CircuitBreaker;
import we.stats.degrade.DegradeRule;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author hongqiaowei
 */

@Component(FlowControlFilter.FLOW_CONTROL_FILTER)
@Order(-10)
public class FlowControlFilter extends FizzWebFilter {

	public static final String FLOW_CONTROL_FILTER = "flowControlFilter";

	private static final Logger log = LoggerFactory.getLogger(FlowControlFilter.class);

	private static final String admin                           = "admin";

	private static final String actuator                        = "actuator";

	private static final String uuid                            = "uuid";

	private static final String defaultFizzTraceIdValueStrategy = "requestId";

	private static final String _fizz                           = "_fizz-";

	private static final String concurrents                     = "concurrents";

	private static final String qps                             = "qps";


	@Resource
	private FlowControlFilterProperties    flowControlFilterProperties;

	@Resource
	private ResourceRateLimitConfigService resourceRateLimitConfigService;

	@Autowired(required = false)
	private FlowStat flowStat;

	@Resource
	private ApiConfigService    apiConfigService;

	@Resource
	private AppService          appService;

	@Resource
	private SystemConfig        systemConfig;

	/*@Resource
	private DegradeRuleService  degradeRuleService;*/

	@Resource
	private CircuitBreakManager circuitBreakManager;

	@Resource
	private FizzMonitorService  fizzMonitorService;

	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {

		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		int secFS = path.indexOf(Consts.S.FORWARD_SLASH, 1);
		if (secFS == -1) {
			return WebUtils.responseError(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "request path should like /optional-prefix/service-name/real-biz-path");
		}
		String service = path.substring(1, secFS);
		boolean adminReq = false, proxyTestReq = false, fizzApiReq = false;
		if (service.equals(admin) || service.equals(actuator)) {
			adminReq = true;
			exchange.getAttributes().put(WebUtils.ADMIN_REQUEST, Consts.S.EMPTY);
		} else if (service.equals(SystemConfig.DEFAULT_GATEWAY_TEST)) {
			proxyTestReq = true;
		} else {
			service = WebUtils.getClientService(exchange);
			if (service.startsWith(_fizz)) {
				fizzApiReq = true;
				exchange.getAttributes().put(WebUtils.FIZZ_REQUEST, Consts.S.EMPTY);
			}
		}

		setTraceId(exchange);

		if (flowControlFilterProperties.isFlowControl() && !adminReq && !proxyTestReq && !fizzApiReq) {
			String traceId = WebUtils.getTraceId(exchange);
			// LogService.setBizId(traceId);
			org.apache.logging.log4j.ThreadContext.put(Consts.TRACE_ID, traceId);
			if (!apiConfigService.serviceConfigMap.containsKey(service)) {
				String json = WebUtils.jsonRespBody(HttpStatus.FORBIDDEN.value(), "no service " + service + " in flow config", traceId);
				return WebUtils.responseJson(exchange, HttpStatus.FORBIDDEN, null, json);
			}
			String app = WebUtils.getAppId(exchange);
			path = WebUtils.getClientReqPath(exchange);
			String ip = WebUtils.getOriginIp(exchange);

			long currentTimeSlot = flowStat.currentTimeSlotId();
			String host = request.getHeaders().getFirst(HttpHeaders.HOST);
			List<ResourceConfig> resourceConfigs = getFlowControlConfigs(app, ip, host, service, path);
			IncrRequestResult result = flowStat.incrRequest(exchange, resourceConfigs, currentTimeSlot, (rc, rcs) -> {
				return getResourceConfigItselfAndParents(rc, rcs);
			});

			if (result != null && !result.isSuccess()) {
				long currentTimeMillis = System.currentTimeMillis();
				String blockedResourceId = result.getBlockedResourceId();
				if (BlockType.CIRCUIT_BREAK == result.getBlockType()) {
					fizzMonitorService.sendAlarm(service, path, FizzMonitorService.CIRCUIT_BREAK_ALARM, null, currentTimeMillis);
					// log.info("{} trigger {} circuit breaker limit", traceId, blockedResourceId, LogService.BIZ_ID, traceId);
					log.info("{} trigger {} circuit breaker limit", traceId, blockedResourceId);

					String responseContentType = flowControlFilterProperties.getDegradeDefaultResponseContentType();
					String responseContent = flowControlFilterProperties.getDegradeDefaultResponseContent();

					CircuitBreaker cb = circuitBreakManager.getCircuitBreaker(blockedResourceId);
					if (cb == null) {
						cb = circuitBreakManager.getCircuitBreaker(ResourceIdUtils.buildResourceId(null, null, null, service, null));
					}

					if (cb.responseContentType != null) {
						responseContentType = cb.responseContentType;
						responseContent = cb.responseContent;
					} else {
						cb = circuitBreakManager.getCircuitBreaker(ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
						if (cb.responseContentType != null) {
							responseContentType = cb.responseContentType;
							responseContent = cb.responseContent;
						}
					}

					ServerHttpResponse resp = exchange.getResponse();
					resp.setStatusCode(HttpStatus.FORBIDDEN);
					HttpHeaders headers = resp.getHeaders();
					headers.set(HttpHeaders.CONTENT_TYPE, responseContentType);
					headers.set("traceId", traceId);
					return resp.writeWith(Mono.just(resp.bufferFactory().wrap(responseContent.getBytes())));

				} else {
					if (BlockType.CONCURRENT_REQUEST == result.getBlockType()) {
						fizzMonitorService.sendAlarm(service, path, FizzMonitorService.RATE_LIMIT_ALARM, concurrents, currentTimeMillis);
						// log.info("{} exceed {} flow limit, blocked by maximum concurrent requests", traceId, blockedResourceId, LogService.BIZ_ID, traceId);
						log.info("{} exceed {} flow limit, blocked by maximum concurrent requests", traceId, blockedResourceId);
					} else {
						fizzMonitorService.sendAlarm(service, path, FizzMonitorService.RATE_LIMIT_ALARM, qps, currentTimeMillis);
						// log.info("{} exceed {} flow limit, blocked by maximum QPS", traceId, blockedResourceId, LogService.BIZ_ID, traceId);
						log.info("{} exceed {} flow limit, blocked by maximum QPS", traceId, blockedResourceId);
					}

					ResourceRateLimitConfig c = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceIdUtils.NODE_RESOURCE);
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
				}
			} else {
				long start = System.currentTimeMillis();
				setTraceId(exchange);
				String finalService = service;
				String finalPath = path;
				return chain.filter(exchange).doFinally(s -> {
					long rt = System.currentTimeMillis() - start;
					CircuitBreaker cb = exchange.getAttribute(CircuitBreaker.DETECT_REQUEST);
					HttpStatus statusCode = exchange.getResponse().getStatusCode();
					Throwable t = exchange.getAttribute(WebUtils.ORIGINAL_ERROR);
					if (t instanceof TimeoutException) {
						statusCode = HttpStatus.GATEWAY_TIMEOUT;
					}
					if (s == SignalType.ON_ERROR || statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
						flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, false, statusCode);
						if (cb != null) {
							cb.transit(CircuitBreaker.State.RESUME_DETECTIVE, CircuitBreaker.State.OPEN, currentTimeSlot, flowStat);
						}
						if (statusCode == HttpStatus.GATEWAY_TIMEOUT) {
							fizzMonitorService.sendAlarm(finalService, finalPath, FizzMonitorService.TIMEOUT_ALARM, t.getMessage(), start);
						} else if (statusCode.is5xxServerError()) {
							fizzMonitorService.sendAlarm(finalService, finalPath, FizzMonitorService.ERROR_ALARM, String.valueOf(statusCode.value()), start);
						} else if (s == SignalType.ON_ERROR && t != null) {
							fizzMonitorService.sendAlarm(finalService, finalPath, FizzMonitorService.ERROR_ALARM, t.getMessage(), start);
						}
					} else {
						flowStat.addRequestRT(resourceConfigs, currentTimeSlot, rt, true, statusCode);
						if (cb != null) {
							cb.transit(CircuitBreaker.State.RESUME_DETECTIVE, CircuitBreaker.State.CLOSED, currentTimeSlot, flowStat);
						}
					}
				});
			}
		}

		// setTraceId(exchange);
		return chain.filter(exchange);
	}

	private void setTraceId(ServerWebExchange exchange) {
		String traceId = exchange.getRequest().getHeaders().getFirst(systemConfig.fizzTraceIdHeader());
		if (StringUtils.isBlank(traceId)) {
			String fizzTraceIdValueStrategy = systemConfig.fizzTraceIdValueStrategy();
			if (fizzTraceIdValueStrategy.equals(defaultFizzTraceIdValueStrategy)) {
				traceId = exchange.getRequest().getId();
			} else if (fizzTraceIdValueStrategy.equals(uuid)) {
				traceId = UUIDUtil.getUUID();
			} else {
				throw Utils.runtimeExceptionWithoutStack("unsupported " + fizzTraceIdValueStrategy + " trace id value strategy!");
			}
		}
		String fizzTraceIdValuePrefix = systemConfig.fizzTraceIdValuePrefix();
		if (StringUtils.isNotBlank(fizzTraceIdValuePrefix)) {
			traceId = fizzTraceIdValuePrefix + Consts.S.DASH + traceId;
		}
		exchange.getAttributes().put(WebUtils.TRACE_ID, traceId);
	}

	private List<ResourceConfig> getResourceConfigItselfAndParents(ResourceConfig rc, List<ResourceConfig> rcs) {
		boolean check = false;
		String rcId = rc.getResourceId();
		String rcApp = ResourceIdUtils.getApp(rcId);
		String rcIp = ResourceIdUtils.getIp(rcId);
		List<ResourceConfig> result = new ArrayList<>();
		for (int i = rcs.size() - 1; i > -1; i--) {
			ResourceConfig r = rcs.get(i);
			String id = r.getResourceId();
			String node = ResourceIdUtils.getNode(id);
			if (node != null && !node.equals(ResourceIdUtils.NODE)) {
				result.add(r);
				continue;
			}
			String app = ResourceIdUtils.getApp(id);
			String ip = ResourceIdUtils.getIp(id);
			String path = ResourceIdUtils.getPath(id);
			if (check) {
				if (rcIp != null) {
					if (ip != null) {
						result.add(r);
					} else {
						if (app == null && path == null) {
							result.add(r);
						}
					}
				} else if (rcApp != null) {
					if (app != null) {
						result.add(r);
					} else {
						if (path == null) {
							result.add(r);
						}
					}
				} else {
					result.add(r);
				}
			} else if (id.equals(rcId)) {
				result.add(r);
				check = true;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("getResourceConfigItselfAndParents:\n" + JacksonUtils.writeValueAsString(rc) + '\n' + JacksonUtils.writeValueAsString(result));
		}
		return result;
	}

	private List<ResourceConfig> getFlowControlConfigs(String app, String ip, String node, String service, String path) {
		if (log.isDebugEnabled()) {
			log.debug("get flow control configs by app={}, ip={}, node={}, service={}, path={}", app, ip, node, service, path);
		}
		boolean hasHost = (StringUtils.isNotBlank(node) && !node.equals(ResourceIdUtils.NODE));
		int sz = hasHost ? 10 : 9;
		List<ResourceConfig> resourceConfigs = new ArrayList<>(sz);
		StringBuilder b = ThreadContext.getStringBuilder();

        if (hasHost) {
            // String resourceId = ResourceIdUtils.buildResourceId(app, ip, node, service, path);
			String resourceId = ResourceIdUtils.buildResourceId(null, null, node, null, null);
            ResourceConfig resourceConfig = new ResourceConfig(resourceId, 0, 0);
            resourceConfigs.add(resourceConfig);
        }
		checkRateLimitConfigAndAddTo(resourceConfigs, b, null, null, ResourceIdUtils.NODE, null, null, null);
		checkRateLimitConfigAndAddTo(resourceConfigs, b, null, null, null, service, null, ResourceIdUtils.SERVICE_DEFAULT);
		checkRateLimitConfigAndAddTo(resourceConfigs, b, null, null, null, service, path, null);

		if (app != null) {
			checkRateLimitConfigAndAddTo(resourceConfigs, b, app, null, null, null, null, ResourceIdUtils.APP_DEFAULT);
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
		ResourceIdUtils.buildResourceIdTo(b, app, ip, node, service, path);
		String resourceId = b.toString();

		// degrade rule only support service and path
		boolean checkDegradeRule = app == null && ip == null && node == null;

		checkRateLimitConfigAndAddTo(resourceConfigs, resourceId, defaultRateLimitConfigId, checkDegradeRule);
		b.delete(0, b.length());
	}

	private void checkRateLimitConfigAndAddTo(List<ResourceConfig> resourceConfigs, String resource, String defaultRateLimitConfigId, boolean checkDegradeRule) {
		int prevSize = resourceConfigs.size();
		ResourceConfig rc = null;
		ResourceRateLimitConfig rateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(resource);
		if (rateLimitConfig != null && rateLimitConfig.isEnable()) {
			something4appAndIp(resourceConfigs, rateLimitConfig);
			rc = new ResourceConfig(resource, rateLimitConfig.concurrents, rateLimitConfig.qps);
			resourceConfigs.add(rc);
		} else {
			String node = ResourceIdUtils.getNode(resource);
			if (node != null && node.equals(ResourceIdUtils.NODE)) {
				rc = new ResourceConfig(resource, 0, 0);
			}
			if (defaultRateLimitConfigId != null) {
				if (defaultRateLimitConfigId.equals(ResourceIdUtils.SERVICE_DEFAULT)) {
					rc = new ResourceConfig(resource, 0, 0);
					rateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
					if (rateLimitConfig != null && rateLimitConfig.isEnable()) {
						rc.setMaxCon(rateLimitConfig.concurrents);
						rc.setMaxQPS(rateLimitConfig.qps);
					}
				}
				if (defaultRateLimitConfigId.equals(ResourceIdUtils.APP_DEFAULT)) {
					rateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceIdUtils.APP_DEFAULT_RESOURCE);
					if (rateLimitConfig != null && rateLimitConfig.isEnable()) {
						rc = new ResourceConfig(resource, rateLimitConfig.concurrents, rateLimitConfig.qps);
					}
				}
			}
			if (rc != null) {
				resourceConfigs.add(rc);
			}
		}

		if (checkDegradeRule && resourceConfigs.size() == prevSize) {
			CircuitBreaker cb = circuitBreakManager.getCircuitBreaker(resource);
			/*if (cb == null) {
				if (defaultRateLimitConfigId != null && defaultRateLimitConfigId.equals(ResourceIdUtils.SERVICE_DEFAULT)) {
					cb = circuitBreakManager.getCircuitBreaker(ResourceIdUtils.SERVICE_DEFAULT_RESOURCE);
					if (cb == null || !cb.serviceDefaultEnable) {
						cb = null;
					}
				}
			}*/
			if (cb != null) {
				rc = new ResourceConfig(resource, 0, 0);
				resourceConfigs.add(rc);
			}
		}
	}

	private void something4appAndIp(List<ResourceConfig> resourceConfigs, ResourceRateLimitConfig rateLimitConfig) {
		int sz = resourceConfigs.size();
		String prev = null, prevPrev = null;
		if (sz > 1) {
			prev = resourceConfigs.get(sz - 1).getResourceId();
			prevPrev = resourceConfigs.get(sz - 2).getResourceId();

			if (rateLimitConfig.type == ResourceRateLimitConfig.Type.APP) {
				String app = ResourceIdUtils.getApp(prev);
				if (rateLimitConfig.path == null) {
					if (rateLimitConfig.service != null && app == null) {
						something4(resourceConfigs, rateLimitConfig.app, null, null);
					}
				} else {
					if (app == null) {
						something4(resourceConfigs, rateLimitConfig.app, null, null);
						something4(resourceConfigs, rateLimitConfig.app, null, rateLimitConfig.service);
					} else {
						String service = ResourceIdUtils.getService(prev);
						if (service == null) {
							something4(resourceConfigs, rateLimitConfig.app, null, rateLimitConfig.service);
						} /*else {
							app = ResourceIdUtils.getApp(prevPrev);
							if (app == null) {
								something4(resourceConfigs, rateLimitConfig.app, null, null);
							}
						}*/
					}
				}

			} else if (rateLimitConfig.type == ResourceRateLimitConfig.Type.IP) {

				if (rateLimitConfig.service == null && rateLimitConfig.path == null) {
				} else if (rateLimitConfig.path == null) {
					String ip = ResourceIdUtils.getIp(prev);
					if (ip == null) {
						something4(resourceConfigs, null, rateLimitConfig.ip, null);
					}
				} else {
					String ip = ResourceIdUtils.getIp(prev);
					if (ip == null) {
						something4(resourceConfigs, null, rateLimitConfig.ip, null);
						something4(resourceConfigs, null, rateLimitConfig.ip, rateLimitConfig.service);
					} else {
						String service = ResourceIdUtils.getService(prev);
						if (service == null) {
							something4(resourceConfigs, null, rateLimitConfig.ip, rateLimitConfig.service);
						} /*else {
							ip = ResourceIdUtils.getIp(prevPrev);
							if (ip == null) {
								something4(resourceConfigs, null, rateLimitConfig.ip, null);
							}
						}*/
					}
				}
			}
		}
	}

	private void something4(List<ResourceConfig> resourceConfigs, String app, String ip, String service) {
		String r = ResourceIdUtils.buildResourceId(app, ip, null, service, null);
		ResourceConfig rc = new ResourceConfig(r, 0, 0);
		resourceConfigs.add(rc);
	}

	private void fillDegradeRuleData(ResourceConfig resourceConfig, DegradeRule degradeRule) {
		resourceConfig.setStrategy(degradeRule.getStrategy());
		resourceConfig.setRatioThreshold(degradeRule.getRatioThreshold());
		resourceConfig.setExceptionCount(degradeRule.getExceptionCount());
		resourceConfig.setMinRequestCount(degradeRule.getMinRequestCount());
		resourceConfig.setTimeWindow(degradeRule.getTimeWindow());
		resourceConfig.setStatInterval(degradeRule.getStatInterval());
		resourceConfig.setRecoveryStrategy(degradeRule.getRecoveryStrategy());
		resourceConfig.setRecoveryTimeWindow(degradeRule.getRecoveryTimeWindow());
	}
}
