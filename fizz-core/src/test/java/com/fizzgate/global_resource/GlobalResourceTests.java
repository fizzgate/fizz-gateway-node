package com.fizzgate.global_resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fizzgate.Fizz;
import com.fizzgate.FizzAppContext;
import com.fizzgate.global_resource.GlobalResource;
import com.fizzgate.global_resource.GlobalResourceService;
import com.fizzgate.redis.RedisProperties;
import com.fizzgate.redis.RedisServerConfiguration;
import com.fizzgate.redis.RedisTemplateConfiguration;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ReflectionUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class GlobalResourceTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    GlobalResourceService globalResourceService;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        globalResourceService = new GlobalResourceService();
        ReflectionUtils.set(globalResourceService, "rt", reactiveStringRedisTemplate);
    }

    @Test
    void constructTest() throws JsonProcessingException {
        String json = "{\"id\":1,\"key\":\"key\",\"type\":4,\"value\":\"{\\\"a0\\\":\\\"v0\\\",\\\"a1\\\":66}\",\"create\":1633756859538,\"update\":1633756859538,\"isDeleted\":1}";
        GlobalResource globalResource = JacksonUtils.readValue(json, GlobalResource.class);
//      assertEquals(96.12347, globalResource.numberVal.doubleValue());
//      assertEquals("96.12347", globalResource.numberVal.toPlainString());
//      System.err.println(globalResource.toString());
    }

    @Test
    void initTest() throws Throwable {

        Fizz.context = new GenericApplicationContext();
        Fizz.context.refresh();

        Map<String, String> resourceMap = new HashMap<>();
        resourceMap.put("key0", "{\"id\":1,\"key\":\"key0\",\"type\":2,\"value\":\"val0\",\"create\":1633756859538,\"update\":1633756859538,\"isDeleted\":1}");
        resourceMap.put("key1", "{\"id\":1,\"key\":\"key1\",\"type\":2,\"value\":\"val1\",\"create\":1633756859538,\"update\":1633756859538,\"isDeleted\":1}");
        stringRedisTemplate.opsForHash().putAll("fizz_global_resource", resourceMap);

        globalResourceService.init();
    }
}
