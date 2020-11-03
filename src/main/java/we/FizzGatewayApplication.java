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

//import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
//import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
//import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import shell.service.InstallService;

import java.io.File;
//import org.springframework.context.annotation.ComponentScan;


/**
 * @author lancer
 * @author francis
 */

public class FizzGatewayApplication {

    public static ConfigurableApplicationContext appContext;
    public static String[] globalArgs;
    public static void main(String[] args) {
        globalArgs = args;
        if (InstallService.shouldInstall() || System.getProperty("install") != null ){
            SpringApplication.run(FizzInstallApplication.class, args);
        } else {
            String configPath = InstallService.getConfigPath();
            ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(FizzMainApplication.class)
                    .properties("spring.config.location="+InstallService.getConfigPath())
                    .build().run(globalArgs);
        }
    }

    public static void restart(ConfigurableApplicationContext context) {
        if (!InstallService.shouldInstall()){
            Thread thread = new Thread(() -> {
                if (context != null)
                    context.close();
                    ConfigurableApplicationContext applicationContext = new SpringApplicationBuilder(FizzMainApplication.class)
                            .properties("spring.config.location="+InstallService.getConfigPath())
                            .build().run(globalArgs);
                    appContext = applicationContext;

            });
            thread.setDaemon(false);
            thread.start();
        } else {
            System.out.println("can't find application.yml under the user.home");
        }
    }
}
