// package we.redis;
//
// import org.springframework.context.annotation.Configuration;
// import redis.embedded.RedisServer;
//
// import javax.annotation.PostConstruct;
// import javax.annotation.PreDestroy;
//
// /**
//  * @author hongqiaowei
//  */
//
// @Configuration
// public class RedisTestConfiguration {
//
//     private RedisServer redisServer;
//
//     public RedisTestConfiguration(RedisProperties redisProperties) {
//         redisServer = RedisServer.builder()
//                 .port(redisProperties.getRedisPort())
//                 .setting("maxmemory 32M")
//                 .build();
//     }
//
//     @PostConstruct
//     public void postConstruct() {
//         redisServer.start();
//     }
//
//     @PreDestroy
//     public void preDestroy() {
//         redisServer.stop();
//     }
// }