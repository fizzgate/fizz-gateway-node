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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author hongqiaowei
 */

@Configuration
@ConfigurationProperties(prefix = ProxyWebClientConfig.prefix)
public class ProxyWebClientConfig extends WebClientConfig {

    protected static final String prefix         = "proxy-webclient";

    public    static final String proxyWebClient = "proxyWebClient";
    public    static final String proxyWebClientBuilder = "proxyWebClientBuilder";

    @Bean(proxyWebClientBuilder)
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean(proxyWebClient)
    public WebClient webClient(@Qualifier("proxyWebClientBuilder") WebClient.Builder builder) {
        log.info(proxyWebClient + ": " + this);
        return super.webClient(builder);
    }
}
