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

package we.controller;

import org.openjdk.jol.info.GraphLayout;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.global_resource.GlobalResourceService;
import we.plugin.auth.ApiConfig2appsService;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.AppService;
import we.plugin.auth.GatewayGroupService;
import we.stats.FlowStat;
import we.stats.ResourceStat;
import we.stats.circuitbreaker.CircuitBreakManager;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.Consts;
import we.util.JacksonUtils;
import we.util.ResourceIdUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author hongqiaowei
 */

@RestController
@RequestMapping("/admin/cache")
public class CacheCheckController {

	@Resource
	private GatewayGroupService            gatewayGroupService;

	@Resource
	private AppService                     appService;

	@Resource
	private ApiConfigService               apiConfigService;

	@Resource
	private ResourceRateLimitConfigService resourceRateLimitConfigService;

	@Resource
	private ApiConfig2appsService          apiConfig2AppsService;

	@Resource
	private GlobalResourceService          globalResourceService;

	@Resource
	private CircuitBreakManager            circuitBreakManager;

	@Resource
	private FlowStat                       flowStat;

	@GetMapping("/gatewayGroups")
	public Mono<String> gatewayGroups(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(gatewayGroupService.gatewayGroupMap));
	}

	@GetMapping("/currentGatewayGroups")
	public Mono<String> currentGatewayGroups(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(gatewayGroupService.currentGatewayGroupSet));
	}

	@GetMapping("/apps")
	public Mono<String> apps(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(appService.getAppMap()));
	}

	@GetMapping("/serviceConfigs")
	public Mono<String> serviceConfigs(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(apiConfigService.serviceConfigMap));
	}

	@GetMapping("/resourceRateLimitConfigs")
	public Mono<String> resourceRateLimitConfigs(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(resourceRateLimitConfigService.getResourceRateLimitConfigMap()));
	}

	@GetMapping("/apiConfig2appsConfigs")
	public Mono<String> apiConfig2appsConfigs(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(apiConfig2AppsService.getApiConfig2appsMap()));
	}

	@GetMapping("/globalResources")
	public Mono<String> globalResources(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(globalResourceService.getResourceMap()));
	}

	@GetMapping("/circuitBreakers")
	public Mono<String> circuitBreakers(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(circuitBreakManager.getResource2circuitBreakerMap()));
	}

	@GetMapping("/resourceStats")
	public Mono<String> resourceStats(ServerWebExchange exchange) {
		Map<String, Object> map = new HashMap<>();
		int nodeCnt = 0, serviceDefaultCnt = 0, serviceCnt = 0, servicePathCnt = 0, appDefaultCnt = 0, appCnt = 0, ipCnt = 0, hostCnt = 0;
		ConcurrentMap<String, ResourceStat> resourceStats = flowStat.resourceStats;
		Set<Map.Entry<String, ResourceStat>> entrySet = resourceStats.entrySet();
		for (Map.Entry<String, ResourceStat> entry : entrySet) {
			String resource = entry.getKey();
			ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(resource);
			if (config == null) {
				String app = ResourceIdUtils.getApp(resource);
				String ip = ResourceIdUtils.getIp(resource);
				String node = ResourceIdUtils.getNode(resource);
				String service = ResourceIdUtils.getService(resource);
				if (node == null) {
					if (app != null) {
						ResourceRateLimitConfig appConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceIdUtils.APP_DEFAULT_RESOURCE);
						if (appConfig != null && appConfig.isEnable()) {
							appDefaultCnt++;
						} else {
							appCnt++;
						}
						continue;
					}
					if (ip != null) {
						ipCnt++;
						continue;
					}
					if (service != null) {
						serviceDefaultCnt++;
					}
				} else {
					if (node.equals(ResourceIdUtils.NODE)) {
						nodeCnt++;
					} else {
						hostCnt++;
					}
				}
			} else {
				byte t = config.type;
				if (t == ResourceRateLimitConfig.Type.NODE) {
					nodeCnt++;
				} else if (t == ResourceRateLimitConfig.Type.SERVICE_DEFAULT) {
					serviceDefaultCnt++;
				} else if (t == ResourceRateLimitConfig.Type.SERVICE) {
					serviceCnt++;
				} else if (t == ResourceRateLimitConfig.Type.APP_DEFAULT) {
					appDefaultCnt++;
				} else if (t == ResourceRateLimitConfig.Type.APP) {
					appCnt++;
				} else if (t == ResourceRateLimitConfig.Type.IP) {
					ipCnt++;
				} else if (t == ResourceRateLimitConfig.Type.API) {
					servicePathCnt++;
			    } else {
					hostCnt++;
				}
			}
		}

		map.put("node", nodeCnt);
		map.put("serviceDefault", serviceDefaultCnt);
		map.put("service", serviceCnt);
		map.put("appDefault", appDefaultCnt);
		map.put("app", appCnt);
		map.put("ip", ipCnt);
		map.put("host", hostCnt);
		map.put("servicePathCnt", servicePathCnt);
		int totalResources = appCnt + appDefaultCnt + ipCnt + nodeCnt + hostCnt + serviceCnt + serviceDefaultCnt + servicePathCnt;
		map.put("totalResources", totalResources);

		long size = GraphLayout.parseInstance(resourceStats).totalSize();
		BigDecimal bigDecimalSize = new BigDecimal(size);
		String resourceStatsSize;
		if (size >= Consts.UN.GB) {
			float r = bigDecimalSize.divide(new BigDecimal(Consts.UN.GB), 2, RoundingMode.HALF_UP).floatValue();
			resourceStatsSize = r + " GB";
		} else if (size >= Consts.UN.MB) {
			float r = bigDecimalSize.divide(new BigDecimal(Consts.UN.MB), 2, RoundingMode.HALF_UP).floatValue();
			resourceStatsSize = r + " MB";
		} else if (size >= Consts.UN.KB) {
			float r = bigDecimalSize.divide(new BigDecimal(Consts.UN.KB), 2, RoundingMode.HALF_UP).floatValue();
			resourceStatsSize = r + " KB";
		} else {
			resourceStatsSize = size + " B";
		}
		map.put("resourceStatsSize", resourceStatsSize);

		return Mono.just(JacksonUtils.writeValueAsString(map));
	}
}
