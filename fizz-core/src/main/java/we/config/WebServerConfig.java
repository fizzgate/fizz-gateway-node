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

package we.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@Configuration
@ConfigurationProperties(prefix = "server")
@EnableConfigurationProperties(ServerProperties.class)
public class WebServerConfig {

    private static final Logger log = LoggerFactory.getLogger(WebServerConfig.class);
    
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
}
