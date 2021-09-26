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

package we.plugin.basicAuth;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.plugin.FizzPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.PluginConfig;
import we.util.JacksonUtils;
import we.util.ReactorUtils;
import we.util.WebUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Francis Dong
 *
 */
@Component(BasicAuthPluginFilter.BASIC_AUTH_PLUGIN_FILTER)
public class BasicAuthPluginFilter implements FizzPluginFilter {

	private static final Logger log = LoggerFactory.getLogger(BasicAuthPluginFilter.class);

	public static final String BASIC_AUTH_PLUGIN_FILTER = "basicAuthPlugin";

	public static final String BASIC_AUTH_KEY = "_basicAuth";

	public static final String BASIC_AUTH_VALUE = "Y";

	/**
	 * Plugin global custom config, example:
	 * 
	 * {"users": {"username":"password","c":"d"}
	 */
	private GlobalConfig globalConfig = null;

	private String customConfigCache = null;

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
		try {
			// global config
			String customConfig = (String) config.get(PluginConfig.CUSTOM_CONFIG);
			if (globalConfig == null || customConfigCache == null
					|| (customConfigCache != null && !customConfigCache.equals(customConfig))) {
				if (StringUtils.isNotBlank(customConfig)) {
					globalConfig = JacksonUtils.readValue(customConfig, GlobalConfig.class);
				} else {
					globalConfig = null;
				}
				customConfigCache = customConfig;
			}

			// route level config
			Map<String, String> routeUsers = new HashMap<>();
			String routeLevelConfig = (String) config.get("users");
			if (StringUtils.isNotBlank(routeLevelConfig)) {
				Map<String, String> tmp = (Map<String, String>) JacksonUtils.readValue(routeLevelConfig, Map.class);
				routeUsers.putAll(tmp);
			}

			// check header auth
			HttpHeaders reqHeaders = exchange.getRequest().getHeaders();
			String authorization = reqHeaders.getFirst(HttpHeaders.AUTHORIZATION);
			if (checkAuth(authorization, globalConfig, routeUsers)) {
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
				response.getHeaders().add("WWW-authenticate", "Basic Realm=\"input username and password\"");
				return WebUtils.buildDirectResponse(exchange, HttpStatus.UNAUTHORIZED, null, null);
			}
		} catch (Exception e) {
			log.error("Basic Auth plugin Exception", e);
			return WebUtils.buildDirectResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, null, null);
		}
	}

	public void doAfter() {

	}

	/**
	 * Validate basic authorization
	 * 
	 * @param authorization
	 * @param globalConfig
	 * @return
	 */
	public boolean checkAuth(String authorization, GlobalConfig globalConfig, Map<String, String> routeUsers) {
		if ((authorization != null) && (authorization.length() > 6)) {
			authorization = authorization.substring(6, authorization.length());
			try {
				String decodedAuth = new String(Base64.getDecoder().decode(authorization));
				if (decodedAuth != null && decodedAuth.indexOf(":") != -1) {
					int idx = decodedAuth.indexOf(":");
					String username = decodedAuth.substring(0, idx);
					String password = decodedAuth.substring(idx + 1, decodedAuth.length());
					if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
						if (routeUsers != null && routeUsers.containsKey(username)) {
							return password.equals(routeUsers.get(username));
						} else if (globalConfig != null && globalConfig.getUsers() != null) {
							return password.equals(globalConfig.getUsers().get(username));
						}
					}
				}
			} catch (Exception e) {
				log.warn("invalid basic authorization", e);
			}
		}
		return false;
	}

}
