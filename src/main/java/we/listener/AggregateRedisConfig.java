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

package we.listener;

import we.config.RedisReactiveConfig;
import we.config.RedisReactiveProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;

/**
 * 聚合配置Redis配置
 * @author zhongjie
 */
@Configuration
public class AggregateRedisConfig extends RedisReactiveConfig {
    static final String AGGREGATE_REACTIVE_REDIS_PROPERTIES = "aggregateReactiveRedisProperties";
    private static final String AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY = "aggregateReactiveRedisConnectionFactory";
    public static final String AGGREGATE_REACTIVE_REDIS_TEMPLATE = "aggregateReactiveRedisTemplate";
    static final String AGGREGATE_REACTIVE_REDIS_MESSAGE_LISTENER_CONTAINER = "aggregateReactiveRedisMessageListenerContainer";

    @ConfigurationProperties(prefix = "aggregate.redis")
    @Configuration(AGGREGATE_REACTIVE_REDIS_PROPERTIES)
    public static class AggregateRedisReactiveProperties extends RedisReactiveProperties {
    }

    public AggregateRedisConfig(@Qualifier(AGGREGATE_REACTIVE_REDIS_PROPERTIES) RedisReactiveProperties properties) {
        super(properties);
    }

    @Override
    @Bean(AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY)
    public ReactiveRedisConnectionFactory lettuceConnectionFactory() {
        return super.lettuceConnectionFactory();
    }

    @Override
    @Bean(AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            @Qualifier(AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY) ReactiveRedisConnectionFactory factory) {
        return super.reactiveStringRedisTemplate(factory);
    }

    @Bean(AGGREGATE_REACTIVE_REDIS_MESSAGE_LISTENER_CONTAINER)
    public ReactiveRedisMessageListenerContainer aggregateReactiveRedisMessageListenerContainer(
            @Qualifier(AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY) ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }
}
