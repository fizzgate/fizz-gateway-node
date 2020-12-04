package we;

import com.alibaba.nacos.spring.context.annotation.config.NacosPropertySource;
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
@NacosPropertySource(dataId = "${nacos.config.data-id}", groupId = "${nacos.config.group}", autoRefreshed = true)
@EnableDiscoveryClient
public class FizzGatewayApplication {

    public static void main(String[] args) {
        FizzAppContext.appContext = SpringApplication.run(FizzGatewayApplication.class, args);
    }
}
