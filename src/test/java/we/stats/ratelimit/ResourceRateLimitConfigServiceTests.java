package we.stats.ratelimit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;

import javax.annotation.Resource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
// @ActiveProfiles("unittest")
public class ResourceRateLimitConfigServiceTests {

    @Resource
    RedisProperties redisProperties;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    ResourceRateLimitConfigService resourceRateLimitConfigService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        resourceRateLimitConfigService = new ResourceRateLimitConfigService();
        // Field rt = ResourceRateLimitConfigService.class.getField("rt");
        // ReflectionUtils.makeAccessible(rt);
        // ReflectionUtils.setField(rt, resourceRateLimitConfigService, reactiveStringRedisTemplate);
        resourceRateLimitConfigService.setReactiveStringRedisTemplate( reactiveStringRedisTemplate);
    }

    @Test
    void initTest() throws Throwable {
        // System.err.println(redisProperties);
        // System.err.println(stringRedisTemplate);
        // System.err.println(reactiveStringRedisTemplate);

        // stringRedisTemplate.opsForValue().set("name", "F-22");
        // Thread.sleep(2000);
        // String name = stringRedisTemplate.opsForValue().get("name");
        // assertEquals(name, "F-22");
        // System.err.println(name);

        // stringRedisTemplate.opsForHash().put("fizz_rate_limit", "2", "{\"concurrents\":100,\"enable\":1,\"id\":2,\"isDeleted\":0,\"resource\":\"service_default\",\"type\":2}");
        // resourceRateLimitConfigService.init();
        // ResourceRateLimitConfig resourceRateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig("service_default");
        //
        // System.err.println(resourceRateLimitConfig);
        // System.err.println("init test end");
        // Thread.currentThread().join();
    }
}
