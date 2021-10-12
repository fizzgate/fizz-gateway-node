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
import we.util.Consts;
import we.util.DateTimeUtils;
import we.util.NetworkUtils;
import we.util.ThreadContext;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

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
    private static final String _peakRps         = "\"peakRps\":";

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

    // private Map<String, AtomicLong> resourceTimeWindow2totalBlockRequestsMap = new HashMap<>(128);

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

        resourceTimeWindowStats.forEach(
                rtws -> {
                    String resource = rtws.getResourceId();
                    String app = null, pi = null, node = ResourceRateLimitConfig.NODE, service = null, path = null;
                    int type = ResourceRateLimitConfig.Type.NODE, id = 0;
                    ResourceRateLimitConfig c = resourceRateLimitConfigService.getResourceRateLimitConfig(resource);

                    if (c == null) { // _global, service, app, app+service, ip, ip+service
                        node = ResourceRateLimitConfig.getNode(resource);
                        if (node != null && node.equals(ResourceRateLimitConfig.NODE)) {
                        } else {
                            service = ResourceRateLimitConfig.getService(resource);
                            app = ResourceRateLimitConfig.getApp(resource);
                            pi = ResourceRateLimitConfig.getIp(resource);
                            if (service == null) {
                                if (app == null) {
                                    type = ResourceRateLimitConfig.Type.IP;
                                } else {
                                    ResourceRateLimitConfig appConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.APP_DEFAULT_RESOURCE);
                                    if (appConfig != null && appConfig.isEnable()) {
                                        type = ResourceRateLimitConfig.Type.APP_DEFAULT;
                                    } else {
                                        type = ResourceRateLimitConfig.Type.APP;
                                    }
                                }
                            } else {
                                if (app == null && pi == null) {
                                    type = ResourceRateLimitConfig.Type.SERVICE_DEFAULT;
                                } else {
                                    if (app == null) {
                                        type = ResourceRateLimitConfig.Type.IP;
                                    } else {
                                        type = ResourceRateLimitConfig.Type.APP;
                                    }
                                }
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
                        BigDecimal peakRps = w.getPeakRps();
                        double qps, pRps;
                        if (rps == null) {
                            qps = 0.00;
                        } else {
                            qps = rps.doubleValue();
                        }
                        if (peakRps == null) {
                            pRps = 0.00;
                        } else {
                            pRps = peakRps.doubleValue();
                        }

                        long tbrs = w.getTotalBlockRequests();

                        b.append(Consts.S.LEFT_BRACE);
                        b.append(_ip);                     toJsonStringValue(b, ip);                 b.append(Consts.S.COMMA);
                        b.append(_id);                     b.append(id);                             b.append(Consts.S.COMMA);

                        String r = null;
                        if (type == ResourceRateLimitConfig.Type.NODE) {
                            r = ResourceRateLimitConfig.NODE;
                        } else if (type == ResourceRateLimitConfig.Type.SERVICE_DEFAULT || type == ResourceRateLimitConfig.Type.SERVICE) {
                            r = service;
                        }
                        if (r != null) {
                        b.append(_resource);               toJsonStringValue(b, r);                  b.append(Consts.S.COMMA);
                        }

                        b.append(_type);                   b.append(type);                           b.append(Consts.S.COMMA);

                        if (app != null) {
                        b.append(_app);                    toJsonStringValue(b, app);                b.append(Consts.S.COMMA);
                        }

                        if (pi != null) {
                        b.append(_sourceIp);               toJsonStringValue(b, pi);                 b.append(Consts.S.COMMA);
                        }

                        if (service != null) {
                        b.append(_service);                toJsonStringValue(b, service);            b.append(Consts.S.COMMA);
                        }

                        if (path != null) {
                        b.append(_path);                   toJsonStringValue(b, path);               b.append(Consts.S.COMMA);
                        }

                        b.append(_start);                  b.append(timeWin);                        b.append(Consts.S.COMMA);
                        b.append(_reqs);                   b.append(w.getTotal());                   b.append(Consts.S.COMMA);
                        b.append(_completeReqs);           b.append(w.getCompReqs());                b.append(Consts.S.COMMA);
                        b.append(_peakConcurrents);        b.append(w.getPeakConcurrentReqeusts());  b.append(Consts.S.COMMA);
                        b.append(_reqPerSec);              b.append(qps);                            b.append(Consts.S.COMMA);
                        b.append(_peakRps);                b.append(pRps);                           b.append(Consts.S.COMMA);
                        b.append(_blockReqs);              b.append(w.getBlockRequests());           b.append(Consts.S.COMMA);
                        b.append(_totalBlockReqs);         b.append(tbrs);                           b.append(Consts.S.COMMA);
                        b.append(_errors);                 b.append(w.getErrors());                  b.append(Consts.S.COMMA);
                        b.append(_avgRespTime);            b.append(w.getAvgRt());                   b.append(Consts.S.COMMA);
                        b.append(_maxRespTime);            b.append(w.getMax());                     b.append(Consts.S.COMMA);
                        b.append(_minRespTime);            b.append(w.getMin());
                        b.append(Consts.S.RIGHT_BRACE);
                        String msg = b.toString();
                        if ("kafka".equals(flowStatSchedConfigProperties.getDest())) { // for internal use
                            log.warn(msg, LogService.HANDLE_STGY, LogService.toKF(flowStatSchedConfigProperties.getQueue()));
                        } else {
                            rt.convertAndSend(flowStatSchedConfigProperties.getQueue(), msg).subscribe();
                        }
                        if (log.isDebugEnabled()) {
                            String wt = 'w' + toDP19(timeWin);
                            log.debug("report " + wt + ": " + msg, LogService.BIZ_ID, wt);
                        }
                    }
                }
        );

        startTimeSlot = recentEndTimeSlot;
        if (log.isInfoEnabled()) {
            log.info(toDP23(st) + " fss " + toDP23(System.currentTimeMillis()));
        }
    }

    private long getRecentEndTimeSlot(FlowStat flowStat) {
        long currentTimeSlot = flowStat.currentTimeSlotId();
        int second = DateTimeUtils.transform(currentTimeSlot).getSecond();
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
        return DateTimeUtils.convert(startTimeSlot, Consts.DP.DP19);
    }

    private String toDP23(long startTimeSlot) {
        return DateTimeUtils.convert(startTimeSlot, Consts.DP.DP23);
    }

    private static void toJsonStringValue(StringBuilder b, String value) {
        b.append(Consts.S.DOUBLE_QUOTE).append(value).append(Consts.S.DOUBLE_QUOTE);
    }
}
