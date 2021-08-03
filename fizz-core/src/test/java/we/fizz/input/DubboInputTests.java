/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.fizz.input;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import we.fizz.group.DevTestGroup;
import we.fizz.input.extension.dubbo.DubboInput;
import we.fizz.input.extension.dubbo.DubboInputConfig;

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