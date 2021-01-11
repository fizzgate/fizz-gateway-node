package we.stats.ratelimit;

import org.junit.jupiter.api.Test;

/**
 * @author hongqiaowei
 */

public class ResourceRateLimitConfigServiceTests {

    @Test
    void initTest() {

        // 其实就是要构建个 context，里面有 redis、ReactiveStringRedisTemplate、ResourceRateLimitConfigService
        // ResourceRateLimitConfigService 依赖 ReactiveStringRedisTemplate，然后能 PostConstruct
    }
}
