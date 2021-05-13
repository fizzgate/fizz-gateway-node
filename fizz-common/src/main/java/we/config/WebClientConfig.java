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

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import we.util.JacksonUtils;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author hongqiaowei
 */

public abstract class WebClientConfig {

    protected static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    // private String        name;
    //
    // private int           maxConnections        = 2_000;
    //
    // private Duration      maxIdleTime           = Duration.ofMillis(40_000);
    //
    // private Duration      pendingAcquireTimeout = Duration.ofMillis(6_000);

    private long          connReadTimeout       = 20_000;

    private long          connWriteTimeout      = 20_000;

    private int           chConnTimeout         = 20_000;

    private boolean       chTcpNodelay          = true;

    private boolean       chSoKeepAlive         = true;

    private boolean       compress              = true;

    // public String getName() {
    //     return name;
    // }
    //
    // public void setName(String name) {
    //     this.name = name;
    // }
    //
    // public int getMaxConnections() {
    //     return maxConnections;
    // }
    //
    // public void setMaxConnections(int maxConnections) {
    //     this.maxConnections = maxConnections;
    // }
    //
    // public Duration getMaxIdleTime() {
    //     return maxIdleTime;
    // }
    //
    // public void setMaxIdleTime(long maxIdleTime) {
    //     this.maxIdleTime = Duration.ofMillis(maxIdleTime);
    // }
    //
    // public Duration getPendingAcquireTimeout() {
    //     return pendingAcquireTimeout;
    // }
    //
    // public void setPendingAcquireTimeout(long pendingAcquireTimeout) {
    //     this.pendingAcquireTimeout = Duration.ofMillis(pendingAcquireTimeout);
    // }

    public long getConnReadTimeout() {
        return connReadTimeout;
    }

    public void setConnReadTimeout(long connReadTimeout) {
        this.connReadTimeout = connReadTimeout;
    }

    public long getConnWriteTimeout() {
        return connWriteTimeout;
    }

    public void setConnWriteTimeout(long connWriteTimeout) {
        this.connWriteTimeout = connWriteTimeout;
    }

    public int getChConnTimeout() {
        return chConnTimeout;
    }

    public void setChConnTimeout(int chConnTimeout) {
        this.chConnTimeout = chConnTimeout;
    }

    public boolean isChTcpNodelay() {
        return chTcpNodelay;
    }

    public void setChTcpNodelay(boolean chTcpNodelay) {
        this.chTcpNodelay = chTcpNodelay;
    }

    public boolean isChSoKeepAlive() {
        return chSoKeepAlive;
    }

    public void setChSoKeepAlive(boolean chSoKeepAlive) {
        this.chSoKeepAlive = chSoKeepAlive;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    @Resource
    ReactorResourceFactory reactorResourceFactory;

    // @Resource
    // ReactorClientHttpConnector reactorClientHttpConnector;

    public WebClient webClient() {
        ConnectionProvider cp = reactorResourceFactory.getConnectionProvider();
        LoopResources lrs = reactorResourceFactory.getLoopResources();
        HttpClient httpClient = HttpClient.create(cp).compress(compress).tcpConfiguration(
                tcpClient -> {
                    return tcpClient.runOn(lrs)
                                    // .bootstrap(
                                    //         bootstrap -> (
                                    //                 bootstrap.channel(NioSocketChannel.class)
                                    //         )
                                    // )
                                    .doOnConnected(
                                            connection -> {
                                                connection.addHandlerLast(new ReadTimeoutHandler( connReadTimeout,   TimeUnit.MILLISECONDS))
                                                          .addHandlerLast(new WriteTimeoutHandler(connWriteTimeout,  TimeUnit.MILLISECONDS));
                                            }
                                    )
                                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, chConnTimeout)
                                    .option(ChannelOption.TCP_NODELAY,            chTcpNodelay)
                                    .option(ChannelOption.SO_KEEPALIVE,           chSoKeepAlive)
                                    // .option(ChannelOption.ALLOCATOR,              PreferHeapByteBufAllocator.DEFAULT)
                                    // .option(ChannelOption.ALLOCATOR,              PooledByteBufAllocator.DEFAULT)
                                    // .option(ChannelOption.ALLOCATOR,              UnpooledByteBufAllocator.DEFAULT)
                                    ;
                }
        );
        return WebClient.builder().exchangeStrategies(
                                      ExchangeStrategies.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1))
                                                                  .build()
                                  )
                                  .clientConnector(new ReactorClientHttpConnector(httpClient))
                                  .build();
    }

    @Override
    public String toString() {
        return  "{ connReadTimeout="  + connReadTimeout +
                ", connWriteTimeout=" + connWriteTimeout +
                ", chConnTimeout="    + chConnTimeout +
                ", chTcpNodelay="     + chTcpNodelay +
                ", chSoKeepAlive="    + chSoKeepAlive +
                ", compress="         + compress +
                " }";
    }
}
