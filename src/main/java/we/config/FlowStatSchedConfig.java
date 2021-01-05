
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

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import we.filter.FlowControlFilter;
import we.stats.FlowStat;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

// @DependsOn(FlowControlFilter.FLOW_CONTROL_FILTER)
// @Configuration
// @EnableScheduling
// @ConfigurationProperties(prefix = "flow-stat-sched")
public class FlowStatSchedConfig extends SchedConfig {

    protected static final Logger log = LoggerFactory.getLogger(FlowStatSchedConfig.class);

    @Resource(name = FlowControlFilter.FLOW_CONTROL_FILTER)
    private FlowControlFilter flowControlFilter;

    @NacosValue(value = "${flow-stat-sched.dest:redis}", autoRefreshed = true)
    @Value("${flow-stat-sched.dest:redis}")
    private String dest;

    @NacosValue(value = "${flow-stat-sched.queue:fizz_resource_access_stat}", autoRefreshed = true)
    @Value("${flow-stat-sched.queue:fizz_resource_access_stat}")
    private String queue;

    @Scheduled(cron = "${flow-stat-sched.cron}")
    public void sched() {
        // System.err.println("now: " + LocalDateTime.now());

        FlowStat flowStat = flowControlFilter.getFlowStat();
        // TODO: rpt data
    }
}
