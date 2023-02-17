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

package com.fizzgate.util;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import com.fizzgate.config.RedisReactiveConfig;
import com.fizzgate.config.RedisReactiveProperties;

import java.time.Duration;

/**
 * @apiNote just helper, RedisReactiveConfig is best practice
 * <p/>
 *
 * @author hongqiaowei
 */

public abstract class ReactiveRedisHelper {

    private ReactiveRedisHelper() {
    }

    public static ReactiveRedisConnectionFactory getConnectionFactory(RedisReactiveProperties redisReactiveProperties) {
        if (redisReactiveProperties.getType() == RedisReactiveProperties.STANDALONE) {
            return getConnectionFactory(redisReactiveProperties.getHost(), redisReactiveProperties.getPort(), redisReactiveProperties.getPassword(), redisReactiveProperties.getDatabase());
        } else {
            return getClusterConnectionFactory(redisReactiveProperties);
        }
    }

    public static ReactiveStringRedisTemplate getStringRedisTemplate(RedisReactiveProperties redisReactiveProperties) {
        ReactiveRedisConnectionFactory connectionFactory = getConnectionFactory(redisReactiveProperties);
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    /**
     * For standalone redis.
     */
    public static ReactiveRedisConnectionFactory getConnectionFactory(String host, int port, String password, int database) {
        RedisStandaloneConfiguration rsc = new RedisStandaloneConfiguration(host, port);
        if (password != null) {
            rsc.setPassword(password);
        }
        rsc.setDatabase(database);

        LettucePoolingClientConfiguration ccs = LettucePoolingClientConfiguration.builder()
                                                                                 .clientResources(RedisReactiveConfig.CLIENT_RESOURCES)
                                                                                 .clientOptions(ClientOptions.builder().publishOnScheduler(true).build())
                                                                                 .poolConfig(new GenericObjectPoolConfig<>())
                                                                                 .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(rsc, ccs);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * For standalone redis.
     */
    public static ReactiveStringRedisTemplate getStringRedisTemplate(String host, int port, String password, int database) {
        ReactiveRedisConnectionFactory connectionFactory = getConnectionFactory(host, port, password, database);
        return new ReactiveStringRedisTemplate(connectionFactory);
    }

    public static ReactiveRedisConnectionFactory getClusterConnectionFactory(RedisReactiveProperties redisReactiveProperties) {
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        String password = redisReactiveProperties.getPassword();
        if (password != null) {
            redisClusterConfiguration.setPassword(password);
        }
        redisClusterConfiguration.setClusterNodes(redisReactiveProperties.getClusterNodes());
        int maxRedirects = redisReactiveProperties.getMaxRedirects();
        if (maxRedirects > 0) {
            redisClusterConfiguration.setMaxRedirects(maxRedirects);
        }

        ClusterTopologyRefreshOptions.Builder builder = ClusterTopologyRefreshOptions.builder();
        int clusterRefreshPeriod = redisReactiveProperties.getClusterRefreshPeriod();
        builder = builder.enablePeriodicRefresh(Duration.ofSeconds(clusterRefreshPeriod));
        boolean enableAllAdaptiveRefreshTriggers = redisReactiveProperties.isEnableAllAdaptiveRefreshTriggers();
        if (enableAllAdaptiveRefreshTriggers) {
            builder = builder.enableAllAdaptiveRefreshTriggers();
        }
        ClusterTopologyRefreshOptions topologyRefreshOptions = builder.build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                                                                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(clusterRefreshPeriod)))
                                                                        .topologyRefreshOptions(topologyRefreshOptions)
                                                                        .publishOnScheduler(true)
                                                                        .build();

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        int minIdle = redisReactiveProperties.getMinIdle();
        if (minIdle > 0) {
            poolConfig.setMinIdle(minIdle);
        }
        int maxIdle = redisReactiveProperties.getMaxIdle();
        if (maxIdle > 0) {
            poolConfig.setMaxIdle(maxIdle);
        }
        int maxTotal = redisReactiveProperties.getMaxTotal();
        if (maxTotal > 0) {
            poolConfig.setMaxTotal(maxTotal);
        } else {
            poolConfig.setMaxTotal(poolConfig.getMaxTotal() * 2);
        }
        Duration maxWait = redisReactiveProperties.getMaxWait();
        if (maxWait != null) {
            poolConfig.setMaxWait(maxWait);
        }
        Duration timeBetweenEvictionRuns = redisReactiveProperties.getTimeBetweenEvictionRuns();
        if (timeBetweenEvictionRuns != null) {
            poolConfig.setTimeBetweenEvictionRuns(timeBetweenEvictionRuns);
        }

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                                                                                   .clientResources(RedisReactiveConfig.CLIENT_RESOURCES)
                                                                                   // .commandTimeout(Duration.ofSeconds(60))
                                                                                   .poolConfig(poolConfig)
                                                                                   .readFrom(redisReactiveProperties.getReadFrom())
                                                                                   .clientOptions(clusterClientOptions)
                                                                                   .build();

        LettuceConnectionFactory reactiveRedisConnectionFactory = new LettuceConnectionFactory(redisClusterConfiguration, clientConfig);
        reactiveRedisConnectionFactory.afterPropertiesSet();
        return reactiveRedisConnectionFactory;
    }

    public static ReactiveStringRedisTemplate getClusterStringRedisTemplate(RedisReactiveProperties redisReactiveProperties) {
        ReactiveRedisConnectionFactory connectionFactory = getClusterConnectionFactory(redisReactiveProperties);
        return new ReactiveStringRedisTemplate(connectionFactory);
    }
}
