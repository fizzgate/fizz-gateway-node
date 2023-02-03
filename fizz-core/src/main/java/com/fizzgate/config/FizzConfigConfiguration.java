/*
 *  Copyright (C) 2021 the original author or authors.
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

package com.fizzgate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fizzgate.beans.factory.config.FizzBeanFactoryPostProcessor;
import com.fizzgate.context.event.FizzRefreshEventListener;
import com.fizzgate.context.scope.refresh.FizzRefreshScope;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Configuration(proxyBeanMethods = false)
public class FizzConfigConfiguration {

    public static final String              PROPERTY_SOURCE    = "FizzPropertySource";

    public static final String              REFRESH_SCOPE_NAME = "FizzRefresh";

    /*public static final Map<String, Object> DEFAULT_CONFIG_MAP = new HashMap<>();

    static {
        DEFAULT_CONFIG_MAP.put("server.port", 8600);
        DEFAULT_CONFIG_MAP.put("gateway.prefix", "/proxy");
        DEFAULT_CONFIG_MAP.put("server.fileUpload.maxDiskUsagePerPart", 104857600);
        DEFAULT_CONFIG_MAP.put("cors", true);

        DEFAULT_CONFIG_MAP.put("fizz.error.response.http-status.enable", true);
        DEFAULT_CONFIG_MAP.put("fizz.error.response.code-field", "msgCode");
        DEFAULT_CONFIG_MAP.put("fizz.error.response.message-field", "message");

        DEFAULT_CONFIG_MAP.put("fizz-trace-id.header", "X-Trace-Id");
        DEFAULT_CONFIG_MAP.put("fizz-trace-id.value-strategy", "requestId");
        DEFAULT_CONFIG_MAP.put("fizz-trace-id.value-prefix", "fizz");

        DEFAULT_CONFIG_MAP.put("custom.header.appid", "fizz-appid");
        DEFAULT_CONFIG_MAP.put("custom.header.sign", "fizz-sign");
        DEFAULT_CONFIG_MAP.put("custom.header.ts", "fizz-ts");

        DEFAULT_CONFIG_MAP.put("proxy-webclient.chTcpNodelay", true);
        DEFAULT_CONFIG_MAP.put("proxy-webclient.chSoKeepAlive", true);
        DEFAULT_CONFIG_MAP.put("proxy-webclient.chConnTimeout", 60000);
        DEFAULT_CONFIG_MAP.put("proxy-webclient.connReadTimeout", 60000);
        DEFAULT_CONFIG_MAP.put("proxy-webclient.connWriteTimeout", 60000);
        DEFAULT_CONFIG_MAP.put("proxy-webclient.compress", true);
        DEFAULT_CONFIG_MAP.put("proxy-webclient.trustInsecureSSL", true);

        DEFAULT_CONFIG_MAP.put("stat.open", true);
        DEFAULT_CONFIG_MAP.put("send-log.open", true);
        DEFAULT_CONFIG_MAP.put("log.headers", "COOKIE,FIZZ-APPID,FIZZ-SIGN,FIZZ-TS,FIZZ-RSV,HOST");

        DEFAULT_CONFIG_MAP.put("fizz.aggregate.writeMapNullValue", false);
        DEFAULT_CONFIG_MAP.put("gateway.aggr.proxy_set_headers", "X-Real-IP,X-Forwarded-Proto,X-Forwarded-For");
        DEFAULT_CONFIG_MAP.put("fizz-dubbo-client.address", "zookeeper://127.0.0.1:2181");

        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.server.enable", true);
        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.client.enable", true);
        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.client.port", 8601);
        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.client.request.timeliness", 300);
        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.client.request.timeout", 0);
        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.client.request.retry-count", 0);
        DEFAULT_CONFIG_MAP.put("fizz.dedicated-line.client.request.retry-interval", 0);
    }*/

    @Bean
    public FizzBeanFactoryPostProcessor fizzBeanFactoryPostProcessor() {
        return new FizzBeanFactoryPostProcessor();
    }

    /*@Bean
    public FizzBeanPostProcessor fizzBeanPostProcessor() {
        return new FizzBeanPostProcessor();
    }*/

    @Bean
    public static FizzRefreshScope fizzRefreshScope() {
        return new FizzRefreshScope();
    }

    @Bean
    public FizzRefreshEventListener fizzRefreshEventListener(FizzBeanFactoryPostProcessor fizzBeanFactoryPostProcessor, FizzRefreshScope scope) {
        return new FizzRefreshEventListener(fizzBeanFactoryPostProcessor, scope);
    }
}
