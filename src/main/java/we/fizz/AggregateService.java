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

package we.fizz;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import we.constants.CommonConstants;
import we.fizz.input.Input;
import we.flume.clients.log4j2appender.LogService;
import we.util.MapUtil;
import we.util.Utils;
import we.util.WebUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hongqiaowei
 */

@Service
public class AggregateService {

	private static final Logger log = LoggerFactory.getLogger(AggregateService.class);

	@Resource
	private ConfigLoader aggregateResourceLoader;

	public Mono<AggregateResult> request(String traceId, String clientReqPathPrefix, String method, String service, String path, MultiValueMap<String, String> queryParams,
										 HttpHeaders headers, String body) {

		// long start = System.currentTimeMillis();
		// ServerHttpRequest request = exchange.getRequest();
		String pash = clientReqPathPrefix + service + path;
		// String method = request.getMethodValue();
		AggregateResource aggregateResource = aggregateResourceLoader.matchAggregateResource(method, pash);
		if (aggregateResource == null) {
			return Mono.error(Utils.runtimeExceptionWithoutStack("no aggregate resource: " + method + ' ' + pash));
		} else {
			Pipeline pipeline = aggregateResource.getPipeline();
			Input input = aggregateResource.getInput();
			Map<String, Object> hs = MapUtil.toHashMap(headers);
			// String traceId = WebUtils.getTraceId(exchange);
			LogService.setBizId(traceId);
			log.debug("matched aggregation api: {}", pash);
			Map<String, Object> clientInput = new HashMap<>();
			clientInput.put("path", pash);
			clientInput.put("method", method);
			clientInput.put("headers", hs);
			// MultiValueMap<String, String> queryParams = request.getQueryParams();
			if (queryParams != null) {
				clientInput.put("params", MapUtil.toHashMap(queryParams));
			}
			if (body != null) {
				clientInput.put("body", JSON.parse(body));
			}
			return pipeline.run(input, clientInput, traceId).subscribeOn(Schedulers.elastic());
		}
	}

	public Mono<AggregateResult> request(String traceId, String clientReqPathPrefix, String method, String service, String path, MultiValueMap<String, String> queryParams,
										 HttpHeaders headers, DataBuffer body) {
		String b = null;
		if (body != null) {
			b = body.toString(StandardCharsets.UTF_8);
		}
		return request(traceId, clientReqPathPrefix, method, service, path, queryParams, headers, b);
	}

	public Mono<? extends Void> genAggregateResponse(ServerWebExchange exchange, AggregateResult ar) {
		ServerHttpResponse clientResp = exchange.getResponse();
		String traceId = WebUtils.getTraceId(exchange);
		LogService.setBizId(traceId);
		String js = null;
		if(ar.getBody() instanceof String) {
			js = (String) ar.getBody();
		}else {
			js = JSON.toJSONString(ar.getBody());
		}
		log.debug("aggregate response body: {}", js);
		if (ar.getHeaders() != null && !ar.getHeaders().isEmpty()) {
			ar.getHeaders().remove("Content-Length");
			clientResp.getHeaders().addAll(ar.getHeaders());
		}
		if (!clientResp.getHeaders().containsKey("Content-Type")) {
			// defalut content-type
			clientResp.getHeaders().add("Content-Type", "application/json; charset=UTF-8");
		}
		List<String> headerTraceIds = clientResp.getHeaders().get(CommonConstants.HEADER_TRACE_ID);
		if (headerTraceIds == null || !headerTraceIds.contains(traceId)) {
			clientResp.getHeaders().add(CommonConstants.HEADER_TRACE_ID, traceId);
		}
		// long end = System.currentTimeMillis();
		// pipeline.getStepContext().addElapsedTime("总耗时", end - start);
		// log.info("ElapsedTimes={}", JSON.toJSONString(pipeline.getStepContext().getElapsedTimes()));
		return clientResp
				.writeWith(Flux.just(exchange.getResponse().bufferFactory().wrap(js.getBytes())));
	}

}
