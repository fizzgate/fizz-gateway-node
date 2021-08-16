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

package we.util;

import io.lettuce.core.ClientOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import we.config.RedisReactiveConfig;

/**
 * @apiNote just helper, RedisReactiveConfig is best practice
 * <p/>
 *
 * @author hongqiaowei
 */

public abstract class ReactiveRedisHelper {

    public static ReactiveRedisConnectionFactory getConnectionFactory(String host, int port, String password, int database) {
        RedisStandaloneConfiguration rcs = new RedisStandaloneConfiguration(host, port);
        if (password != null) {
            rcs.setPassword(password);
        }
        rcs.setDatabase(database);

        LettucePoolingClientConfiguration ccs = LettucePoolingClientConfiguration.builder()
                .clientResources(RedisReactiveConfig.clientResources)
                .clientOptions(ClientOptions.builder().publishOnScheduler(true).build())
                .poolConfig(new GenericObjectPoolConfig())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(rcs, ccs);
        factory.afterPropertiesSet();
        return factory;
    }

    public static ReactiveStringRedisTemplate getStringRedisTemplate(String host, int port, String password, int database) {
        ReactiveRedisConnectionFactory connectionFactory = getConnectionFactory(host, port, password, database);
        ReactiveStringRedisTemplate template = new ReactiveStringRedisTemplate(connectionFactory);
        return template;
    }
}
