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

package com.wehotel.controller;

import java.util.Date;

import com.wehotel.plugin.auth.ApiConfigService;
import com.wehotel.plugin.auth.AppService;
import com.wehotel.util.JacksonUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author unknown
 */
@RestController
public class HealthController {

	@Resource
	private AppService appService;

	@Resource
	private ApiConfigService apiConfigService;

	@GetMapping("/time")
	public Mono<String> time(ServerWebExchange exchange) throws Exception{
		Date d = new Date();
		return Mono.just("Time: " + d.toString());
	}

	// add by lancer
	@GetMapping("/sysgc")
	public Mono<String> sysgc(ServerWebExchange exchange) throws Exception {
		System.gc();
		return Mono.just("sysgc done");
	}

	@GetMapping("/apps")
	public Mono<String> apps(ServerWebExchange exchange) throws Exception {
		return Mono.just(JacksonUtils.writeValueAsString(appService.getAppMap()));
	}

	@GetMapping("/apiConfigs")
	public Mono<String> apiConfigs(ServerWebExchange exchange) throws Exception {
		return Mono.just(JacksonUtils.writeValueAsString(apiConfigService.getApp2gatewayGroupMap()));
	}
}
