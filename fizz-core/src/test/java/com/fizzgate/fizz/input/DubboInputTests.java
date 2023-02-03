package com.fizzgate.fizz.input;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fizzgate.fizz.group.DevTestGroup;
import com.fizzgate.fizz.input.extension.dubbo.DubboInput;
import com.fizzgate.fizz.input.extension.dubbo.DubboInputConfig;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;


@ActiveProfiles("")
//@SpringBootTest
@Category(DevTestGroup.class)
class DubboInputTests {
    private static final String SERVICE_NAME = "com.fizzgate.fizz.examples.dubbo.common.service.UserService";
    private static final String METHOD_NAME = "findAll";
    //@Test
    public void test() {

        Map <String ,Object>requestConfig = new HashMap<String, Object>();
        requestConfig.put("serviceName",SERVICE_NAME);
        requestConfig.put("method",METHOD_NAME);
//        requestConfig.put("parameterTypes", "java.lang.String");

        DubboInputConfig inputConfig = new DubboInputConfig(requestConfig);
        inputConfig.parse();
        DubboInput input = new DubboInput();
        input.setConfig(inputConfig);
        input.beforeRun(null);
        Mono<Map>mono = input.run();
        Map result = mono.block();
        System.out.print(result);
        Assertions.assertNotEquals(result, null, "no response");

    }

}