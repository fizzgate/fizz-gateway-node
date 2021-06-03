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

import com.alibaba.nacos.api.config.annotation.NacosValue;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import org.springframework.web.reactive.resource.HttpResource;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.time.Duration;

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

        @Override
        public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
            configurer.defaultCodecs().maxInMemorySize(-1);
        }
        
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/*.*")
                    .addResourceLocations("classpath:/static/");
        }
    }
}
