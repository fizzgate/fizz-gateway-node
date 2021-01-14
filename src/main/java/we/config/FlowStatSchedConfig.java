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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
// import we.filter.FlowControlFilter;
import we.filter.GlobalFlowControlFilter;
import we.flume.clients.log4j2appender.LogService;
import we.stats.FlowStat;
import we.stats.ResourceTimeWindowStat;
import we.stats.TimeWindowStat;
import we.stats.ratelimit.ResourceRateLimitConfig;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.Constants;
import we.util.DateTimeUtils;
import we.util.NetworkUtils;
import we.util.ThreadContext;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author hongqiaowei
 */

@Configuration
// @ConditionalOnProperty(name="flowControl",havingValue = "true")
@DependsOn(GlobalFlowControlFilter.GLOBAL_FLOW_CONTROL_FILTER)
@EnableScheduling
// @ConfigurationProperties(prefix = "flow-stat-sched")
public class FlowStatSchedConfig extends SchedConfig {

    private static final Logger log = LoggerFactory.getLogger(FlowStatSchedConfig.class);

    private static final String _ip              = "\"ip\":";
    private static final String _id              = "\"id\":";
    private static final String _resource        = "\"resource\":";
    private static final String _type            = "\"type\":";
    private static final String _start           = "\"start\":";
    private static final String _reqs            = "\"reqs\":";
    private static final String _completeReqs    = "\"completeReqs\":";
    private static final String _peakConcurrents = "\"peakConcurrents\":";
    private static final String _reqPerSec       = "\"reqPerSec\":";
    private static final String _blockReqs       = "\"blockReqs\":";
    private static final String _totalBlockReqs  = "\"totalBlockReqs\":";
    private static final String _errors          = "\"errors\":";
    private static final String _avgRespTime     = "\"avgRespTime\":";
    private static final String _minRespTime     = "\"minRespTime\":";
    private static final String _maxRespTime     = "\"maxRespTime\":";

    @NacosValue(value = "${flowControl:false}", autoRefreshed = true)
    @Value("${flowControl:false}")
    private boolean flowControl;

    // @Resource(name = FlowControlFilter.FLOW_CONTROL_FILTER)
    private FlowStat flowStat = GlobalFlowControlFilter.flowStat;

    @Resource
    private ResourceRateLimitConfigService resourceRateLimitConfigService;

    @NacosValue(value = "${flow-stat-sched.dest:redis}", autoRefreshed = true)
    @Value("${flow-stat-sched.dest:redis}")
    private String dest;

    @NacosValue(value = "${flow-stat-sched.queue:fizz_resource_access_stat}", autoRefreshed = true)
    @Value("${flow-stat-sched.queue:fizz_resource_access_stat}")
    private String queue;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    private final String ip = NetworkUtils.getServerIp();

    private long startTimeSlot = 0;

    private Map<String, AtomicLong> key2totalBlockMap = new HashMap<>();

    @Scheduled(cron = "${flow-stat-sched.cron}")
    public void sched() {

        if (!flowControl) {
            return;
        }
        // FlowStat flowStat = flowControlFilter.getFlowStat();
        if (startTimeSlot == 0) {
            startTimeSlot = getRecentEndTimeSlot(flowStat);
            return;
        }
        long st = System.currentTimeMillis();
        long recentEndTimeSlot = getRecentEndTimeSlot(flowStat);
        List<ResourceTimeWindowStat> resourceTimeWindowStats = flowStat.getResourceTimeWindowStats(null, startTimeSlot, recentEndTimeSlot, 10);
        if (resourceTimeWindowStats == null || resourceTimeWindowStats.isEmpty()) {
            log.info(toDP19(startTimeSlot) + " - " + toDP19(recentEndTimeSlot) + " no flow stat data");
            return;
        }

        key2totalBlockMap.clear();
        resourceTimeWindowStats.forEach(rtws -> {
            List<TimeWindowStat> wins = rtws.getWindows();
            wins.forEach(w -> {
                AtomicLong totalBlock = key2totalBlockMap.computeIfAbsent(String.format("%s%s",
                        ResourceRateLimitConfig.GLOBAL, w.getStartTime()), key -> new AtomicLong(0));
                totalBlock.addAndGet(w.getBlockRequests());
            });
        });

        resourceTimeWindowStats.forEach(
                rtws -> {
                    String resource = rtws.getResourceId();
                    ResourceRateLimitConfig config = resourceRateLimitConfigService.getResourceRateLimitConfig(resource);
                    int id = (config == null ? 0 : config.id);
                    int type;
                    if (ResourceRateLimitConfig.GLOBAL.equals(resource)) {
                        type = ResourceRateLimitConfig.Type.GLOBAL;
                    } else if (resource.charAt(0) == '/') {
                        type = ResourceRateLimitConfig.Type.API;
                    } else {
                        type = ResourceRateLimitConfig.Type.SERVICE;
                    }
                    List<TimeWindowStat> wins = rtws.getWindows();
                    wins.forEach(
                            w -> {
                                StringBuilder b = ThreadContext.getStringBuilder();
                                Long winStart = w.getStartTime();
                                BigDecimal rps = w.getRps();
                                double qps;
                                if (rps == null) {
                                    qps = 0.00;
                                } else {
                                    qps = rps.doubleValue();
                                }

                                AtomicLong totalBlock = key2totalBlockMap.get(String.format("%s%s", resource, winStart));
                                Long totalBlockReqs = totalBlock != null ? totalBlock.get() : w.getBlockRequests();

                                b.append(Constants.Symbol.LEFT_BRACE);
                                b.append(_ip);                     toJsonStringValue(b, ip);                 b.append(Constants.Symbol.COMMA);
                                b.append(_id);                     b.append(id);                             b.append(Constants.Symbol.COMMA);
                                b.append(_resource);               toJsonStringValue(b, resource);           b.append(Constants.Symbol.COMMA);
                                b.append(_type);                   b.append(type);                           b.append(Constants.Symbol.COMMA);
                                b.append(_start);                  b.append(winStart);                       b.append(Constants.Symbol.COMMA);
                                b.append(_reqs);                   b.append(w.getTotal());                   b.append(Constants.Symbol.COMMA);
                                b.append(_completeReqs);           b.append(w.getCompReqs());                b.append(Constants.Symbol.COMMA);
                                b.append(_peakConcurrents);        b.append(w.getPeakConcurrentReqeusts());  b.append(Constants.Symbol.COMMA);
                                b.append(_reqPerSec);              b.append(qps);                            b.append(Constants.Symbol.COMMA);
                                b.append(_blockReqs);              b.append(w.getBlockRequests());           b.append(Constants.Symbol.COMMA);
                                b.append(_totalBlockReqs);         b.append(totalBlockReqs);                 b.append(Constants.Symbol.COMMA);
                                b.append(_errors);                 b.append(w.getErrors());                  b.append(Constants.Symbol.COMMA);
                                b.append(_avgRespTime);            b.append(w.getAvgRt());                   b.append(Constants.Symbol.COMMA);
                                b.append(_maxRespTime);            b.append(w.getMax());                     b.append(Constants.Symbol.COMMA);
                                b.append(_minRespTime);            b.append(w.getMin());
                                b.append(Constants.Symbol.RIGHT_BRACE);
                                String msg = b.toString();
                                if ("kafka".equals(dest)) { // for internal use
                                    log.warn(msg, LogService.HANDLE_STGY, LogService.toKF(queue));
                                } else {
                                    rt.convertAndSend(queue, msg).subscribe();
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("report " + toDP19(winStart) + " win10: " + msg);
                                }
                            }
                    );
                }
        );

        startTimeSlot = recentEndTimeSlot;
        log.info(toDP23(st) + " fss " + toDP23(System.currentTimeMillis()));
    }

    private long getRecentEndTimeSlot(FlowStat flowStat) {
        long currentTimeSlot = flowStat.currentTimeSlotId();
        int second = DateTimeUtils.from(currentTimeSlot).getSecond();
        long interval;
        if (second > 49) {
            interval = second - 50;
        } else if (second > 39) {
            interval = second - 40;
        } else if (second > 29) {
            interval = second - 30;
        } else if (second > 19) {
            interval = second - 20;
        } else if (second > 9) {
            interval = second - 10;
        } else if (second > 0) {
            interval = second - 0;
        } else {
            interval = 0;
        }
        long recentEndTimeSlot = currentTimeSlot - interval * 1000;
        return recentEndTimeSlot;
    }

    private String toDP19(long startTimeSlot) {
        return DateTimeUtils.toDate(startTimeSlot, Constants.DatetimePattern.DP19);
    }

    private String toDP23(long startTimeSlot) {
        return DateTimeUtils.toDate(startTimeSlot, Constants.DatetimePattern.DP23);
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Constants.Symbol.DOUBLE_QUOTE).append(value).append(Constants.Symbol.DOUBLE_QUOTE);
    }
}
