package we.redis;

import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author hongqiaowei
 */

@TestConfiguration
public class RedisServerConfiguration {

    private RedisServer redisServer;

    public RedisServerConfiguration(RedisProperties redisProperties) {
        redisServer = RedisServer.builder()
                .port(redisProperties.getPort())
                .setting("maxmemory 32M")
                .build();
    }

    @PostConstruct
    public void postConstruct() {
        redisServer.start();
    }

    @PreDestroy
    public void preDestroy() {
        redisServer.stop();
    }
}