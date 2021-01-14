package we.redis;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * @author hongqiaowei
 */

@TestConfiguration
// @EnableRedisRepositories
public class RedisTemplateConfiguration {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            RedisProperties redisProperties) {
        LettuceConnectionFactory cf = new LettuceConnectionFactory(
                redisProperties.getHost(),
                redisProperties.getPort());
        cf.setDatabase(redisProperties.getDatabase());
        return cf;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}