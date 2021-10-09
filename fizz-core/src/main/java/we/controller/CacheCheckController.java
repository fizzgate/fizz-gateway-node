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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.dict.DictService;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.ApiConifg2appsService;
import we.plugin.auth.AppService;
import we.plugin.auth.GatewayGroupService;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.JacksonUtils;

import javax.annotation.Resource;

/**
 * @author unknown
 */

@RestController
@RequestMapping("/admin/cache")
public class CacheCheckController {

	@Resource
	private GatewayGroupService gatewayGroupService;

	@Resource
	private AppService appService;

	@Resource
	private ApiConfigService apiConfigService;

	@Resource
	private ResourceRateLimitConfigService resourceRateLimitConfigService;

	@Resource
	private ApiConifg2appsService apiConifg2appsService;

	@Resource
	private DictService dictService;

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
	public Mono<String> apiConfigs(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(apiConfigService.serviceConfigMap));
	}

	@GetMapping("/resourceRateLimitConfigs")
	public Mono<String> resourceRateLimitConfigs(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(resourceRateLimitConfigService.getResourceRateLimitConfigMap()));
	}

	@GetMapping("/apiConfig2appsConfigs")
	public Mono<String> apiConfig2appsConfigs(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(apiConifg2appsService.getApiConfig2appsMap()));
	}

	@GetMapping("/dicts")
	public Mono<String> dicts(ServerWebExchange exchange) {
		return Mono.just(JacksonUtils.writeValueAsString(dictService.getDictMap()));
	}
}
