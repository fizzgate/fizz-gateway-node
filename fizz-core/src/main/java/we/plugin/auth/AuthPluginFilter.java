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

package we.plugin.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.plugin.PluginFilter;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * @author hongqiaowei
 * @apiNote unstable.
 */

@Component(AuthPluginFilter.AUTH_PLUGIN_FILTER)
public class AuthPluginFilter extends PluginFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthPluginFilter.class);

    public static final String AUTH_PLUGIN_FILTER = "authPlugin";

    public static final String RESULT = "result";

    @Resource
    private ApiConfigService apiConfigService;

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String pluginConfig) {
        return apiConfigService.auth(exchange).flatMap(
                r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("req auth: {}", r, LogService.BIZ_ID, WebUtils.getTraceId(exchange));
                    }
                    Map<String, Object> data = Collections.singletonMap(RESULT, r);
                    return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, AUTH_PLUGIN_FILTER, data);
                }
        );
    }
}
