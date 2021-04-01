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
import we.plugin.PluginFilter;
import we.util.JacksonUtils;
import we.util.WebUtils;

import java.util.Base64;
import java.util.Map;

/**
 * 
 * @author Francis Dong
 *
 */
@Component(BasicAuthPluginFilter.BASIC_AUTH_PLUGIN_FILTER)
public class BasicAuthPluginFilter extends PluginFilter {

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

	private String fixedConfigCache = null;

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {
		try {
			if (globalConfig == null || fixedConfigCache == null
					|| (fixedConfigCache != null && !fixedConfigCache.equals(fixedConfig))) {
				if (StringUtils.isNotBlank(fixedConfig)) {
					globalConfig = JacksonUtils.readValue(fixedConfig, GlobalConfig.class);
				} else {
					globalConfig = null;
				}
				fixedConfigCache = fixedConfig;
			}

			return exchange.getSession().flatMap(webSession -> {
				// check session
				String authInfo = webSession.getAttribute(BASIC_AUTH_KEY);
				if (authInfo == null) {
					// check header auth
					HttpHeaders reqHeaders = exchange.getRequest().getHeaders();
					String authorization = reqHeaders.getFirst(HttpHeaders.AUTHORIZATION);
					if (checkAuth(authorization, globalConfig)) {
						webSession.getAttributes().put(BASIC_AUTH_KEY, BASIC_AUTH_VALUE);
						authInfo = BASIC_AUTH_VALUE;
					}
				}
				if (authInfo == null) {
					// Auth failed
					ServerHttpResponse response = exchange.getResponse();
					response.setStatusCode(HttpStatus.UNAUTHORIZED);
					response.getHeaders().setCacheControl("no-store");
					response.getHeaders().setExpires(0);
					response.getHeaders().add("WWW-authenticate", "Basic Realm=\"input username and password\"");
					return WebUtils.responseErrorAndBindContext(exchange, BASIC_AUTH_PLUGIN_FILTER,
							HttpStatus.UNAUTHORIZED);
				} else {
					return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, BASIC_AUTH_PLUGIN_FILTER, null);
				}
			});
		} catch (Exception e) {
			log.error("Basic Auth plugin Exception", e);
			return WebUtils.responseErrorAndBindContext(exchange, BASIC_AUTH_PLUGIN_FILTER,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Validate basic authorization
	 * 
	 * @param authorization
	 * @param globalConfig
	 * @return
	 */
	public boolean checkAuth(String authorization, GlobalConfig globalConfig) {
		if ((authorization != null) && (authorization.length() > 6)) {
			authorization = authorization.substring(6, authorization.length());
			try {
				String decodedAuth = new String(Base64.getDecoder().decode(authorization));
				if (decodedAuth != null && decodedAuth.indexOf(":") != -1) {
					int idx = decodedAuth.indexOf(":");
					String username = decodedAuth.substring(0, idx);
					String password = decodedAuth.substring(idx + 1, decodedAuth.length());
					if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password) && globalConfig != null
							&& globalConfig.getUsers() != null) {
						return password.equals(globalConfig.getUsers().get(username));
					}
				}
			} catch (Exception e) {
				log.warn("invalid basic authorization", e);
			}
		}
		return false;
	}

}
