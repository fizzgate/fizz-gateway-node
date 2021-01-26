package we.plugin.auth;

import org.junit.jupiter.api.Test;
import we.util.JacksonUtils;

import java.util.Arrays;

/**
 * @author hongqiaowei
 */

public class ApiConifg2appsServiceTests {

    @Test
    void test() {
        ApiConfig2apps apiConfig2apps = new ApiConfig2apps();
        apiConfig2apps.id = 0;
        apiConfig2apps.isDeleted = 0;
        apiConfig2apps.apps = Arrays.asList("app0", "app1");
        String apiConfig2appsJson = JacksonUtils.writeValueAsString(apiConfig2apps);
        // System.err.println(apiConfig2appsJson);
    }
}
