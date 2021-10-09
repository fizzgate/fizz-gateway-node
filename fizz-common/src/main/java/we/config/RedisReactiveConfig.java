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

package we.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * @author hongqiaowei
 */

public abstract class RedisReactiveConfig {

    protected static final Logger log = LoggerFactory.getLogger(RedisReactiveConfig.class);

    // this should not be changed unless there is a truely good reason to do so
    private static final int ps = Runtime.getRuntime().availableProcessors();
    public  static final ClientResources clientResources = DefaultClientResources.builder()
                                                                                 .ioThreadPoolSize(ps)
                                                                                 .computationThreadPoolSize(ps)
                                                                                 .build();

    private RedisReactiveProperties redisReactiveProperties;

    public RedisReactiveConfig(RedisReactiveProperties properties) {
        redisReactiveProperties = properties;
    }

    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory fact) {
        return new ReactiveStringRedisTemplate(fact);
    }

    public ReactiveRedisConnectionFactory lettuceConnectionFactory() {

        log.info("connect to {}", redisReactiveProperties);

        RedisStandaloneConfiguration rcs = new RedisStandaloneConfiguration(redisReactiveProperties.getHost(), redisReactiveProperties.getPort());
        String password = redisReactiveProperties.getPassword();
        if (password != null) {
            rcs.setPassword(password);
        }
        rcs.setDatabase(redisReactiveProperties.getDatabase());

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolConfig.getMaxTotal() * 2);
        LettucePoolingClientConfiguration ccs = LettucePoolingClientConfiguration.builder()
                                                .clientResources(clientResources)
                                                .clientOptions(ClientOptions.builder().publishOnScheduler(true).build())
                                                .poolConfig(poolConfig)
                                                .build();

        return new LettuceConnectionFactory(rcs, ccs);
    }
}
