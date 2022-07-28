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

package we.plugin.stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import we.config.AggregateRedisConfig;
import we.config.SchedConfig;
import we.util.Consts;
import we.util.DateTimeUtils;
import we.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Configuration
public class AccessStatSchedConfig extends SchedConfig {

    private static final Logger LOGGER      = LoggerFactory.getLogger(AccessStatSchedConfig.class);

    private static final Logger STAT_LOGGER = LoggerFactory.getLogger("stat");

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Resource
    private StatPluginFilterProperties statPluginFilterProperties;

    @Resource
    private StatPluginFilter statPluginFilter;

    @Scheduled(cron = "${fizz-access-stat-sched.cron:2/10 * * * * ?}")
    public void sched() {
        long prevTimeWinStart = DateTimeUtils.get10sTimeWinStart(2);
        Map<String, AccessStat> accessStatMap = statPluginFilter.getAccessStat(prevTimeWinStart);

        if (accessStatMap.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no access stat in {} window", DateTimeUtils.convert(prevTimeWinStart, Consts.DP.DP19));
            }
        } else {
            accessStatMap.forEach(
                    (smp, accessStat) -> {
                        String msg = accessStat.toString();
                        String topic = statPluginFilterProperties.getFizzAccessStatTopic();
                        if (StringUtils.isBlank(topic)) {
                            String channel = statPluginFilterProperties.getFizzAccessStatChannel();
                            rt.convertAndSend(channel, msg).subscribe();
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("send access stat {} which belong to {} window to channel {}", msg, DateTimeUtils.convert(accessStat.start, Consts.DP.DP19), channel);
                            }
                        } else {
                            STAT_LOGGER.info(msg);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("send access stat {} which belong to {} window to topic", msg, DateTimeUtils.convert(accessStat.start, Consts.DP.DP19));
                            }
                        }
                    }
            );
        }
    }
}
