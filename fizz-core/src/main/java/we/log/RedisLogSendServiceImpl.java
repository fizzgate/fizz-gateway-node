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

package we.log;

import com.alibaba.fastjson.JSON;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import we.config.AggregateRedisConfig;
import we.config.AggregateRedisConfigProperties;

/**
 * {@link LogSendService} impl class, using redis channel to send log
 *
 * @author zhongjie
 */
public class RedisLogSendServiceImpl implements LogSendService {

    public RedisLogSendServiceImpl(AggregateRedisConfigProperties aggregateRedisConfigProperties,
                                   AggregateRedisConfig aggregateRedisConfig, ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.aggregateRedisConfigProperties = aggregateRedisConfigProperties;
        this.aggregateRedisConfig = aggregateRedisConfig;
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    private AggregateRedisConfigProperties aggregateRedisConfigProperties;
    private AggregateRedisConfig aggregateRedisConfig;
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Override
    public void send(LogSend logSend) {
        if (aggregateRedisConfigProperties.isSendLogOpen()) {
            reactiveStringRedisTemplate.convertAndSend(aggregateRedisConfigProperties.getSendLogChannel(), JSON.toJSONString(logSend)).subscribe();
        }
    }
}
