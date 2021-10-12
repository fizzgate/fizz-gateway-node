package we.plugin.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.FizzAppContext;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class ApiConfigServiceTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    ApiConfigServiceProperties apiConfigServiceProperties;

    ApiConfigService apiConfigService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        ApiConfigServiceProperties apiConfigServiceProperties = new ApiConfigServiceProperties();
        ReflectionUtils.set(apiConfigServiceProperties, "fizzApiConfig", "fizz_api_config_route");
        ReflectionUtils.set(apiConfigServiceProperties, "fizzPluginConfig", "fizz_plugin_config");
        ReflectionUtils.set(apiConfigServiceProperties, "fizzApiConfigChannel", "fizz_api_config_channel_route");
        ReflectionUtils.set(apiConfigServiceProperties, "fizzPluginConfigChannel", "fizz_plugin_config_channel");
        apiConfigService = new ApiConfigService();
        ReflectionUtils.set(apiConfigService, "rt", reactiveStringRedisTemplate);
        ReflectionUtils.set(apiConfigService, "apiConfigServiceProperties", apiConfigServiceProperties);
    }

    @Test
    void initTest() throws Throwable {

        FizzAppContext.appContext = new GenericApplicationContext();
        FizzAppContext.appContext.refresh();

        Map<String, String> pluginsMap = new HashMap<>();
        pluginsMap.put("p0", "{\"plugin\":\"p0\", \"fixedConfig\":\"p0fc\"}");
        pluginsMap.put("p1", "{\"plugin\":\"p1\", \"fixedConfig\":\"p1fc\"}");
        stringRedisTemplate.opsForHash().putAll("fizz_plugin_config", pluginsMap);

        apiConfigService.init();
    }
}
