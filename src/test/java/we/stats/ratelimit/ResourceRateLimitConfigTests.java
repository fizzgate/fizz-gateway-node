package we.stats.ratelimit;

import org.junit.jupiter.api.Test;
import we.util.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author hongqiaowei
 */

public class ResourceRateLimitConfigTests {

    @Test
    void test() {
        String resourceRateLimitConfigJson = "{\"concurrents\":1000,\"enable\":1,\"id\":1,\"isDeleted\":0,\"qps\":500,\"resource\":\"_global\",\"responseContent\":\"{\\\"msg\\\":\\\"rate limit, please try again\\\"}\",\"responseType\":\"application/json; charset=UTF-8\",\"type\":1}";
        ResourceRateLimitConfig resourceRateLimitConfig = JacksonUtils.readValue(resourceRateLimitConfigJson, ResourceRateLimitConfig.class);
        assertEquals("application/json; charset=UTF-8", resourceRateLimitConfig.responseType);
    }
}
