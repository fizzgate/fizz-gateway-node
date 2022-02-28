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
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import we.util.JacksonUtils;
import we.util.NetworkUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.management.ManagementFactory;

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
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FizzGatewayNodeStatSchedConfig.class);

    private static final String fizz_gateway_nodes = "fizz_gateway_nodes";

    @Resource
    private ReactiveWebServerApplicationContext applicationContext;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    private Stat stat = new Stat();

    @PostConstruct
    public void postConstruct() {
        stat.serviceName = applicationContext.getApplicationName();
        stat.ip          = NetworkUtils.getServerIp();
        stat.port        = Integer.parseInt(applicationContext.getEnvironment().getProperty("server.port", "8600"));
        stat.startTs     = ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    @Scheduled(cron = "${fizz-gateway-node-stat-sched.cron:*/3 * * * * ?}")
    public void sched() {
        stat.ts = System.currentTimeMillis();
        String s;
        try {
            s = JacksonUtils.writeValueAsString(stat);
        } catch (RuntimeException e) {
            LOGGER.error("serial fizz gateway node stat error", e);
            return;
        }
        rt.opsForHash().put(fizz_gateway_nodes, stat.ip, s)
          .doOnError(
              t -> {
                  LOGGER.error("report fizz gateway node stat error", t);
              }
          )
          .block();
    }
}
