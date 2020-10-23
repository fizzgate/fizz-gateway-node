package we;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
    exclude = {ErrorWebFluxAutoConfiguration.class, RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class},
    scanBasePackages = {"we"}
)
@EnableApolloConfig
@EnableDiscoveryClient
public class FizzGatewayApplication {

    public static void main(String[] args) {
        FizzAppContext.appContext = SpringApplication.run(FizzGatewayApplication.class, args);
    }
}
