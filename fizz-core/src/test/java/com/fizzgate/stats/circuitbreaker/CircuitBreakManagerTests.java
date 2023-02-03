package com.fizzgate.stats.circuitbreaker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fizzgate.Fizz;
import com.fizzgate.redis.RedisProperties;
import com.fizzgate.redis.RedisServerConfiguration;
import com.fizzgate.redis.RedisTemplateConfiguration;
import com.fizzgate.stats.FlowStat;
import com.fizzgate.stats.ResourceStat;
import com.fizzgate.stats.TimeSlot;
import com.fizzgate.stats.circuitbreaker.CircuitBreakManager;
import com.fizzgate.stats.circuitbreaker.CircuitBreaker;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.ReflectionUtils;
import com.fizzgate.util.ResourceIdUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@TestPropertySource("/application.properties")
@SpringJUnitConfig(classes = {RedisProperties.class, RedisTemplateConfiguration.class, RedisServerConfiguration.class})
public class CircuitBreakManagerTests {

    @Resource
    StringRedisTemplate         stringRedisTemplate;

    @Resource
    ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    CircuitBreakManager         circuitBreakManager;

    @BeforeEach
    void beforeEach() throws NoSuchFieldException {
        circuitBreakManager = new CircuitBreakManager();
        ReflectionUtils.set(circuitBreakManager, "rt", reactiveStringRedisTemplate);
    }

    @Test
    void constructTest() throws JsonProcessingException {
        String json = "{\"id\":123456789012345,\"type\":1,\"service\":\"xservice\",\"path\":\"/ypath\",\"strategy\":1,\"ratioThreshold\":0.1,\"exceptionCount\":10,\"minRequestCount\":20,\"timeWindow\":5,\"statInterval\":5,\"recoveryStrategy\":2,\"recoveryTimeWindow\":5,\"responseContentType\":\"application/json\",\"responseContent\":\"error\",\"enable\":1,\"isDeleted\":0}";
        CircuitBreaker cb = JacksonUtils.readValue(json, CircuitBreaker.class);
        System.err.println("CircuitBreaker: " + cb);
    }

    @Test
    void initTest() throws Throwable {

        Fizz.context = new GenericApplicationContext();
        Fizz.context.refresh();

        Map<String, String> circuitBreakerMap = new HashMap<>();
        circuitBreakerMap.put("123456789012345", "{\"id\":123456789012345,\"type\":3,\"service\":\"xservice\",\"path\":\"/ypath\",\"strategy\":2,\"exceptionCount\":10,\"minRequestCount\":20,\"timeWindow\":5,\"statInterval\":5,\"recoveryStrategy\":3,\"responseContentType\":\"application/json\",\"responseContent\":\"error\",\"enable\":1,\"isDeleted\":0}");
        circuitBreakerMap.put("123456789012346", "{\"id\":123456789012346,\"type\":1,\"service\":\"service_default\",\"strategy\":2,\"exceptionCount\":20,\"minRequestCount\":40,\"timeWindow\":5,\"statInterval\":5,\"recoveryStrategy\":3,\"responseContentType\":\"application/json\",\"responseContent\":\"error\",\"enable\":1,\"isDeleted\":0}");
        stringRedisTemplate.opsForHash().putAll("fizz_degrade_rule", circuitBreakerMap);

        circuitBreakManager.init();
    }

    @Test
    void permitTest() {
        FlowStat flowStat = new FlowStat(circuitBreakManager);
        flowStat.cleanResource = false;
        flowStat.createTimeSlotOnlyTraffic = false;
        long currentTimeWindow = flowStat.currentTimeSlotId();

        MockServerHttpRequest mockServerHttpRequest = MockServerHttpRequest.get("/xxx").build();
        MockServerWebExchange mockServerWebExchange = MockServerWebExchange.from(mockServerHttpRequest);

        String service = "xservice";
        String path = "ypath";

        CircuitBreaker cb = new CircuitBreaker();
        cb.service = service;
        cb.path = path;
        cb.resource = ResourceIdUtils.buildResourceId(null, null, null, service, path);
        cb.breakStrategy = CircuitBreaker.BreakStrategy.TOTAL_ERRORS;
        cb.monitorDuration = 5 * 1000;
        cb.minRequests = 100;
        cb.totalErrorThreshold = 10;
        cb.breakDuration = 5 * 1000;
        cb.resumeStrategy = CircuitBreaker.ResumeStrategy.IMMEDIATE;
        cb.stateStartTime = currentTimeWindow;
        Map<String, CircuitBreaker> circuitBreakerMap = circuitBreakManager.getResource2circuitBreakerMap();
        circuitBreakerMap.put(cb.resource, cb);

        ResourceStat resourceStat = flowStat.getResourceStat(cb.resource);
        TimeSlot timeSlot = resourceStat.getTimeSlot(currentTimeWindow);
        timeSlot.setCompReqs(200);
        timeSlot.setErrors(11);

        boolean permit = circuitBreakManager.permit(mockServerWebExchange, currentTimeWindow, flowStat, service, path);
        Assertions.assertFalse(permit);
        Assertions.assertEquals(CircuitBreaker.State.OPEN, cb.stateRef.get());
        permit = circuitBreakManager.permit(mockServerWebExchange, currentTimeWindow, flowStat, service, path);
        Assertions.assertFalse(permit);
        Assertions.assertEquals(CircuitBreaker.State.OPEN, timeSlot.getCircuitBreakState().get());
        Assertions.assertEquals(2, timeSlot.getCircuitBreakNum());
    }
}
