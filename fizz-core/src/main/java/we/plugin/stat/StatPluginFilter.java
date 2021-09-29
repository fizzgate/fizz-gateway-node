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

package we.plugin.stat;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.config.AggregateRedisConfig;
import we.plugin.PluginFilter;
import we.plugin.auth.GatewayGroupService;
import we.util.Consts;
import we.util.ThreadContext;
import we.util.WebUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Iterator;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Component(StatPluginFilter.STAT_PLUGIN_FILTER)
public class StatPluginFilter extends PluginFilter {

    private static final Logger log = LoggerFactory.getLogger(StatPluginFilter.class);

    public  static final String STAT_PLUGIN_FILTER = "statPlugin";

    private static final String ip                 = "\"ip\":";

    private static final String gatewayGroup       = "\"gatewayGroup\":";

    private static final String service            = "\"service\":";

    private static final String appid              = "\"appid\":";

    private static final String apiMethod          = "\"apiMethod\":";

    private static final String apiPath            = "\"apiPath\":";

    private static final String reqTime            = "\"reqTime\":";

    @Resource
    private StatPluginFilterProperties statPluginFilterProperties;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private GatewayGroupService gatewayGroupService;

    /*
    private String currentGatewayGroups;

    @PostConstruct
    public void init() {
        Iterator<String> it = gatewayGroupService.currentGatewayGroupSet.iterator();
        while (it.hasNext()) {
            if (StringUtils.isBlank(currentGatewayGroups)) {
                currentGatewayGroups = it.next();
            } else {
                currentGatewayGroups = currentGatewayGroups + ',' + it.next();
            }
        }
    }
    */

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {

        if (statPluginFilterProperties.isStatOpen()) {
            StringBuilder b = ThreadContext.getStringBuilder();
            b.append(Consts.S.LEFT_BRACE);
            b.append(ip);              toJsonStringValue(b, WebUtils.getOriginIp(exchange));               b.append(Consts.S.COMMA);
            b.append(gatewayGroup);    toJsonStringValue(b, currentGatewayGroups());                       b.append(Consts.S.COMMA);
            b.append(service);         toJsonStringValue(b, WebUtils.getClientService(exchange));          b.append(Consts.S.COMMA);

            String appId = WebUtils.getAppId(exchange);
            if (appId != null) {
            b.append(appid);           toJsonStringValue(b, appId);                                        b.append(Consts.S.COMMA);
            }

            b.append(apiMethod);       toJsonStringValue(b, exchange.getRequest().getMethodValue());       b.append(Consts.S.COMMA);
            b.append(apiPath);         toJsonStringValue(b, WebUtils.getClientReqPath(exchange));          b.append(Consts.S.COMMA);
            b.append(reqTime)                               .append(System.currentTimeMillis());
            b.append(Consts.S.RIGHT_BRACE);

            if (StringUtils.isBlank(statPluginFilterProperties.getFizzAccessStatTopic())) {
                rt.convertAndSend(statPluginFilterProperties.getFizzAccessStatChannel(), b.toString()).subscribe();
            } else {
                log.warn(b.toString(), LogService.HANDLE_STGY, LogService.toKF(statPluginFilterProperties.getFizzAccessStatTopic())); // for internal use
            }
        }

        return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, STAT_PLUGIN_FILTER, null);
    }

    private String currentGatewayGroups() {
        int sz = gatewayGroupService.currentGatewayGroupSet.size();
        if (sz == 1) {
            return gatewayGroupService.currentGatewayGroupSet.iterator().next();
        }
        StringBuilder b = ThreadContext.getStringBuilder();
        byte i = 0;
        for (String g : gatewayGroupService.currentGatewayGroupSet) {
            b.append(g);
            i++;
            if (i < sz) {
                b.append(Consts.S.COMMA);
            }
        }
        return b.toString();
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Consts.S.DOUBLE_QUOTE).append(value).append(Consts.S.DOUBLE_QUOTE);
    }

    private static void toJsonStringValue(StringBuilder b, Character value) {
        b.append(Consts.S.DOUBLE_QUOTE).append(value).append(Consts.S.DOUBLE_QUOTE);
    }
}
