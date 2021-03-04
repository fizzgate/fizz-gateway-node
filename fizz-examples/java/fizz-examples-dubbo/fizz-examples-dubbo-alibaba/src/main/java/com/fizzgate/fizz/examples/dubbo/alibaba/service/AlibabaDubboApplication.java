package com.fizzgate.fizz.examples.dubbo.alibaba.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * AlibabaDubboApplication.
 *
 * @author linwaiwai
 */
@SpringBootApplication
@ImportResource({"classpath:spring-dubbo.xml"})
public class AlibabaDubboApplication {
    public static void main(final String[] args) {
        SpringApplication.run(AlibabaDubboApplication.class, args);
    }

}
