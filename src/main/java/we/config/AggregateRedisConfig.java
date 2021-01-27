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

import com.alibaba.nacos.api.config.annotation.NacosValue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReactiveRedisClusterConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.SpringSessionRedisConnectionFactory;

import we.log.LogSendAppender;
import we.log.RedisLogSendServiceImpl;

/**
 * aggregate Redis config
 *
 * @author zhongjie
 */
@Configuration
public class AggregateRedisConfig extends RedisReactiveConfig {
    static final String AGGREGATE_REACTIVE_REDIS_PROPERTIES = "aggregateReactiveRedisProperties";
    private static final String AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY = "aggregateReactiveRedisConnectionFactory";
    public static final String AGGREGATE_REACTIVE_REDIS_TEMPLATE = "aggregateReactiveRedisTemplate";
    public static final String AGGREGATE_REACTIVE_REDIS_MESSAGE_LISTENER_CONTAINER = "aggregateReactiveRedisMessageListenerContainer";

    private static final String SEND_LOG_TYPE_REDIS = "redis";
    public static ProxyLettuceConnectionFactory proxyLettuceConnectionFactory;

    @NacosValue(value = "${send-log.open:false}", autoRefreshed = true)
    @Value("${send-log.open:false}")
    private boolean sendLogOpen;

    @NacosValue(value = "${send-log.channel:fizz_send_log_channel}", autoRefreshed = true)
    @Value("${send-log.channel:fizz_log_channel}")
    private String sendLogChannel;

    @NacosValue(value = "${send-log.type:redis}", autoRefreshed = true)
    @Value("${send-log.type:redis}")
    private String sendLogType;

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
        LettuceConnectionFactory lettuceConnectionFactory = (LettuceConnectionFactory) super.lettuceConnectionFactory();
        if (SEND_LOG_TYPE_REDIS.equals(sendLogType)) {
            proxyLettuceConnectionFactory = new ProxyLettuceConnectionFactory(lettuceConnectionFactory);
            proxyLettuceConnectionFactory.afterPropertiesSet();
            return proxyLettuceConnectionFactory;
        } else {
            return lettuceConnectionFactory;
        }
    }

    @Override
    @Bean(AGGREGATE_REACTIVE_REDIS_TEMPLATE)
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            @Qualifier(AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY) ReactiveRedisConnectionFactory factory) {
        ReactiveStringRedisTemplate reactiveStringRedisTemplate = super.reactiveStringRedisTemplate(factory);

        // test redis can connect
        reactiveStringRedisTemplate.getConnectionFactory().getReactiveConnection().ping().block();

        if (SEND_LOG_TYPE_REDIS.equals(sendLogType)) {
            // set LogSendAppender.logSendService here to let send log as early as possible
            LogSendAppender.logSendService = new RedisLogSendServiceImpl(this, reactiveStringRedisTemplate);
        }

        return reactiveStringRedisTemplate;
    }

    @Bean(AGGREGATE_REACTIVE_REDIS_MESSAGE_LISTENER_CONTAINER)
    public ReactiveRedisMessageListenerContainer aggregateReactiveRedisMessageListenerContainer(
            @Qualifier(AGGREGATE_REACTIVE_REDIS_CONNECTION_FACTORY) ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisMessageListenerContainer(factory);
    }
    
    /**
     * Spring Session Redis
     */
    @Bean
	@SpringSessionRedisConnectionFactory
	public LettuceConnectionFactory springSessionRedisConnectionFactory(){
    	return (LettuceConnectionFactory) super.lettuceConnectionFactory();
    }
    
	/**
	 * Spring session redis serializer
	 * @return
	 */
	@Bean("springSessionDefaultRedisSerializer")
	public RedisSerializer<Object> defaultRedisSerializer() {
		return new GenericJackson2JsonRedisSerializer();
	}

	/**
	 * Default RedisTemplate
	 * @return
	 */
	@Bean
	@Primary
	public RedisTemplate<Object, Object> redisTemplate() {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory((RedisConnectionFactory) lettuceConnectionFactory());
		template.setKeySerializer(new StringRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
		return template;
	}
	
    public boolean getSendLogOpen() {
        return sendLogOpen;
    }

    public String getSendLogChannel() {
        return sendLogChannel;
    }

    public static class ProxyLettuceConnectionFactory implements RedisConnectionFactory, ReactiveRedisConnectionFactory {
        ProxyLettuceConnectionFactory(LettuceConnectionFactory lettuceConnectionFactory) {
            this.lettuceConnectionFactory = lettuceConnectionFactory;
        }

        private LettuceConnectionFactory lettuceConnectionFactory;

        public void destroy() {
            lettuceConnectionFactory.destroy();
        }

        void afterPropertiesSet() {
            lettuceConnectionFactory.afterPropertiesSet();
        }

        @Override
        public ReactiveRedisConnection getReactiveConnection() {
            return lettuceConnectionFactory.getReactiveConnection();
        }

        @Override
        public ReactiveRedisClusterConnection getReactiveClusterConnection() {
            return lettuceConnectionFactory.getReactiveClusterConnection();
        }

        @Override
        public RedisConnection getConnection() {
            return lettuceConnectionFactory.getConnection();
        }

        @Override
        public RedisClusterConnection getClusterConnection() {
            return lettuceConnectionFactory.getClusterConnection();
        }

        @Override
        public boolean getConvertPipelineAndTxResults() {
            return lettuceConnectionFactory.getConvertPipelineAndTxResults();
        }

        @Override
        public RedisSentinelConnection getSentinelConnection() {
            return lettuceConnectionFactory.getSentinelConnection();
        }

        @Override
        public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
            return lettuceConnectionFactory.translateExceptionIfPossible(ex);
        }
    }
}
