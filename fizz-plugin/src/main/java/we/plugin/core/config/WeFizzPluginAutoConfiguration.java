package we.plugin.core.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import we.plugin.core.filter.config.parser.JsonParser;
import we.plugin.core.spring.FizzPluginAliasProcessor;

/**
 * @author huanghua
 */
@Configuration
@ComponentScan({"we.config", "we.fizz", "we.plugin", "we.filter", "we.proxy", "we.stats"/*, "we.plugin.core"*/})
public class WeFizzPluginAutoConfiguration {

    @Bean
    public FizzPluginAliasProcessor fizzPluginAliasProcess(ApplicationContext context) {
        return new FizzPluginAliasProcessor(context);
    }

    @Bean
    public JsonParser jsonParser() {
        return new JsonParser();
    }
}
