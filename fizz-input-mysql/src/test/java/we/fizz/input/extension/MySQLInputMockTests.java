package we.fizz.input.extension;

import we.fizz.input.extension.mysql.MySQLInput;
import we.fizz.input.extension.mysql.MySQLInputConfig;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class MySQLInputMockTests {

    @Test
    public void test() {

        Map<String ,Object> requestConfig = new HashMap<String, Object>();
        requestConfig.put("URL","r2dbcs:mysql://fizz-test:password@101.132.114.209:3306/fizz-test?ssl=false&sslmode=DISABLE");
        requestConfig.put("sql","SELECT * FROM user WHERE id = ?id");
        requestConfig.put("binds", "{\"id\":\"1\"}" );
        MySQLInputConfig inputConfig = new MySQLInputConfig(requestConfig);
        inputConfig.parse();
        MySQLInput input = new MySQLInput();
        input.setConfig(inputConfig);
        input.beforeRun(null);
        Mono<Map> mono = input.run();
        Map result = mono.block();
        System.out.print(result);
        List data = (List) result.get("data");
        String name = (String)((Map)data.get(0)).get("name");
        System.out.print(name);
        Assertions.assertNotEquals(name, null, "no name");
        Assertions.assertNotEquals(result, null, "no response");
    }
}
