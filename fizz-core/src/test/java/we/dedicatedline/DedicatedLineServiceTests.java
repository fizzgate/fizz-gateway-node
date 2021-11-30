package we.dedicatedline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import we.Fizz;
import we.redis.RedisProperties;
import we.redis.RedisServerConfiguration;
import we.redis.RedisTemplateConfiguration;
import we.util.ReflectionUtils;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class DedicatedLineServiceTests {

    @Resource
    StringRedisTemplate         stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    DedicatedLineService        dedicatedLineService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        dedicatedLineService = new DedicatedLineService();
        ReflectionUtils.set(dedicatedLineService, "rt", reactiveStringRedisTemplate);
    }

    @Test
    void initTest() throws Throwable {
        Fizz.context = new GenericApplicationContext();
        Fizz.context.refresh();
    }
}
