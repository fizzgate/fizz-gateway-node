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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import javax.annotation.Resource;
import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author hongqiaowei
 */

public abstract class WebClientConfig {

    protected static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    private Long          connReadTimeout         = null; // 20_000

    private Long          connWriteTimeout        = null; // 20_000

    private Integer       chConnTimeout           = null; // 20_000;

//  private Long          responseTimeout         = null; // 20_000

    private Boolean       chTcpNodelay            = null; // true

    private Boolean       chSoKeepAlive           = null; // true

    private Boolean       compress                = null; // true

    private Boolean       trustInsecureSSL        = null; // false

    public Boolean getTrustInsecureSSL() {
        return trustInsecureSSL;
    }

    public void setTrustInsecureSSL(Boolean trustInsecureSSL) {
        this.trustInsecureSSL = trustInsecureSSL;
    }

    public Long getConnReadTimeout() {
        return connReadTimeout;
    }

    public void setConnReadTimeout(Long connReadTimeout) {
        this.connReadTimeout = connReadTimeout;
    }

    public Long getConnWriteTimeout() {
        return connWriteTimeout;
    }

    public void setConnWriteTimeout(Long connWriteTimeout) {
        this.connWriteTimeout = connWriteTimeout;
    }

    public Integer getChConnTimeout() {
        return chConnTimeout;
    }

    public void setChConnTimeout(Integer chConnTimeout) {
        this.chConnTimeout = chConnTimeout;
    }

    /*
    public Long getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(Long responseTimeout) {
        this.responseTimeout = responseTimeout;
    }
    */

    public Boolean isChTcpNodelay() {
        return chTcpNodelay;
    }

    public void setChTcpNodelay(Boolean chTcpNodelay) {
        this.chTcpNodelay = chTcpNodelay;
    }

    public Boolean isChSoKeepAlive() {
        return chSoKeepAlive;
    }

    public void setChSoKeepAlive(Boolean chSoKeepAlive) {
        this.chSoKeepAlive = chSoKeepAlive;
    }

    public Boolean isCompress() {
        return compress;
    }

    public void setCompress(Boolean compress) {
        this.compress = compress;
    }

    /*
    @Resource
    ReactorClientHttpConnector reactorClientHttpConnector;

    @Resource
    WebClient.Builder webClientBuilder;
    */

    @Resource
    WebClientBuilderConfig webClientBuilderConfig;

    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create()
                                          .tcpConfiguration(
                                              tcpClient -> {
                                                  TcpClient newTcpClient = tcpClient.doOnConnected(
                                                          connection -> {
                                                              if (connReadTimeout != null) {
                                                                  connection.addHandlerLast(new ReadTimeoutHandler(connReadTimeout,   TimeUnit.MILLISECONDS));
                                                              }
                                                              if (connWriteTimeout != null) {
                                                                  connection.addHandlerLast(new WriteTimeoutHandler(connWriteTimeout, TimeUnit.MILLISECONDS));
                                                              }
                                                          }
                                                  );
                                                  if (chConnTimeout != null) {
                                                      newTcpClient = newTcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, chConnTimeout);
                                                  }
                                                  if (chTcpNodelay != null) {
                                                      newTcpClient = newTcpClient.option(ChannelOption.TCP_NODELAY,            chTcpNodelay);
                                                  }
                                                  if (chSoKeepAlive != null) {
                                                      newTcpClient = newTcpClient.option(ChannelOption.SO_KEEPALIVE,           chSoKeepAlive);
                                                  }
                                                  return newTcpClient;
                                              }
                                          );

        if (compress != null) {
            httpClient = httpClient.compress(compress);
        }
        /*
        if (responseTimeout != null) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(responseTimeout));
        }
        */

        if (trustInsecureSSL != null && trustInsecureSSL) {
            try {
                SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                httpClient = httpClient.secure(spec -> spec.sslContext(sslContext));
                log.warn("disable SSL verification");
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
        }

        return webClientBuilderConfig.getBuilder()
                                     .exchangeStrategies(
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
                ", trustInsecureSSL=" + trustInsecureSSL +
                " }";
    }
}
