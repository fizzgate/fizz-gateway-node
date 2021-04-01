package we.config;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Apollo config
 * @author zhongjie
 */
@ConditionalOnProperty(name = "apollo.enabled")
@EnableApolloConfig
@Configuration
public class ApolloConfig {
}
