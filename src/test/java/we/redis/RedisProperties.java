// package we.redis;
//
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Configuration;
// import we.util.JacksonUtils;
//
// /**
//  * @author hongqiaowei
//  */
//
// @Configuration
// public class RedisProperties {
//
//     private int redisPort;
//     private String redisHost;
//     private int database;
//
//     public RedisProperties(
//             /*@Value("${spring.redis.port}") int redisPort,
//             @Value("${spring.redis.host}") String redisHost,
//             @Value("${spring.redis.database}") int database*/) {
//         // this.redisPort = redisPort;
//         // this.redisHost = redisHost;
//         // this.database = database;
//         this.redisPort = 6379;
//         this.redisHost = "localhost";
//         this.database = 3;
//     }
//
//     public int getRedisPort() {
//         return redisPort;
//     }
//
//     public void setRedisPort(int redisPort) {
//         this.redisPort = redisPort;
//     }
//
//     public String getRedisHost() {
//         return redisHost;
//     }
//
//     public void setRedisHost(String redisHost) {
//         this.redisHost = redisHost;
//     }
//
//     public int getDatabase() {
//         return database;
//     }
//
//     public void setDatabase(int database) {
//         this.database = database;
//     }
//
//     @Override
//     public String toString() {
//         return JacksonUtils.writeValueAsString(this);
//     }
// }
