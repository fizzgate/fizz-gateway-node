package com.fizzgate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import com.fizzgate.config.RedisReactiveConfig;
import com.fizzgate.config.RedisReactiveProperties;

public class RedisClusterTests {

    // @Test
    void test() throws InterruptedException {
        System.setProperty("log4j2.isThreadContextMapInheritable", "true");
        Logger LOGGER = LogManager.getLogger(RedisClusterTests.class);

        RedisReactiveProperties redisReactiveProperties = new RedisReactiveProperties() {
        };
        redisReactiveProperties.setType(RedisReactiveProperties.CLUSTER);
        redisReactiveProperties.setPassword("123456");
        redisReactiveProperties.setClusterNodes("ip:port");

        RedisReactiveConfig redisReactiveConfig = new RedisReactiveConfig(redisReactiveProperties) {
        };
        LettuceConnectionFactory lettuceConnectionFactory = (LettuceConnectionFactory) redisReactiveConfig.lettuceConnectionFactory();
        lettuceConnectionFactory.afterPropertiesSet();
        ReactiveStringRedisTemplate reactiveStringRedisTemplate = redisReactiveConfig.reactiveStringRedisTemplate(lettuceConnectionFactory);
        reactiveStringRedisTemplate.opsForValue().set("hqw", "lancer").block();

        String channel = "ch1";
        reactiveStringRedisTemplate.listenToChannel(channel)
                                   .doOnError(
                                           t -> {
                                               LOGGER.error("lsn channel {} error", channel, t);
                                           }
                                   )
                                   .doOnSubscribe(
                                           s -> {
                                               LOGGER.info("success to lsn on {}", channel);
                                           }
                                   )
                                   .doOnNext(
                                           msg -> {
                                               String message = msg.getMessage();
                                               LOGGER.info("receive message: {}", message);
                                           }
                                   )
                                   .subscribe();
        Thread.currentThread().join();
    }
}
