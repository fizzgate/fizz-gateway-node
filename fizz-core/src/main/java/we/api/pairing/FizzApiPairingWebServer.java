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

package we.api.pairing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.session.DefaultWebSessionManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * @author hongqiaowei
 */

@ConditionalOnBean({ApiPairingInfoService.class})
@Configuration
@AutoConfigureAfter({HttpHandlerAutoConfiguration.class})
public class FizzApiPairingWebServer {

    private static final Logger log = LoggerFactory.getLogger(FizzApiPairingWebServer.class);

    @Resource
    private ReactiveWebServerApplicationContext applicationContext;

    @Resource
    private HttpHandler                         httpHandler;

    private WebServer                           server;

    @Value("${fizz.api.pairing.client.port:8601}")
    private int port = 8601;

    @PostConstruct
    public void start() {
        HttpWebHandlerAdapter adapter = (HttpWebHandlerAdapter) httpHandler;
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory(port);
                             server = factory.getWebServer(
                                                   new FizzApiPairingHttpHandler(
                                                       applicationContext,
                                                       new DefaultWebSessionManager(),
                                                       adapter.getCodecConfigurer(),
                                                       adapter.getLocaleContextResolver(),
                                                       adapter.getForwardedHeaderTransformer()
                                                   )
                                      );
                             server.start();
        log.info("fizz api pairing web server listen on {}", port);
        applicationContext.publishEvent(new FizzApiPairingWebServerInitializedEvent(server, applicationContext));
    }

    @PreDestroy
    public void stop() {
        server.stop();
    }
}
