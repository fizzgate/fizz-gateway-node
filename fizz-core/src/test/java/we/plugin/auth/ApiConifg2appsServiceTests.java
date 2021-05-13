package we.plugin.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.stats.ratelimit.ResourceRateLimitConfigService;
import we.util.JacksonUtils;
import we.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class ApiConifg2appsServiceTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    ApiConifg2appsService apiConifg2appsService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        apiConifg2appsService = new ApiConifg2appsService();
        ReflectionUtils.set(apiConifg2appsService, "rt", reactiveStringRedisTemplate);
    }

    @Test
    void initTest() throws Throwable {
        Map<String, String> apiConfigId2appSetCountMap = new HashMap<>();
        apiConfigId2appSetCountMap.put("60", "1");
        apiConfigId2appSetCountMap.put("61", "2");
        stringRedisTemplate.opsForHash().putAll("fizz_api_config_app_set_size", apiConfigId2appSetCountMap);

        stringRedisTemplate.opsForSet().add("fizz_api_config_app:60_0", "app_a");
        stringRedisTemplate.opsForSet().add("fizz_api_config_app:61_0", "app_b", "app_c");
        stringRedisTemplate.opsForSet().add("fizz_api_config_app:61_1", "app_d");

        apiConifg2appsService.init();
        Thread.sleep(4000);

        Map<Integer, Set<String>> apiConfig2appsMap = apiConifg2appsService.getApiConfig2appsMap();
        // System.err.println("r: " + JacksonUtils.writeValueAsString(apiConfig2appsMap));
        assertTrue(apiConfig2appsMap.get(61).contains("app_c"));
    }
}
