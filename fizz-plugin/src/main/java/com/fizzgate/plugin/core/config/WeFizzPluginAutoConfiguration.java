package com.fizzgate.plugin.core.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fizzgate.plugin.core.filter.config.parser.JsonParser;
import com.fizzgate.plugin.core.spring.FizzPluginAliasProcessor;

/**
 * @author huanghua
 */
@Configuration
@ComponentScan({"we.config", "we.fizz", "we.plugin", "we.filter", "we.proxy", "we.stats", "com.fizzgate.config", "com.fizzgate.fizz", "com.fizzgate.plugin", "com.fizzgate.filter", "com.fizzgate.proxy", "com.fizzgate.stats"/*, "com.fizzgate.plugin.core"*/})
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
