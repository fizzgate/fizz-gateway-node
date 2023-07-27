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

package com.fizzgate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.netty.http.server.HttpRequestDecoderSpec;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@Configuration
@ConfigurationProperties(prefix = "server")
@EnableConfigurationProperties(ServerProperties.class)
public class WebServerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerConfig.class);
    
    @Configuration
    @EnableWebFlux
    public static class FizzWebFluxConfigurer implements WebFluxConfigurer {

        @Resource
        private WebServerConfigProperties webServerConfigProperties;
        
        @Override
        public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
            ServerCodecConfigurer.ServerDefaultCodecs serverDefaultCodecs = configurer.defaultCodecs();
            serverDefaultCodecs.maxInMemorySize(-1);

            SynchronossPartHttpMessageReader partReader = new SynchronossPartHttpMessageReader();
            partReader.setMaxParts(webServerConfigProperties.getMaxParts());
            partReader.setMaxDiskUsagePerPart(webServerConfigProperties.getMaxDiskUsagePerPart());
            MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(partReader);
            serverDefaultCodecs.multipartReader(multipartReader);
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/*.*")
                    .addResourceLocations("classpath:/static/");
        }
    }

    @Configuration
    public static class FizzWebServerFactoryCustomizer implements WebServerFactoryCustomizer<NettyReactiveWebServerFactory> {

        @Value("${fizz.client-request.max-initial-line-length:16384}")
        private int maxInitialLineLength;

        @Value("${fizz.client-request.max-header-size:8192}")
        private int maxHeaderSize;

        @Value("${fizz.client-request.max-chunk-size:8192}")
        private int maxChunkSize;

        @Override
        public void customize(NettyReactiveWebServerFactory factory) {
            factory.addServerCustomizers(
                    httpServer -> {
                        return httpServer.httpRequestDecoder(
                                spec -> {
                                    HttpRequestDecoderSpec decoderSpec = spec.maxInitialLineLength(maxInitialLineLength)
                                                                             .maxHeaderSize       (maxHeaderSize)
                                                                             .maxChunkSize        (maxChunkSize);
                                    LOGGER.info("set client request max initial line length to {}, max header size to {}, max chunk size to {}", maxInitialLineLength, maxHeaderSize, maxChunkSize);
                                    return decoderSpec;
                                }
                        );
                    }
            );
        }
    }

}
