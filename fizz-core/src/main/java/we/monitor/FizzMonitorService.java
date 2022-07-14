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

package we.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import we.config.AggregateRedisConfig;
import we.util.Consts;
import we.util.ThreadContext;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@Service
public class FizzMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger("monitor");

    public static final byte ERROR_ALARM         = 1;
    public static final byte TIMEOUT_ALARM       = 2;
    public static final byte RATE_LIMIT_ALARM    = 3;
    public static final byte CIRCUIT_BREAK_ALARM = 4;

    private static final String _service   = "\"service\":";
    private static final String _path      = "\"path\":";
    private static final String _type      = "\"type\":";
    private static final String _desc      = "\"desc\":";
    private static final String _timestamp = "\"timestamp\":";

    @Value("${fizz.monitor.alarm.enable:true}")
    private boolean alarmEnable;

    @Value("${fizz.monitor.alarm.dest:redis}")
    private String dest;

    @Value("${fizz.monitor.alarm.queue:fizz_alarm_channel}")
    private String queue;

    @Resource(name = AggregateRedisConfig.AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    private ReactiveStringRedisTemplate rt;

    public void sendAlarm(String service, String path, byte type, String desc, long timestamp) {
        if (alarmEnable) {
            StringBuilder b = ThreadContext.getStringBuilder();
            b.append(Consts.S.LEFT_BRACE);
                b.append(_service);   toJsonStrVal(b, service);       b.append(Consts.S.COMMA);
                b.append(_path);      toJsonStrVal(b, path);          b.append(Consts.S.COMMA);
                b.append(_type);      b.append(type);                 b.append(Consts.S.COMMA);

                if (desc != null) {
                b.append(_desc);      toJsonStrVal(b, desc);          b.append(Consts.S.COMMA);
                }

                b.append(_timestamp)  .append(timestamp);
            b.append(Consts.S.RIGHT_BRACE);
            String msg = b.toString();
            if (Consts.KAFKA.equals(dest)) {
                // LOGGER.warn(msg, LogService.HANDLE_STGY, LogService.toKF(queue));
                LOGGER.info(msg);
            } else {
                rt.convertAndSend(queue, msg).subscribe();
            }
        }
    }

    private static void toJsonStrVal(StringBuilder b, String value) {
        b.append(Consts.S.DOUBLE_QUOTE).append(value).append(Consts.S.DOUBLE_QUOTE);
    }

}
