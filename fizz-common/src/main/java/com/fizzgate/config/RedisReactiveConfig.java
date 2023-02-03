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

package com.fizzgate.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

/**
 * @author hongqiaowei
 */

public abstract class RedisReactiveConfig {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RedisReactiveConfig.class);

    // this should not be changed unless there is a good reason to do so
    private static final int             ps               = Runtime.getRuntime().availableProcessors();

    /**
     * @deprecated and renamed to CLIENT_RESOURCES
     */
    @Deprecated
    public  static final ClientResources clientResources  = DefaultClientResources.builder()
                                                                                  .ioThreadPoolSize(ps)
                                                                                  .computationThreadPoolSize(ps)
                                                                                  .build();

    public  static final ClientResources CLIENT_RESOURCES = clientResources;

    private RedisReactiveProperties redisReactiveProperties;

    public RedisReactiveConfig(RedisReactiveProperties properties) {
        redisReactiveProperties = properties;
    }

    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory fact) {
        return new ReactiveStringRedisTemplate(fact);
    }

    public ReactiveRedisConnectionFactory lettuceConnectionFactory() {

        ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

        if (redisReactiveProperties.getType() == RedisReactiveProperties.STANDALONE) {

            RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisReactiveProperties.getHost(), redisReactiveProperties.getPort());
            String password = redisReactiveProperties.getPassword();
            if (password != null) {
                redisStandaloneConfiguration.setPassword(password);
            }
            redisStandaloneConfiguration.setDatabase(redisReactiveProperties.getDatabase());

            GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(poolConfig.getMaxTotal() * 2);
            LettucePoolingClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
                                                                                                     .clientResources(clientResources)
                                                                                                     .clientOptions(ClientOptions.builder().publishOnScheduler(true).build())
                                                                                                     .poolConfig(poolConfig)
                                                                                                     .build();

            reactiveRedisConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfiguration);

        } else {

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
                                                                                       .clientResources(clientResources)
                                                                                       // .commandTimeout(Duration.ofSeconds(60))
                                                                                       .poolConfig(poolConfig)
                                                                                       .readFrom(redisReactiveProperties.getReadFrom())
                                                                                       .clientOptions(clusterClientOptions)
                                                                                       .build();

            reactiveRedisConnectionFactory =  new LettuceConnectionFactory(redisClusterConfiguration, clientConfig);
        }

        LOGGER.info("build reactive redis connection factory for {}", redisReactiveProperties);
        return reactiveRedisConnectionFactory;
    }
}
