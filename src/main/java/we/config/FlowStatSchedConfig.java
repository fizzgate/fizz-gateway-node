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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import we.filter.FlowControlFilter;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Configuration
// @ConditionalOnProperty(name="flowControl",havingValue = "true")
@DependsOn(FlowControlFilter.FLOW_CONTROL_FILTER)
@EnableScheduling
// @ConfigurationProperties(prefix = "flow-stat-sched")
public class FlowStatSchedConfig extends SchedConfig {

    private static final Logger log = LoggerFactory.getLogger(FlowStatSchedConfig.class);

    private static final String collectedWins = "$collectedWins";

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
    private static final String _errors          = "\"errors\":";
    private static final String _avgRespTime     = "\"avgRespTime\":";
    private static final String _minRespTime     = "\"minRespTime\":";
    private static final String _maxRespTime     = "\"maxRespTime\":";

    @NacosValue(value = "${flowControl:false}", autoRefreshed = true)
    @Value("${flowControl:false}")
    private boolean flowControl;

    @Resource(name = FlowControlFilter.FLOW_CONTROL_FILTER)
    private FlowControlFilter flowControlFilter;

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

    private long startTimeSlot = 0;

    private Map<String, List<TimeWindowStat>> resourceTimeWindowStatsMap = new HashMap<>();

    private List<TimeWindowStat> tmpTimeWindowStats = new ArrayList<>();

    @Scheduled(cron = "${flow-stat-sched.cron}")
    public void sched() {

        if (!flowControl) {
            return;
        }
        FlowStat flowStat = flowControlFilter.getFlowStat();
        long currentTimeSlot = flowStat.currentTimeSlotId();
        if (startTimeSlot == 0) {
            startTimeSlot = currentTimeSlot;
            return;
        }
        List<ResourceTimeWindowStat> resourceTimeWindowStats = flowStat.getResourceTimeWindowStats(null, startTimeSlot, currentTimeSlot);
        if (resourceTimeWindowStats == null || resourceTimeWindowStats.isEmpty()) {
            log.info(DateTimeUtils.toDate(startTimeSlot, Constants.DatetimePattern.DP19) + " -> " + DateTimeUtils.toDate(currentTimeSlot, Constants.DatetimePattern.DP19) + " no flow stat data");
            startTimeSlot = currentTimeSlot;
            return;
        }
        resourceTimeWindowStats.forEach(
                rtws -> {
                    String resource = rtws.getResourceId();
                    List<TimeWindowStat> timeWindowStats = rtws.getWindows();
                    List<TimeWindowStat> toBeCollectedWins = resourceTimeWindowStatsMap.get(resource);
                    if (toBeCollectedWins == null) {
                        resourceTimeWindowStatsMap.put(resource, timeWindowStats);
                    } else {
                        toBeCollectedWins.addAll(timeWindowStats);
                    }
                }
        );

        resourceTimeWindowStatsMap.forEach(
                (resource, toBeCollectedWins) -> {
                    try {
                            int current = 0;
                            for (; current < toBeCollectedWins.size(); ) {
                                TimeWindowStat win = toBeCollectedWins.get(current);
                                Long timeSlot = win.getStartTime();
                                if (DateTimeUtils.from(timeSlot).getSecond() % 10 == 9) {
                                    int from = current - 9;
                                    if (from > 0) {
                                        ArrayList<TimeWindowStat> cws = ThreadContext.getArrayList(collectedWins, TimeWindowStat.class, true);
                                        while (from <= current) {
                                            cws.add(toBeCollectedWins.get(from));
                                            from++;
                                        }
                                        calcAndRpt(resource, cws);
                                    }
                                    current += 10;
                                } else {
                                    current++;
                                }
                            }
                            if (current > 9) {
                                tmpTimeWindowStats.clear();
                                for (int f = current - 9; f < toBeCollectedWins.size(); f++) {
                                    tmpTimeWindowStats.add(toBeCollectedWins.get(f));
                                }
                                toBeCollectedWins.clear();
                                toBeCollectedWins.addAll(tmpTimeWindowStats);
                            }

                    } catch (Throwable t) {
                            toBeCollectedWins.clear();
                            log.error("report " + resource + " flow stat", t);
                    }
                }
        );

        startTimeSlot = currentTimeSlot;
    }

    private void calcAndRpt(String resource, List<TimeWindowStat> cws) {
        String ip = NetworkUtils.getServerIp();
        int id = resourceRateLimitConfigService.getResourceRateLimitConfig(resource).id;
        int type;
        if (ResourceRateLimitConfig.GLOBAL.equals(resource)) {
            type = ResourceRateLimitConfig.Type.GLOBAL;
        } else if (resource.charAt(0) == '/') {
            type = ResourceRateLimitConfig.Type.API;
        } else {
            type = ResourceRateLimitConfig.Type.SERVICE;
        }
        long start = cws.get(0).getStartTime();
        long reqs = 0, completeReqs = 0, peakConcurrents = 0, blockReqs = 0, errors = 0, avgRespTime = 0, minRespTime = Long.MAX_VALUE, maxRespTime = 0;
        BigDecimal reqPerSec = BigDecimal.ZERO;

        for (int i = 0; i < cws.size(); i++) {
            TimeWindowStat w = cws.get(i);
            reqs = reqs + w.getTotal();
            completeReqs = completeReqs + w.getCompReqs();
            Long pcrs = w.getPeakConcurrentReqeusts();
            if (pcrs > peakConcurrents) {
                peakConcurrents = pcrs;
            }
            blockReqs = blockReqs + w.getBlockRequests();
            errors = errors + w.getErrors();
            Long max = w.getMax();
            if (max > maxRespTime) {
                maxRespTime = max;
            }
            Long min = w.getMin();
            if (min < minRespTime) {
                minRespTime = min;
            }
            avgRespTime = avgRespTime + w.getAvgRt();
        }

        if (reqs > 0) {
            BigDecimal sec = new BigDecimal(cws.size() * 1000).divide(new BigDecimal(1000), 5, BigDecimal.ROUND_HALF_UP);
            reqPerSec = new BigDecimal(reqs).divide(sec, 5, BigDecimal.ROUND_HALF_UP);
            if (reqPerSec.compareTo(new BigDecimal(10)) >= 0) {
                reqPerSec = reqPerSec.setScale(0, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
            } else {
                reqPerSec = reqPerSec.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
            }
        }

        if (completeReqs > 0) {
            avgRespTime = avgRespTime / cws.size();
        }

        StringBuilder b = ThreadContext.getStringBuilder();
        b.append(Constants.Symbol.LEFT_BRACE);
        b.append(_ip);                     toJsonStringValue(b, ip);                 b.append(Constants.Symbol.COMMA);
        b.append(_id);                     b.append(id);                             b.append(Constants.Symbol.COMMA);
        b.append(_resource);               toJsonStringValue(b, resource);           b.append(Constants.Symbol.COMMA);
        b.append(_type);                   b.append(type);                           b.append(Constants.Symbol.COMMA);
        b.append(_start);                  b.append(start);                          b.append(Constants.Symbol.COMMA);
        b.append(_reqs);                   b.append(reqs);                           b.append(Constants.Symbol.COMMA);
        b.append(_completeReqs);           b.append(completeReqs);                   b.append(Constants.Symbol.COMMA);
        b.append(_peakConcurrents);        b.append(peakConcurrents);                b.append(Constants.Symbol.COMMA);
        b.append(_reqPerSec);              b.append(reqPerSec.doubleValue());        b.append(Constants.Symbol.COMMA);
        b.append(_blockReqs);              b.append(blockReqs);                      b.append(Constants.Symbol.COMMA);
        b.append(_errors);                 b.append(errors);                         b.append(Constants.Symbol.COMMA);
        b.append(_avgRespTime);            b.append(avgRespTime);                    b.append(Constants.Symbol.COMMA);
        b.append(_maxRespTime);            b.append(maxRespTime);                    b.append(Constants.Symbol.COMMA);
        b.append(_minRespTime);            b.append(minRespTime);
        b.append(Constants.Symbol.RIGHT_BRACE);

        if ("kafka".equals(dest)) {
            log.info(b.toString(), LogService.HANDLE_STGY, LogService.toKF(queue));
        } else {
            rt.convertAndSend(queue, b.toString()).subscribe();
        }
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Constants.Symbol.DOUBLE_QUOTE).append(value).append(Constants.Symbol.DOUBLE_QUOTE);
    }
}
