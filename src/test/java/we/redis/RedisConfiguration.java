// package we.redis;
//
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
// import org.springframework.data.redis.core.StringRedisTemplate;
// import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
//
// /**
//  * @author hongqiaowei
//  */
//
// @Configuration
// @EnableRedisRepositories
// public class RedisConfiguration {
//
//     @Bean
//     public LettuceConnectionFactory redisConnectionFactory(
//             RedisProperties redisProperties) {
//         LettuceConnectionFactory cf = new LettuceConnectionFactory(
//                 redisProperties.getRedisHost(),
//                 redisProperties.getRedisPort());
//         cf.setDatabase(redisProperties.getDatabase());
//         return cf;
//     }
//
//     @Bean
//     public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
//         StringRedisTemplate template = new StringRedisTemplate();
//         template.setConnectionFactory(connectionFactory);
//         return template;
//     }
// }