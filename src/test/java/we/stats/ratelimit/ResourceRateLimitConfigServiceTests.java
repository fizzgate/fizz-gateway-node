package we.stats.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.redis.RedisTemplateConfiguration;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
// @ActiveProfiles("unittest")
public class ResourceRateLimitConfigServiceTests {

    // private static RedisServer redisServer;
    //
    // @BeforeAll
    // static void startRedis() {
    //     redisServer = RedisServer.builder()
    //             .port(6379)
    //             .setting("maxmemory 32M")
    //             .build();
    //     redisServer.start();
    // }
    //
    // @AfterAll
    // static void stopRedis() {
    //     redisServer.stop();
    // }

    @Resource
    private RedisProperties redisProperties;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void initTest() {
        System.err.println(redisProperties);
        System.err.println(stringRedisTemplate);
    }
}
