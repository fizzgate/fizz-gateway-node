/*
 *  Copyright (C) 2021 the original author or authors.
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

package we.plugin.pairing;

import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.FizzPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.auth.App;
import we.plugin.auth.AppService;
import we.util.DigestUtils;
import we.util.ReactorUtils;
import we.util.WebUtils;

/**
 * 
 * @author Francis Dong
 *
 */
@Component(ApiPairingPluginFilter.API_PAIRING_PLUGIN_FILTER)
public class ApiPairingPluginFilter implements FizzPluginFilter {

	private static final Logger log = LoggerFactory.getLogger(ApiPairingPluginFilter.class);

	public static final String API_PAIRING_PLUGIN_FILTER = "apiPairingPlugin";

	@Resource
	private SystemConfig systemConfig;

	@Resource
	private AppService appService;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
		String traceId = WebUtils.getTraceId(exchange);
		try {
			LogService.setBizId(traceId);
			String appid = WebUtils.getAppId(exchange);
			App app = appService.getApp(appid);
			String ts = WebUtils.getTimestamp(exchange);
			String sign = WebUtils.getSign(exchange);
			if (validateSign(appid, ts, sign, app)) {
				// Go to next plugin
				Mono next = FizzPluginFilterChain.next(exchange);
				return next.defaultIfEmpty(ReactorUtils.NULL).flatMap(nil -> {
					doAfter();
					return Mono.empty();
				});
			} else {
				// Auth failed
				ServerHttpResponse response = exchange.getResponse();
				response.setStatusCode(HttpStatus.UNAUTHORIZED);
				response.getHeaders().setCacheControl("no-store");
				response.getHeaders().setExpires(0);
				String respJson = WebUtils.jsonRespBody(HttpStatus.UNAUTHORIZED.value(),
						HttpStatus.UNAUTHORIZED.getReasonPhrase(), traceId);
				return WebUtils.response(exchange, HttpStatus.UNAUTHORIZED, null, respJson);
			}
		} catch (Exception e) {
			log.error("{} {} Exception", traceId, API_PAIRING_PLUGIN_FILTER, e, LogService.BIZ_ID, traceId);
			String respJson = WebUtils.jsonRespBody(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), traceId);
			return WebUtils.response(exchange, HttpStatus.INTERNAL_SERVER_ERROR, null, respJson);
		}
	}

	private boolean validateSign(String appid, String ts, String sign, App app) {
		if (StringUtils.isBlank(appid) || StringUtils.isBlank(ts) || StringUtils.isBlank(sign) || app == null
				|| StringUtils.isBlank(app.secretkey)) {
			return false;
		}

		// SHA256(appid+_+ts+_+secretkey)
		String data = appid + "_" + ts + "_" + app.secretkey;
		if (!DigestUtils.sha256Hex(data).equals(sign)) {
			return false;
		}

		// validate timestamp
		long t = 0;
		try {
			t = Long.valueOf(ts).longValue();
		} catch (Exception e) {
			log.warn("invalid timestamp: {}", ts);
			return false;
		}
		long now = System.currentTimeMillis();
		long offset = 5 * 60 * 1000;
		if (t < now - offset || t > now + offset) {
			log.warn("timestamp expired: {}", ts);
			return false;
		}

		return true;
	}

	public void doAfter() {

	}

}
