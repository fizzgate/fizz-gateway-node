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

    @Test
    void resourceIdTest() {
        String resourceRateLimitConfigJson = "{\"concurrents\":1000,\"enable\":1,\"id\":1,\"isDeleted\":0,\"qps\":500,  \"type\":1,  \"resource\":\"_global\"  }";
        ResourceRateLimitConfig c = JacksonUtils.readValue(resourceRateLimitConfigJson, ResourceRateLimitConfig.class);
        String resourceId = c.getResourceId();
        assertEquals("@@_global@@", resourceId);

        String node = ResourceRateLimitConfig.getNode(resourceId);
        assertEquals("_global", node);

        resourceRateLimitConfigJson = "{\"concurrents\":1000,\"enable\":1,\"id\":1,\"isDeleted\":0,\"qps\":500,  \"type\":2,  \"resource\":\"service_default\"  }";
        c = JacksonUtils.readValue(resourceRateLimitConfigJson, ResourceRateLimitConfig.class);
        resourceId = c.getResourceId();
        assertEquals("@@@service_default@", resourceId);

        resourceRateLimitConfigJson = "{\"concurrents\":1000,\"enable\":1,\"id\":1,\"isDeleted\":0,\"qps\":500,  \"type\":3,  \"resource\":\"xservice\"  }";
        c = JacksonUtils.readValue(resourceRateLimitConfigJson, ResourceRateLimitConfig.class);
        resourceId = c.getResourceId();
        assertEquals("@@@xservice@", resourceId);

        resourceId = ResourceRateLimitConfig.buildResourceId(null, null, ResourceRateLimitConfig.NODE, null, null);
        assertEquals("@@_global@@", resourceId);

        resourceId = ResourceRateLimitConfig.buildResourceId(null, "192.168.1.1", null, "xservice", null);
        assertEquals("@192.168.1.1@@xservice@", resourceId);
    }
}
