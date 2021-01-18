package we.stats.ratelimit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import redis.embedded.RedisServer;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
// @ActiveProfiles("unittest")
public class ResourceRateLimitConfigServiceTests {

    @Resource
    private RedisProperties redisProperties;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    ResourceRateLimitConfigService resourceRateLimitConfigService;

    @BeforeAll
    void beforeAllTests() {
        resourceRateLimitConfigService = new ResourceRateLimitConfigService();
    }

    @Test
    void initTest() throws InterruptedException {
        System.err.println(redisProperties);
        System.err.println(stringRedisTemplate);
        stringRedisTemplate.opsForValue().set("name", "F-22");
        Thread.sleep(2000);
        String name = stringRedisTemplate.opsForValue().get("name");
        assertEquals(name, "F-22");
        System.err.println(name);
    }
}
