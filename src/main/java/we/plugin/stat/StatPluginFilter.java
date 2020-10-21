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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.flume.clients.log4j2appender.LogService;
import we.listener.AggregateRedisConfig;
import we.plugin.PluginFilter;
import we.plugin.auth.GatewayGroupService;
import we.util.Constants;
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

    @Value("${stat.open:false}")
    private boolean statOpen = false;

    @Value("${stat.channel:fizz_access_stat}")
    private String fizzAccessStatChannel;

    @Value("${stat.topic:}")
    private String fizzAccessStatTopic;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private GatewayGroupService gatewayGroupService;

    private String currentGatewayGroups;

    @PostConstruct
    public void init() {
        Iterator<String> it = gatewayGroupService.currentGatewayGroupSet.iterator();
        currentGatewayGroups = it.next();
        while (it.hasNext()) {
            currentGatewayGroups = currentGatewayGroups + ',' + it.next();
        }
    }

    @Override
    public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {

        if (statOpen) {
            StringBuilder b = ThreadContext.getStringBuilder();
            b.append(Constants.Symbol.LEFT_BRACE);
            b.append(ip);              toJsonStringValue(b, WebUtils.getOriginIp(exchange));               b.append(Constants.Symbol.COMMA);
            b.append(gatewayGroup);    toJsonStringValue(b, currentGatewayGroups);                         b.append(Constants.Symbol.COMMA);
            b.append(service);         toJsonStringValue(b, WebUtils.getServiceId(exchange));              b.append(Constants.Symbol.COMMA);
            b.append(appid);           toJsonStringValue(b, WebUtils.getAppId(exchange));                  b.append(Constants.Symbol.COMMA);
            b.append(apiMethod);       toJsonStringValue(b, exchange.getRequest().getMethodValue());       b.append(Constants.Symbol.COMMA);
            b.append(apiPath);         toJsonStringValue(b, WebUtils.getReqPath(exchange));                b.append(Constants.Symbol.COMMA);
            b.append(reqTime)                               .append(System.currentTimeMillis());
            b.append(Constants.Symbol.RIGHT_BRACE);

            if (StringUtils.isBlank(fizzAccessStatTopic)) {
                rt.convertAndSend(fizzAccessStatChannel, b.toString());
            } else {
                log.info(b.toString(), LogService.HANDLE_STGY, LogService.toKF(fizzAccessStatTopic));
            }
        }

        return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, STAT_PLUGIN_FILTER, null);
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Constants.Symbol.DOUBLE_QUOTE).append(value).append(Constants.Symbol.DOUBLE_QUOTE);
    }

    private static void toJsonStringValue(StringBuilder b, Character value) {
        b.append(Constants.Symbol.DOUBLE_QUOTE).append(value).append(Constants.Symbol.DOUBLE_QUOTE);
    }
}
