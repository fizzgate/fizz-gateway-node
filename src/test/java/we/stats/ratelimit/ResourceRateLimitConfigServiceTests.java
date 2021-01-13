// package we.stats.ratelimit;
//
// import org.junit.jupiter.api.AfterAll;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.context.annotation.PropertySource;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
// import redis.embedded.RedisServer;
// import we.redis.RedisProperties;
// import we.redis.RedisTestConfiguration;
//
// import javax.annotation.Resource;
//
// /**
//  * @author hongqiaowei
//  */
//
// // @SpringBootTest(classes = RedisTestConfiguration.class)
// // @ContextConfiguration(classes = RedisTestConfiguration.class)
// @PropertySource("classpath:/application.yml")
// @SpringJUnitConfig(classes = {RedisProperties.class, RedisTestConfiguration.class, RedisTestConfiguration.class})
// // @ActiveProfiles("unittest")
// public class ResourceRateLimitConfigServiceTests {
//
//     // private static RedisServer redisServer;
//     //
//     // @BeforeAll
//     // static void startRedis() {
//     //     redisServer = RedisServer.builder()
//     //             .port(6379)
//     //             .setting("maxmemory 32M")
//     //             .build();
//     //     redisServer.start();
//     // }
//     //
//     // @AfterAll
//     // static void stopRedis() {
//     //     redisServer.stop();
//     // }
//
//     @Resource
//     private RedisProperties redisProperties;
//
//     @Resource
//     private StringRedisTemplate stringRedisTemplate;
//
//     @Test
//     void initTest() {
//         System.err.println("redis: " + redisProperties);
//         System.err.println("stringRedisTemplate: " + stringRedisTemplate);
//         // 其实就是要构建个 context，里面有 redis、ReactiveStringRedisTemplate、ResourceRateLimitConfigService
//         // ResourceRateLimitConfigService 依赖 ReactiveStringRedisTemplate，然后能 PostConstruct
//     }
// }
