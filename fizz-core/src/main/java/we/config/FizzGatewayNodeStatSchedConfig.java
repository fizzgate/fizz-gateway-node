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

package we.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import we.stats.FlowStat;
import we.stats.ResourceTimeWindowStat;
import we.stats.TimeWindowStat;
import we.util.JacksonUtils;
import we.util.NetworkUtils;
import we.util.ResourceIdUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author hongqiaowei
 */

@Configuration
//@EnableScheduling
public class FizzGatewayNodeStatSchedConfig extends SchedConfig {

    private final static class Stat {
        public String serviceName;
        public String ip;
        public int    port;
        public long   ts;
        public long   startTs;
        public long   concurrents;
        public double rps;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzGatewayNodeStatSchedConfig.class);

    private static final String fizz_gateway_nodes = "fizz_gateway_nodes";

    @Resource
    private ReactiveWebServerApplicationContext applicationContext;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    @Value("${flowControl:false}")
    private boolean flowControl;

    @Autowired(required = false)
    private FlowStat flowStat;

    @Value("${izz-gateway-node-stat-sched.recent:1}")
    private int recent;

    private Stat stat = new Stat();

    private String hashKey;

    @PostConstruct
    public void postConstruct() {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        stat.serviceName = env.getProperty("spring.application.name");
        stat.ip          = NetworkUtils.getServerIp();
        stat.port        = Integer.parseInt(env.getProperty("server.port", "8600"));
        hashKey          = stat.ip + ':' + stat.port;
        stat.startTs     = ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    @Scheduled(cron = "${fizz-gateway-node-stat-sched.cron:*/1 * * * * ?}")
    public void sched() {
        stat.ts = System.currentTimeMillis();
        if (flowControl) {
            stat.rps = 0;
            long currentTimeSlot = flowStat.currentTimeSlotId();
            long startTimeSlot = currentTimeSlot - recent * 1000;
            List<ResourceTimeWindowStat> resourceTimeWindowStats = flowStat.getResourceTimeWindowStats(ResourceIdUtils.NODE_RESOURCE, startTimeSlot, currentTimeSlot, recent);
            if (!CollectionUtils.isEmpty(resourceTimeWindowStats)) {
                TimeWindowStat timeWindowStat = resourceTimeWindowStats.get(0).getWindows().get(0);
                BigDecimal rps = timeWindowStat.getRps();
                if (rps != null) {
                    stat.rps = rps.doubleValue();
                }
            }
            stat.concurrents = flowStat.getConcurrentRequests(ResourceIdUtils.NODE_RESOURCE);
        }
        String s;
        try {
            s = JacksonUtils.writeValueAsString(stat);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("gateway stat: {}", s);
            }
        } catch (RuntimeException e) {
            LOGGER.error("serial fizz gateway node stat error", e);
            return;
        }
        rt.opsForHash().put(fizz_gateway_nodes, hashKey, s)
          .doOnError(
              t -> {
                  LOGGER.error("report fizz gateway node stat error", t);
              }
          )
          .block();
    }
}
