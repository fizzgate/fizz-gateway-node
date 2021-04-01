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
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import reactor.netty.resources.LoopResources;

/**
 * @author hongqiaowei
 */

@Configuration
@ConfigurationProperties(prefix = "server")
@EnableConfigurationProperties(ServerProperties.class)
public class WebFluxConfig {

    private static final Logger log = LoggerFactory.getLogger(WebFluxConfig.class);

    // private ConnectionProvider getConnectionProvider() {
    //     String cpName = "fizz-cp";
    //     ConnectionProvider cp = ConnectionProvider.builder(cpName).maxConnections(10_000)
    //             .pendingAcquireTimeout(Duration.ofMillis(6_000)).maxIdleTime(Duration.ofMillis(40_000)).build();
    //     log.info(cpName + ' ' + cp);
    //     return cp;
    // }

    // private LoopResources getLoopResources() {
    //     String elPrefix = "fizz-el";
    //     LoopResources lr = LoopResources.create(elPrefix, 1, Runtime.getRuntime().availableProcessors(), true);
    //     lr.onServerSelect(false);
    //     lr.onServer(false);
    //     log.info("fizz-lr " + lr);
    //     return lr;
    // }

    // @Bean
    // public ReactorResourceFactory reactorResourceFactory() {
    //     ReactorResourceFactory fact = new ReactorResourceFactory();
    //     fact.setUseGlobalResources(false);
    //     // fact.setConnectionProvider(getConnectionProvider());
    //     fact.setLoopResources(getLoopResources());
    //     // fact.afterPropertiesSet();
    //     return fact;
    // }

    // public static EventLoopGroup acceptorGroup;
    // public static EventLoopGroup workerGroup;
    // static {
    //     if (SystemUtils.IS_OS_WINDOWS) {
    //         acceptorGroup = new NioEventLoopGroup(1, new DefaultLoopResources.EventLoopFactory(true, "fizz-acceptor", new AtomicLong(0)));
    //         workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new DefaultLoopResources.EventLoopFactory(true, "fizz-worker", new AtomicLong(0)));
    //     } else {
    //         // DefaultLoop defaultLoop = DefaultLoopNativeDetector.getInstance();
    //         // EventLoopGroup newEventLoopGroup = defaultLoop.newEventLoopGroup(
    //         //         selectCount,
    //         //         threadFactory(this, "select-" + defaultLoop.getName()));
    //         acceptorGroup = new EpollEventLoopGroup(1, new DefaultLoopResources.EventLoopFactory(true, "fizz-acceptor", new AtomicLong(0)));
    //         workerGroup = new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(), new DefaultLoopResources.EventLoopFactory(true, "fizz-worker", new AtomicLong(0)));
    //     }
    // }

    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory(ServerProperties serverProperties/*, ReactorResourceFactory reactorResourceFactory*/) {
        NettyReactiveWebServerFactory httpServerFactory = new NettyReactiveWebServerFactory();
        httpServerFactory.setResourceFactory(null);
        // httpServerFactory.setResourceFactory(reactorResourceFactory);
        LoopResources lr = LoopResources.create("fizz-el", 1, Runtime.getRuntime().availableProcessors(), true);
        httpServerFactory.addServerCustomizers(
                httpServer -> (
                        httpServer.tcpConfiguration(
                                tcpServer -> {
                                    return (
                                            tcpServer
                                                    // .runOn(workerGroup)
                                                    .runOn(lr, false)
                                                    // .runOn(lr)
                                                    // .selectorOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                                                    // .port(7777)
                                                    .bootstrap(
                                                            serverBootstrap -> (
                                                                    serverBootstrap
                                                                            // .group(parentGroup, childGroup)
                                                                            // .channel(NioServerSocketChannel.class)
                                                                            // .handler(new LoggingHandler(LogLevel.DEBUG))
                                                                            // .childHandler(new ChannelInitializer<SocketChannel>() {
                                                                            //     @Override
                                                                            //     public void initChannel(final SocketChannel socketChannel) {
                                                                            //         socketChannel.pipeline().addLast(new BufferingInboundHandler());
                                                                            //     }
                                                                            // })
                                                                            // .channel(NioServerSocketChannel.class)
                                                                            .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                                                                            // .option(ChannelOption.SO_BACKLOG, 8192)
                                                                            .childOption(ChannelOption.ALLOCATOR,    UnpooledByteBufAllocator.DEFAULT)
                                                                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                                                                            .childOption(ChannelOption.TCP_NODELAY,  true)
                                                            )
                                                    )
                                    );
                                }
                        )
                )
        );

        return httpServerFactory;
    }

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
