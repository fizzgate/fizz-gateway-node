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
import we.util.Consts;
import we.util.DateTimeUtils;

/**
 * @author hongqiaowei
 */

@RestController
@RequestMapping("/admin")
public class HealthCheckController {

	@GetMapping("/health")
	public Mono<String> health(ServerWebExchange exchange) {
		long mills = System.currentTimeMillis();
		String now = DateTimeUtils.convert(mills, Consts.DP.DP23);
		return Mono.just(now + " ok");
	}
}
