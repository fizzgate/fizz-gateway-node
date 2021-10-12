package we.dict;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.FizzAppContext;
import we.plugin.auth.ApiConfigService;
import we.plugin.auth.ApiConfigServiceProperties;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.util.JacksonUtils;
import we.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class DictTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    DictService dictService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        dictService = new DictService();
        ReflectionUtils.set(dictService, "rt", reactiveStringRedisTemplate);
    }

    @Test
    void constructTest() throws JsonProcessingException {
        String json = "{\"id\":1,\"key\":\"key\",\"type\":4,\"value\":\"{\\\"a0\\\":\\\"v0\\\",\\\"a1\\\":66}\",\"create\":1633756859538,\"update\":1633756859538,\"isDeleted\":1}";
        Dict dict = JacksonUtils.readValue(json, Dict.class);
//      assertEquals(96.12347, dict.numberVal.doubleValue());
//      assertEquals("96.12347", dict.numberVal.toPlainString());
//      System.err.println(dict.toString());
    }

    @Test
    void initTest() throws Throwable {

        FizzAppContext.appContext = new GenericApplicationContext();
        FizzAppContext.appContext.refresh();

        Map<String, String> dictsMap = new HashMap<>();
        dictsMap.put("key0", "{\"id\":1,\"key\":\"key0\",\"type\":2,\"value\":\"val0\",\"create\":1633756859538,\"update\":1633756859538,\"isDeleted\":1}");
        dictsMap.put("key1", "{\"id\":1,\"key\":\"key1\",\"type\":2,\"value\":\"val1\",\"create\":1633756859538,\"update\":1633756859538,\"isDeleted\":1}");
        stringRedisTemplate.opsForHash().putAll("fizz_dict", dictsMap);

        dictService.init();
    }
}
