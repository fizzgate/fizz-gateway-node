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
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
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
@EnableScheduling
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

    private static final String _app             = "\"app\":";
    private static final String _sourceIp        = "\"sourceIp\":";
    private static final String _service         = "\"service\":";
    private static final String _path            = "\"path\":";

    private static final String parentResourceList = "$prl";

    @Resource
    private FlowStatSchedConfigProperties flowStatSchedConfigProperties;

    // @Resource
    @Autowired(required = false)
    private FlowStat flowStat;

    @Resource
    private ResourceRateLimitConfigService resourceRateLimitConfigService;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    private final String ip = NetworkUtils.getServerIp();

    private long startTimeSlot = 0;

    private Map<String, AtomicLong> resourceTimeWindow2totalBlockRequestsMap = new HashMap<>(128);

    @Scheduled(cron = "${flow-stat-sched.cron}")
    public void sched() {

        if (!flowStatSchedConfigProperties.isFlowControl()) {
            return;
        }
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

        resourceTimeWindow2totalBlockRequestsMap.clear();
        resourceTimeWindowStats.forEach(rtws -> {
            String resource = rtws.getResourceId();
            List<TimeWindowStat> wins = rtws.getWindows();
            wins.forEach(w -> {
                long t = w.getStartTime();
                long blockRequests = w.getBlockRequests();
                resourceTimeWindow2totalBlockRequestsMap.put(resource + t, new AtomicLong(blockRequests));
            });
        });

        resourceTimeWindowStats.forEach(rtws -> {
            String resource = rtws.getResourceId();
            List<TimeWindowStat> wins = rtws.getWindows();
            wins.forEach(w -> {
                accumulateParents(resource, w.getStartTime(), w.getBlockRequests());
            });
        });

        resourceTimeWindowStats.forEach(
                rtws -> {
                    String resource = rtws.getResourceId();
                    String app = null, pi = null, node = ResourceRateLimitConfig.NODE, service = null, path = null;
                    int type = ResourceRateLimitConfig.Type.NODE, id = 0;
                    ResourceRateLimitConfig c = resourceRateLimitConfigService.getResourceRateLimitConfig(resource);
                    if (c == null) {
                        service = ResourceRateLimitConfig.getService(resource);
                        if (service != null) {
                            type = ResourceRateLimitConfig.Type.SERVICE_DEFAULT;
                        } else {
                            app = ResourceRateLimitConfig.getApp(resource);
                            if (app != null) {
                                type = ResourceRateLimitConfig.Type.APP_DEFAULT;
                            }
                        }
                    } else {
                        app = c.app;
                        pi = c.ip;
                        service = c.service;
                        path = c.path;
                        type = c.type;
                        id = c.id;
                    }

                    List<TimeWindowStat> wins = rtws.getWindows();
                    for (int i = 0; i < wins.size(); i++) {
                        TimeWindowStat w = wins.get(i);
                        StringBuilder b = ThreadContext.getStringBuilder();
                        long timeWin = w.getStartTime();
                        BigDecimal rps = w.getRps();
                        double qps;
                        if (rps == null) {
                            qps = 0.00;
                        } else {
                            qps = rps.doubleValue();
                        }

                        AtomicLong totalBlockRequests = resourceTimeWindow2totalBlockRequestsMap.get(resource + timeWin);
                        long tbrs = (totalBlockRequests == null ? w.getBlockRequests() : totalBlockRequests.longValue());

                        b.append(Constants.Symbol.LEFT_BRACE);
                        b.append(_ip);                     toJsonStringValue(b, ip);                 b.append(Constants.Symbol.COMMA);
                        b.append(_id);                     b.append(id);                             b.append(Constants.Symbol.COMMA);

                        String r = null;
                        if (type == ResourceRateLimitConfig.Type.NODE) {
                            r = ResourceRateLimitConfig.NODE;
                        } else if (type == ResourceRateLimitConfig.Type.SERVICE_DEFAULT || type == ResourceRateLimitConfig.Type.SERVICE) {
                            r = service;
                        }
                        if (r != null) {
                        b.append(_resource);               toJsonStringValue(b, r);                  b.append(Constants.Symbol.COMMA);
                        }

                        b.append(_type);                   b.append(type);                           b.append(Constants.Symbol.COMMA);

                        if (app != null) {
                        b.append(_app);                    toJsonStringValue(b, app);                b.append(Constants.Symbol.COMMA);
                        }

                        if (pi != null) {
                        b.append(_sourceIp);               toJsonStringValue(b, pi);                 b.append(Constants.Symbol.COMMA);
                        }

                        if (service != null) {
                        b.append(_service);                toJsonStringValue(b, service);            b.append(Constants.Symbol.COMMA);
                        }

                        if (path != null) {
                        b.append(_path);                   toJsonStringValue(b, path);               b.append(Constants.Symbol.COMMA);
                        }

                        b.append(_start);                  b.append(timeWin);                        b.append(Constants.Symbol.COMMA);
                        b.append(_reqs);                   b.append(w.getTotal());                   b.append(Constants.Symbol.COMMA);
                        b.append(_completeReqs);           b.append(w.getCompReqs());                b.append(Constants.Symbol.COMMA);
                        b.append(_peakConcurrents);        b.append(w.getPeakConcurrentReqeusts());  b.append(Constants.Symbol.COMMA);
                        b.append(_reqPerSec);              b.append(qps);                            b.append(Constants.Symbol.COMMA);
                        b.append(_blockReqs);              b.append(w.getBlockRequests());           b.append(Constants.Symbol.COMMA);
                        b.append(_totalBlockReqs);         b.append(tbrs);                           b.append(Constants.Symbol.COMMA);
                        b.append(_errors);                 b.append(w.getErrors());                  b.append(Constants.Symbol.COMMA);
                        b.append(_avgRespTime);            b.append(w.getAvgRt());                   b.append(Constants.Symbol.COMMA);
                        b.append(_maxRespTime);            b.append(w.getMax());                     b.append(Constants.Symbol.COMMA);
                        b.append(_minRespTime);            b.append(w.getMin());
                        b.append(Constants.Symbol.RIGHT_BRACE);
                        String msg = b.toString();
                        if ("kafka".equals(flowStatSchedConfigProperties.getDest())) { // for internal use
                            log.warn(msg, LogService.HANDLE_STGY, LogService.toKF(flowStatSchedConfigProperties.getQueue()));
                        } else {
                            rt.convertAndSend(flowStatSchedConfigProperties.getQueue(), msg).subscribe();
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("report " + toDP19(timeWin) + " win10: " + msg);
                        }
                    }
                }
        );

        startTimeSlot = recentEndTimeSlot;
        if (log.isInfoEnabled()) {
            log.info(toDP23(st) + " fss " + toDP23(System.currentTimeMillis()));
        }
    }

    private void accumulateParents(String resource, long timeWin, long blockRequests) {
        List<String> prl = ThreadContext.getArrayList(parentResourceList, String.class);
        resourceRateLimitConfigService.getParentsTo(resource, prl);
        for (int i = 0; i < prl.size(); i++) {
            String parentResource = prl.get(i);
            AtomicLong parentTotalBlockRequests = resourceTimeWindow2totalBlockRequestsMap.get(parentResource + timeWin);
            if (parentTotalBlockRequests != null) {
                parentTotalBlockRequests.addAndGet(blockRequests);
            }
        }
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
        return currentTimeSlot - interval * 1000 - 10 * 1000;
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
