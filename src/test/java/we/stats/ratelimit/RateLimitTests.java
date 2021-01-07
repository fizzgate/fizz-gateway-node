/*
 *  Copyright (C) 2021 the original author or authors.
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

package we.stats.ratelimit;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import we.stats.FlowStat;
import we.stats.ResourceTimeWindowStat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author hongqiaowei
 */

public class RateLimitTests {

	private FlowStat stat = new FlowStat();

	private ConnectionProvider getConnectionProvider() {
		return ConnectionProvider
				.builder("flow-control-cp")
				.maxConnections(100)
				.pendingAcquireTimeout(Duration.ofMillis(6_000))
				.maxIdleTime(Duration.ofMillis(40_000))
				.build();
	}

	private LoopResources getLoopResources() {
		LoopResources lr = LoopResources.create("flow-control-el", Runtime.getRuntime().availableProcessors(), true);
		lr.onServer(false);
		return lr;
	}

	private ReactorResourceFactory reactorResourceFactory() {
		ReactorResourceFactory fact = new ReactorResourceFactory();
		fact.setUseGlobalResources(false);
		fact.setConnectionProvider(getConnectionProvider());
		fact.setLoopResources(getLoopResources());
		fact.afterPropertiesSet();
		return fact;
	}

	private WebClient getWebClient() {
		ConnectionProvider cp = getConnectionProvider();
		LoopResources lr = getLoopResources();
		HttpClient httpClient = HttpClient.create(cp).compress(false).tcpConfiguration(
				tcpClient -> {
					return tcpClient.runOn(lr, false)
							.doOnConnected(
									connection -> {
										connection.addHandlerLast(new ReadTimeoutHandler(  20_000, TimeUnit.MILLISECONDS))
												  .addHandlerLast(new WriteTimeoutHandler( 20_000, TimeUnit.MILLISECONDS));
									}
							)
							.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 20_000)
							.option(ChannelOption.TCP_NODELAY,            true)
							.option(ChannelOption.SO_KEEPALIVE,           true)
							.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
				}
		);
		return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder().codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)).build())
				.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}

	@Test
	public void flowControlTests() {
		WebClient webClient = getWebClient();
		for (int i = 0; i < 10; i++) {
			webClient
					.method(HttpMethod.GET)
					.uri("")
					.headers(hdrs -> {})
					.body(Mono.just(""), String.class)
					.exchange().name("")
					.doOnRequest(l -> {})
					.doOnSuccess(r -> {})
					.doOnError(t -> {
						t.printStackTrace();
					})
					.timeout(Duration.ofMillis(6_000))
					.flatMap(
							remoteResp -> {
								remoteResp.bodyToMono(String.class)
										  .doOnSuccess(
												  s -> {
													  System.out.println(s);
												  }
										  );
								return Mono.empty();
							}
					);
		}
	}
}
