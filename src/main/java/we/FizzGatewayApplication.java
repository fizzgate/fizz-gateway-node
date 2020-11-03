package we;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;
import shell.service.InstallService;
import java.io.File;


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
