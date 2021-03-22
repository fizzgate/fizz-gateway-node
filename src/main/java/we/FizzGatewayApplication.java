/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package we;

import com.alibaba.nacos.spring.context.annotation.config.NacosPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import we.config.AggregateRedisConfig;
import we.log.LogSendAppender;

/**
 * fizz gateway application boot entrance
 *
 * @author linwaiwai
 * @author Francis Dong
 * @author hongqiaowei
 * @author zhongjie
 */
@SpringBootApplication(
    exclude = {ErrorWebFluxAutoConfiguration.class, RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class},
    scanBasePackages = {"we"}
)
@NacosPropertySource(dataId = "${nacos.config.data-id}", groupId = "${nacos.config.group}", autoRefreshed = true)
@EnableDiscoveryClient
public class FizzGatewayApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(FizzGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(FizzGatewayApplication.class);
        springApplication.setApplicationContextClass(CustomReactiveWebServerApplicationContext.class);
        FizzAppContext.appContext = springApplication.run(args);
    }

    private static class CustomReactiveWebServerApplicationContext extends AnnotationConfigReactiveWebServerApplicationContext {
        @Override
        protected void onClose() {
            super.onClose();
            if (AggregateRedisConfig.proxyLettuceConnectionFactory != null) {
                LOGGER.info("FizzGatewayApplication stopped.");
                // set LogSendAppender.logEnabled to false to stop send log to fizz-manager
                LogSendAppender.logEnabled = Boolean.FALSE;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore
                }
                // the ProxyLettuceConnectionFactory remove DisposableBean interface, so invoke destroy method here
                AggregateRedisConfig.proxyLettuceConnectionFactory.destroy();
            }
        }
    }
}
