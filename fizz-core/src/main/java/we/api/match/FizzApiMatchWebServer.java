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

package we.api.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
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

//@Configuration
//@AutoConfigureAfter({HttpHandlerAutoConfiguration.class})
public class FizzApiMatchWebServer {

    private static final Logger log = LoggerFactory.getLogger(FizzApiMatchWebServer.class);

    @Resource
    private HttpHandler httpHandler;

    private WebServer server;

    private int port = 8601;

    @PostConstruct
    public void start() {
        HttpWebHandlerAdapter adapter = (HttpWebHandlerAdapter) httpHandler;
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory(port);
                             server = factory.getWebServer(
                                                   new FizzApiMatchHttpHandler(
                                                       new DefaultWebSessionManager(),
                                                       adapter.getCodecConfigurer(),
                                                       adapter.getLocaleContextResolver(),
                                                       adapter.getForwardedHeaderTransformer()
                                                   )
                                      );
                             server.start();
        log.info("fizz api match web server listen on {}", port);
    }

    @PreDestroy
    public void stop() {
        server.stop();
    }
}
