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
import org.springframework.beans.factory.annotation.Value;
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

import com.alibaba.nacos.api.config.annotation.NacosValue;

/**
 * @author hongqiaowei
 */

@Configuration
@ConfigurationProperties(prefix = "server")
@EnableConfigurationProperties(ServerProperties.class)
public class WebFluxConfig {

    private static final Logger log = LoggerFactory.getLogger(WebFluxConfig.class);
    
    // @NacosValue(value = "${server.connection-pool.max-connections:500}", autoRefreshed = true)
    // @Value(             "${server.connection-pool.max-connections:500}"                      )
    // private int         maxConnections;
    //
    // @NacosValue(value = "${server.connection-pool.max-idle-time:30000}", autoRefreshed = true)
    // @Value(             "${server.connection-pool.max-idle-time:30000}"                      )
    // private long        maxIdleTime;

    // private ConnectionProvider connectionProvider() {
    //     ConnectionProvider connectionProvider = ConnectionProvider.builder("fizz-cp")
    //                                                               .maxConnections(maxConnections)
    //                                                               .maxIdleTime(Duration.ofMillis(maxIdleTime))
    //                                                               .pendingAcquireMaxCount(-1)
    //                                                               .build();
    //     log.info("server connection provider: maxConnections={}, maxIdleTime={}", maxConnections, maxIdleTime);
    //     return connectionProvider;
    // }

    // @ConditionalOnBean(ReactorResourceFactory.class)
    // @Bean(name = "$dummyObject")
    // public Object dummyObject() {
    //     ConnectionProvider provider = connectionProvider();
    //     HttpResources.set(provider);
    //     log.info("replace default connection provider with: " + provider);
    //     return new Object();
    // }

    // private LoopResources loopResources() {
    //     LoopResources loopResources = LoopResources.create("fizz-lrs");
    //     log.info("server loop resources: " + loopResources);
    //     return loopResources;
    // }

    // @Bean
    // public ReactorResourceFactory reactorServerResourceFactory() {
    //     ReactorResourceFactory rrf = new ReactorResourceFactory();
    //     rrf.setUseGlobalResources(false);
    //     rrf.setLoopResources(loopResources());
    //     rrf.setConnectionProvider(connectionProvider());
    //     log.info("server reactor resource factory: " + rrf);
    //     return rrf;
    // }

    // @Bean
    // public NettyReactiveWebServerFactory nettyReactiveWebServerFactory(ServerProperties serverProperties/*, ReactorResourceFactory reactorResourceFactory*/) {
    //     NettyReactiveWebServerFactory httpServerFactory = new NettyReactiveWebServerFactory();
    //     httpServerFactory.setResourceFactory(null);
    //     // httpServerFactory.setResourceFactory(reactorResourceFactory);
    //     // LoopResources lr = LoopResources.create("fizz-el", 1, Runtime.getRuntime().availableProcessors(), true);
    //     httpServerFactory.addServerCustomizers(
    //             httpServer -> (
    //                     httpServer.tcpConfiguration(
    //                             tcpServer -> {
    //                                 return (
    //                                         tcpServer
    //                                                 // .runOn(lr, true)
    //                                                 // .runOn(lr)
    //                                                 // .selectorOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
    //                                                 // .port(7777)
    //                                                 .bootstrap(
    //                                                         serverBootstrap -> (
    //                                                                 serverBootstrap
    //                                                                         // .group(parentGroup, childGroup)
    //                                                                         // .channel(NioServerSocketChannel.class)
    //                                                                         // .handler(new LoggingHandler(LogLevel.DEBUG))
    //                                                                         // .childHandler(new ChannelInitializer<SocketChannel>() {
    //                                                                         //     @Override
    //                                                                         //     public void initChannel(final SocketChannel socketChannel) {
    //                                                                         //         socketChannel.pipeline().addLast(new BufferingInboundHandler());
    //                                                                         //     }
    //                                                                         // })
    //                                                                         // .channel(NioServerSocketChannel.class)
    //                                                                         // .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
    //                                                                         // .option(ChannelOption.SO_BACKLOG, 8192)
    //                                                                         // .childOption(ChannelOption.ALLOCATOR,    UnpooledByteBufAllocator.DEFAULT)
    //                                                                         .childOption(ChannelOption.SO_KEEPALIVE, true)
    //                                                                         .childOption(ChannelOption.TCP_NODELAY,  true)
    //                                                         )
    //                                                 )
    //                                 );
    //                             }
    //                     )
    //             )
    //     );
    //
    //     return httpServerFactory;
    // }

    @Configuration
    @EnableWebFlux
    public static class FizzWebFluxConfigurer implements WebFluxConfigurer {

    	/**
         * Configure the maximum amount of disk space allowed for file parts. Default 100M (104857600) 
         */
        @NacosValue(value = "${server.fileUpload.maxDiskUsagePerPart:104857600}", autoRefreshed = true)
        @Value(             "${server.fileUpload.maxDiskUsagePerPart:104857600}"                      )
        private long maxDiskUsagePerPart;
        
        /**
         * Maximum parts of multipart form-data, including form field parts; Default -1 no limit
         */
        @NacosValue(value = "${server.fileUpload.maxParts:-1}", autoRefreshed = true)
        @Value(             "${server.fileUpload.maxParts:-1}"                      )
        private int maxParts;
        
        @Override
        public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
            configurer.defaultCodecs().maxInMemorySize(-1);
            SynchronossPartHttpMessageReader partReader = new SynchronossPartHttpMessageReader();
            partReader.setMaxParts(maxParts);
            partReader.setMaxDiskUsagePerPart(maxDiskUsagePerPart);
            MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(partReader);
            configurer.defaultCodecs().multipartReader(multipartReader);
        }
        
        
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/*.*")
                    .addResourceLocations("classpath:/static/");
        }
    }
}
