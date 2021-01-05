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
