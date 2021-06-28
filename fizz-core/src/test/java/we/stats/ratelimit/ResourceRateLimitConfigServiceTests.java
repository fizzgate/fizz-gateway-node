package we.stats.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.util.JacksonUtils;

import javax.annotation.Resource;

import java.util.Map;

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
        resourceRateLimitConfigService.setReactiveStringRedisTemplate(reactiveStringRedisTemplate);
    }

    @Test
    void initTest() throws Throwable {
        stringRedisTemplate.opsForHash().put("fizz_rate_limit", "2", "{\"concurrents\":66,\"enable\":1,\"id\":2,\"isDeleted\":0,\"resource\":\"service_default\",\"type\":2}");
        stringRedisTemplate.opsForHash().put("fizz_rate_limit", "3", "{\"concurrents\":88,\"enable\":1,\"id\":3,\"isDeleted\":0,    \"type\":6,    \"app\":\"xapp\",    \"service\":\"yservice\"    }");
        resourceRateLimitConfigService.init();
        ResourceRateLimitConfig resourceRateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig(ResourceRateLimitConfig.SERVICE_DEFAULT_RESOURCE);
        // Map<String, ResourceRateLimitConfig> resourceRateLimitConfigMap = resourceRateLimitConfigService.getResourceRateLimitConfigMap();
        // System.err.println(JacksonUtils.writeValueAsString(resourceRateLimitConfigMap));
        assertEquals(resourceRateLimitConfig.concurrents, 66);
        // System.err.println(resourceRateLimitConfig);
        // Thread.currentThread().join();
        resourceRateLimitConfig = resourceRateLimitConfigService.getResourceRateLimitConfig("xapp^^^yservice^");
        assertEquals(resourceRateLimitConfig.concurrents, 88);

        Thread.sleep(4000);
        // System.err.println("init test end");
    }
}
