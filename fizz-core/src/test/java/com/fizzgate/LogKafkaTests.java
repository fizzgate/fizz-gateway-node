package com.fizzgate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LogKafkaTests {

    // @Test
    void test() throws InterruptedException, IOException {
        System.setProperty("log4j2.isThreadContextMapInheritable", "true");

        /*ConfigurationSource source = new ConfigurationSource(new FileInputStream("D:\\idea\\projects\\fizz-gateway-community\\fizz-core\\src\\test\\resources\\log4j2-test.xml"));
        Configurator.initialize(null, source);
        Logger logger4es = LogManager.getLogger(LogKafkaTests.class);*/

        Logger logger4es = LoggerFactory.getLogger(LogKafkaTests.class);
        Logger logger4kf = LoggerFactory.getLogger("monitor");

        ThreadContext.put("traceId", "ti1");

        // MDC.put("traceId", "ti0");

        logger4es.warn("520");
        logger4kf.warn("521");
        Thread.currentThread().join();
    }
}
