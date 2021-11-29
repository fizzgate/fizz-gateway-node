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

package we.dedicatedline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.session.DefaultWebSessionManager;
import we.config.SystemConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@ConditionalOnProperty(name = SystemConfig.FIZZ_DEDICATED_LINE_CLIENT_ENABLE, havingValue = "true")
@Configuration
@AutoConfigureAfter({HttpHandlerAutoConfiguration.class})
public class DedicatedLineWebServer {

    private static final Logger log = LoggerFactory.getLogger(DedicatedLineWebServer.class);

    @Resource
    private ReactiveWebServerApplicationContext applicationContext;

    @Resource
    private HttpHandler                         httpHandler;

    private WebServer                           server;

    @Value("${fizz.dedicated-line.client.port:8601}")
    private int port = 8601;

    @PostConstruct
    public void start() {
        HttpWebHandlerAdapter adapter = (HttpWebHandlerAdapter) httpHandler;
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory(port);
                             server = factory.getWebServer(
                                                   new DedicatedLineHttpHandler(
                                                       applicationContext,
                                                       new DefaultWebSessionManager(),
                                                       adapter.getCodecConfigurer(),
                                                       adapter.getLocaleContextResolver(),
                                                       adapter.getForwardedHeaderTransformer()
                                                   )
                                      );
                             server.start();
        log.info("fizz dedicated line web server listen on {}", port);
        applicationContext.publishEvent(new DedicatedLineWebServerInitializedEvent(server, applicationContext));
    }

    @PreDestroy
    public void stop() {
        server.stop();
    }
}
