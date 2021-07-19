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

package we.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.filter.FilterResult;
import we.flume.clients.log4j2appender.LogService;
import we.legacy.RespEntity;
import we.util.WebUtils;

import java.util.Map;

/**
 * @apiNote Custom plugin should implement FizzPluginFilter directly
 * <p/>
 *
 * @author hongqiaowei
 * @deprecated
 */

@Deprecated
public abstract class PluginFilter implements FizzPluginFilter {

    private static final Logger log = LoggerFactory.getLogger(PluginFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        String fixedConfig = (String) config.get(PluginConfig.CUSTOM_CONFIG);
        return filter(exchange, config, fixedConfig);
    }

    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {
        FilterResult pfr = WebUtils.getPrevFilterResult(exchange);
        if (log.isDebugEnabled()) {
            log.debug(this + ": " + pfr.id + " execute " + (pfr.success ? "success" : "fail"), LogService.BIZ_ID, exchange.getRequest().getId());
        }
        if (pfr.success) {
            return doFilter(exchange, config, fixedConfig);
        } else {
            if (WebUtils.getDirectResponse(exchange) == null) { // should not reach here
                ServerHttpRequest clientReq = exchange.getRequest();
                String rid = clientReq.getId();
                String msg = pfr.id + " fail";
                if (pfr.cause == null) {
                    log.error(msg, LogService.BIZ_ID, rid);
                } else {
                    log.error(msg, LogService.BIZ_ID, rid, pfr.cause);
                }
                return WebUtils.buildJsonDirectResponseAndBindContext(exchange, HttpStatus.OK, null, RespEntity.toJson(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg, rid));
            } else {
                return Mono.empty();
            }
        }
    }

    public abstract Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig);
}
