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

package com.fizzgate.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fizzgate.config.AggregateRedisConfig;
import com.fizzgate.config.SchedConfig;
import com.fizzgate.util.Consts;
import com.fizzgate.util.DateTimeUtils;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ThreadContext;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Service
public class FizzMonitorService extends SchedConfig {

    private static final Logger MONITOR_LOGGER = LoggerFactory.getLogger("monitor");
    private static final Logger LOGGER         = LoggerFactory.getLogger(FizzMonitorService.class);

    public static final byte ERROR_ALARM         = 1;
    public static final byte TIMEOUT_ALARM       = 2;
    public static final byte RATE_LIMIT_ALARM    = 3;
    public static final byte CIRCUIT_BREAK_ALARM = 4;

    private static class Alarm {

        public String service;
        public String path;
        public int    type;
        public String desc;
        public long   timestamp;
        public int    reqs = 0;
        public long   start;

        @Override
        public String toString() {
            return JacksonUtils.writeValueAsString(this);
        }
    }

    @Value("${fizz.monitor.alarm.enable:true}")
    private boolean alarmEnable;

    @Value("${fizz.monitor.alarm.dest:redis}")
    private String dest;

    @Value("${fizz.monitor.alarm.queue:fizz_alarm_channel_new}")
    private String queue;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    private Map<Long/*thread id*/,
                                      Map/*LinkedHashMap*/<Long/*time win start*/,
                                                                                      Map<String/*service+path+type*/, Alarm>
                                                                                      >
                                      >
            threadTimeWinAlarmMap = new HashMap<>();

    public void alarm(String service, String path, byte type, String desc) {
        if (alarmEnable) {
            long tid = Thread.currentThread().getId();
            Map<Long, Map<String, Alarm>> timeWinAlarmMap = threadTimeWinAlarmMap.get(tid);
            if (timeWinAlarmMap == null) {
                timeWinAlarmMap = new LinkedHashMap<Long, Map<String, Alarm>>(4, 1) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry eldest) {
                        return size() > 2;
                    }
                };
                threadTimeWinAlarmMap.put(tid, timeWinAlarmMap);
            }

            long currentTimeWinStart = DateTimeUtils.get10sTimeWinStart(1);
            Map<String, Alarm> alarmMap = timeWinAlarmMap.computeIfAbsent(currentTimeWinStart, k -> new HashMap<>(128));

            String key = ThreadContext.getStringBuilder().append(service).append(path).append(type).toString();
            Alarm alarm = alarmMap.get(key);
            if (alarm == null) {
                alarm = new Alarm();
                alarm.service = service;
                alarm.path    = path;
                alarm.type    = type;
                alarmMap.put(key, alarm);
            }
            alarm.desc = desc;
            alarm.timestamp = System.currentTimeMillis();
            alarm.reqs++;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("update alarm: {} at {}", alarm, DateTimeUtils.convert(alarm.timestamp, Consts.DP.DP19));
            }
        }
    }

    @Scheduled(cron = "${fizz.monitor.alarm.sched.cron:2/10 * * * * ?}")
    public void sched() {
        long prevTimeWinStart = DateTimeUtils.get10sTimeWinStart(2);
        Map<String, Alarm> alarmMap = ThreadContext.getHashMap();
        threadTimeWinAlarmMap.forEach(
                (t, timeWinAlarmMap) -> {
                    Map<String, Alarm> alarmMap0 = timeWinAlarmMap.get(prevTimeWinStart);
                    if (alarmMap0 != null) {
                        alarmMap0.forEach(
                                (spt, alarm) -> {
                                    Alarm a = alarmMap.get(spt);
                                    if (a == null) {
                                        alarm.start = prevTimeWinStart;
                                        alarmMap.put(spt, alarm);
                                    } else {
                                        a.reqs = a.reqs + alarm.reqs;
                                        if (alarm.timestamp > a.timestamp) {
                                            a.timestamp = alarm.timestamp;
                                            a.desc = alarm.desc;
                                        }
                                    }
                                }
                        );
                    }
                }
        );
        if (alarmMap.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no alarm in {} window", DateTimeUtils.convert(prevTimeWinStart, Consts.DP.DP19));
            }
        } else {
            alarmMap.forEach(
                    (spt, alarm) -> {
                        String msg = alarm.toString();
                        if (Consts.KAFKA.equals(dest)) {
                            MONITOR_LOGGER.info(msg);
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("send alarm {} which belong to {} window to topic", msg, DateTimeUtils.convert(alarm.start, Consts.DP.DP19));
                            }
                        } else {
                            rt.convertAndSend(queue, msg).subscribe();
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("send alarm {} which belong to {} window to channel {}", msg, DateTimeUtils.convert(alarm.start, Consts.DP.DP19), queue);
                            }
                        }
                    }
            );
        }
    }

}
