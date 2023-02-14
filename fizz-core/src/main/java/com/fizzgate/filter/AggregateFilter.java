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

package com.fizzgate.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fizzgate.config.SystemConfig;
import com.fizzgate.constants.CommonConstants;
import com.fizzgate.fizz.AggregateResource;
import com.fizzgate.fizz.AggregateResult;
import com.fizzgate.fizz.ConfigLoader;
import com.fizzgate.fizz.Pipeline;
import com.fizzgate.fizz.input.Input;
import com.fizzgate.plugin.auth.ApiConfig;
import com.fizzgate.util.Consts;
import com.fizzgate.util.MapUtil;
import com.fizzgate.util.NettyDataBufferUtils;
import com.fizzgate.util.WebUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Francis Dong
 */
@Component
@Order(30)
public class AggregateFilter implements WebFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(AggregateFilter.class);
	
	private  static  final  String X_FORWARDED_FOR = "X-FORWARDED-FOR";

	@Resource
	private ConfigLoader configLoader;

	@Resource
	private AggregateFilterProperties aggregateFilterProperties;

	@Resource
	private SystemConfig systemConfig;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		String serviceId = WebUtils.getBackendService(exchange);
		if (serviceId == null) {
			return chain.filter(exchange);
		} else if (WebUtils.ignorePlugin(exchange) && WebUtils.getRoute(exchange).type == ApiConfig.Type.SERVICE_AGGREGATE) {
		} else {
			byte act = WebUtils.getApiConfigType(exchange);
			if (act == ApiConfig.Type.UNDEFINED) {
				String p = exchange.getRequest().getURI().getPath();
				if (StringUtils.startsWith(p, SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX0)) {
					if (systemConfig.isAggregateTestAuth()) {
						return chain.filter(exchange);
					}
				} else if (aggregateFilterProperties.isNeedAuth()) {
					return chain.filter(exchange);
				}
			} else if (act != ApiConfig.Type.SERVICE_AGGREGATE) {
				return chain.filter(exchange);
			}
		}

		FilterResult pfr = WebUtils.getPrevFilterResult(exchange);
		if (!pfr.success) {
			return WebUtils.getDirectResponse(exchange);
		}

		long start = System.currentTimeMillis();
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse serverHttpResponse = exchange.getResponse();

		String clientReqPathPrefix = WebUtils.getClientReqPathPrefix(exchange);
		String path = clientReqPathPrefix + serviceId + WebUtils.getBackendPath(exchange);
		String method = request.getMethodValue();
		if (HttpMethod.HEAD.matches(method.toUpperCase())) {
			method = HttpMethod.GET.name();
		}
		AggregateResource aggregateResource = configLoader.matchAggregateResource(method, path);
		if (aggregateResource == null) {
			if (SystemConfig.DEFAULT_GATEWAY_TEST_PREFIX0.equals(clientReqPathPrefix) || 
					WebUtils.getApiConfigType(exchange) == ApiConfig.Type.SERVICE_AGGREGATE) {
				return WebUtils.responseError(exchange, HttpStatus.NOT_FOUND.value(), "API not found in aggregation: " + path);
			} else {
				return chain.filter(exchange);
			}
		}

		Pipeline pipeline = aggregateResource.getPipeline();
		Input input = aggregateResource.getInput();

		HttpHeaders hds = request.getHeaders();
		Map<String, Object> headers = MapUtil.headerToHashMap(hds);
		if (CollectionUtils.isEmpty(hds.get(X_FORWARDED_FOR)) && systemConfig.isFizzWebClientXForwardedForEnable()) {
			headers.put(X_FORWARDED_FOR, WebUtils.getOriginIp(exchange));
        }
		Map<String, Object> fizzHeaders = (Map<String, Object>) exchange.getAttributes().get(WebUtils.APPEND_HEADERS);
		if (fizzHeaders != null && !fizzHeaders.isEmpty()) {
			Set<Entry<String, Object>> entrys = fizzHeaders.entrySet();
			for (Entry<String, Object> entry : entrys) {
				headers.put(entry.getKey().toUpperCase(), entry.getValue());
			}
		}

		// traceId
		final String traceId = WebUtils.getTraceId(exchange);
		// LogService.setBizId(traceId);
		ThreadContext.put(Consts.TRACE_ID, traceId);
		
		LOGGER.debug("{} matched api in aggregation: {}", traceId, path);
		
		// 客户端提交上来的信息
		Map<String, Object> clientInput = new HashMap<>();
		clientInput.put("path", path);
		clientInput.put("method", method);
		clientInput.put("headers", headers);
		clientInput.put("params", MapUtil.toHashMap(request.getQueryParams()));
		clientInput.put("contentType", request.getHeaders().getFirst(CommonConstants.HEADER_CONTENT_TYPE));

		Mono<AggregateResult> result = null;
		MediaType contentType = request.getHeaders().getContentType();
		
		if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {
			result = exchange.getMultipartData().flatMap(md -> {
				Map<String, FilePart> filePartMap = new HashMap<>();
				clientInput.put("body", MapUtil.extractFormData(md, CommonConstants.FILE_KEY_PREFIX, filePartMap));
				clientInput.put("filePartMap", filePartMap);
				return pipeline.run(input, clientInput, traceId);
			});
		} else if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)) {
			result = exchange.getFormData().flatMap(fd -> {
				clientInput.put("body", MapUtil.toHashMap(fd));
				return pipeline.run(input, clientInput, traceId);
			});
		} else {
			if (HttpMethod.POST.name().equalsIgnoreCase(method)) {
				result = DataBufferUtils.join(request.getBody()).defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER).flatMap(buf -> {
					if (buf != NettyDataBufferUtils.EMPTY_DATA_BUFFER) {
						try {
							clientInput.put("body", buf.toString(StandardCharsets.UTF_8));
						} finally {
							DataBufferUtils.release(buf);
						}
					}
					return pipeline.run(input, clientInput, traceId);
				});
			} else {
				result = pipeline.run(input, clientInput, traceId);
			}
		}
		return result.subscribeOn(Schedulers.elastic()).flatMap(aggResult -> {
			// LogService.setBizId(traceId);
			ThreadContext.put(Consts.TRACE_ID, traceId);
			if (aggResult.getHttpStatus() != null) {
				serverHttpResponse.setRawStatusCode(aggResult.getHttpStatus());
			}
			String jsonString = null;
			if (aggResult.getBody() instanceof String) {
				jsonString = (String) aggResult.getBody();
			} else {
				if (this.aggregateFilterProperties.isWriteMapNullValue()) {
					jsonString = JSON.toJSONString(aggResult.getBody(), SerializerFeature.WriteMapNullValue);
				} else {
					jsonString = JSON.toJSONString(aggResult.getBody());
				}
			}
			LOGGER.debug("{} response body: {}", traceId, jsonString);
			if (aggResult.getHeaders() != null && !aggResult.getHeaders().isEmpty()) {
				serverHttpResponse.getHeaders().addAll(aggResult.getHeaders());
				serverHttpResponse.getHeaders().remove(CommonConstants.HEADER_CONTENT_LENGTH);
			}
			if (!serverHttpResponse.getHeaders().containsKey(CommonConstants.HEADER_CONTENT_TYPE)) {
				// default content-type
				serverHttpResponse.getHeaders().add(CommonConstants.HEADER_CONTENT_TYPE, CommonConstants.CONTENT_TYPE_JSON);
			}
			List<String> headerTraceIds = serverHttpResponse.getHeaders().get(systemConfig.fizzTraceIdHeader());
			if (headerTraceIds == null || !headerTraceIds.contains(traceId)) {
				serverHttpResponse.getHeaders().add(systemConfig.fizzTraceIdHeader(), traceId);
			}

			long end = System.currentTimeMillis();
			pipeline.getStepContext().addElapsedTime("总耗时", end - start);
			LOGGER.info("{} ElapsedTimes={}", traceId, JSON.toJSONString(pipeline.getStepContext().getElapsedTimes()));

			return serverHttpResponse
					.writeWith(Flux.just(exchange.getResponse().bufferFactory().wrap(jsonString.getBytes())));
		});

	}

}