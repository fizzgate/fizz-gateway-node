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

package we.filter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.alibaba.fastjson.JSON;

import io.netty.buffer.UnpooledByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import we.constants.CommonConstants;
import we.fizz.AggregateResource;
import we.fizz.AggregateResult;
import we.fizz.ConfigLoader;
import we.fizz.Pipeline;
import we.fizz.input.Input;
import we.flume.clients.log4j2appender.LogService;
import we.util.Constants;
import we.util.MapUtil;
import we.util.WebUtils;

/**
 * @author francis
 */
@Component
@Order(2)
public class FizzGatewayFilter implements WebFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(FizzGatewayFilter.class);

	private static final DataBuffer emptyBody = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false, true)).wrap(Constants.Symbol.EMPTY.getBytes());
	
	@Resource
	private ConfigLoader configLoader;
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		long start = System.currentTimeMillis();
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse serverHttpResponse = exchange.getResponse();
		
		if (WebUtils.getServiceId(exchange) == null) {
			return chain.filter(exchange);
		}
		
		String path = WebUtils.getPathPrefix(exchange) + WebUtils.getServiceId(exchange) + WebUtils.getReqPath(exchange);
		String method = request.getMethodValue();
		AggregateResource aggregateResource = configLoader.matchAggregateResource(method, path);
		if (aggregateResource == null) {
			return chain.filter(exchange);
		}

		Pipeline pipeline = aggregateResource.getPipeline();
		Input input = aggregateResource.getInput();

		Map<String, Object> headers = MapUtil.toHashMap(request.getHeaders());
		Map<String, Object> fizzHeaders = (Map<String, Object>) exchange.getAttributes().get(WebUtils.APPEND_HEADERS);
		if(fizzHeaders != null && !fizzHeaders.isEmpty()) {
			headers.putAll(fizzHeaders);
		}
		
		// traceId
		String traceId = exchange.getRequest().getId();
		LogService.setBizId(traceId);
		serverHttpResponse.getHeaders().add(CommonConstants.HEADER_TRACE_ID, traceId);
		
		// 客户端提交上来的信息
		Map<String, Object> clientInput = new HashMap<>();
		clientInput.put("path", path);
		clientInput.put("method", method);
		clientInput.put("headers", headers);
		clientInput.put("params", MapUtil.toHashMap(request.getQueryParams()));
		
		
		Mono<AggregateResult> result = null;
		if (HttpMethod.POST.name().equalsIgnoreCase(method)) {
			result = DataBufferUtils.join(request.getBody()).defaultIfEmpty(emptyBody).flatMap(buf -> {
				if(buf != null && buf != emptyBody) {
					try {
						clientInput.put("body", JSON.parse(buf.toString(StandardCharsets.UTF_8)));
					} finally {
						DataBufferUtils.release(buf);
					}
				}
				return pipeline.run(input, clientInput, traceId);
			});
		} else {
			result = pipeline.run(input, clientInput, traceId);
		}
		return result.subscribeOn(Schedulers.elastic()).flatMap(aggResult -> {
			LogService.setBizId(traceId);
			String jsonString = JSON.toJSONString(aggResult.getBody());
			if (aggResult.getHeaders() != null && !aggResult.getHeaders().isEmpty()) {
				aggResult.getHeaders().remove("Content-Length");
				serverHttpResponse.getHeaders().addAll(aggResult.getHeaders());
			}
			if (!serverHttpResponse.getHeaders().containsKey("Content-Type")) {
				// defalut content-type
				serverHttpResponse.getHeaders().add("Content-Type", "application/json; charset=UTF-8");
			}
			
			long end = System.currentTimeMillis();
			pipeline.getStepContext().addElapsedTime("总耗时", end - start);
			LOGGER.info("ElapsedTimes={}", JSON.toJSONString(pipeline.getStepContext().getElapsedTimes()));

			return serverHttpResponse
					.writeWith(Flux.just(exchange.getResponse().bufferFactory().wrap(jsonString.getBytes())));
		});

	}

}