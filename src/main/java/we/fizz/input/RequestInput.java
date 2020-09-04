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

package we.fizz.input;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.fastjson.JSON;
import we.flume.clients.log4j2appender.LogService;
import we.FizzGatewayApplication;
import we.constants.CommonConstants;
import we.fizz.StepContext;
import we.fizz.StepResponse;
import we.proxy.FizzWebClient;
import we.util.MapUtil;

import reactor.core.publisher.Mono;

/**
 * 
 * @author linwaiwai
 * @author francis
 *
 */
@SuppressWarnings("unchecked")
public class RequestInput extends Input {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestInput.class);
	private InputType type;
	protected Map<String, Object> dataMapping;
	protected Map<String, Object> request = new HashMap<>();
	protected Map<String, Object> response = new HashMap<>();

	private static final String FALLBACK_MODE_STOP = "stop";
	private static final String FALLBACK_MODE_CONTINUE = "continue";

	public InputType getType() {
		return type;
	}

	public void setType(InputType typeEnum) {
		this.type = typeEnum;
	}

	public Map<String, Object> getDataMapping() {
		return dataMapping;
	}

	public void setDataMapping(Map<String, Object> dataMapping) {
		this.dataMapping = dataMapping;
	}

	private void doRequestMapping(InputConfig aConfig, InputContext inputContext) {
		RequestInputConfig config = (RequestInputConfig) aConfig;

		// 把请求信息放入stepContext
		Map<String, Object> group = new HashMap<>();
		group.put("request", request);
		group.put("response", response);
		this.stepResponse.getRequests().put(name, group);

		HttpMethod method = HttpMethod.valueOf(config.getMethod().toUpperCase());
		request.put("method", method);

		Map<String, Object> params = new HashMap<>();
		params.putAll(MapUtil.toHashMap(config.getQueryParams()));
		request.put("params", params);

		// 数据转换
		if (inputContext != null && inputContext.getStepContext() != null) {
			StepContext<String, Object> stepContext = inputContext.getStepContext();
			Map<String, Object> dataMapping = this.getConfig().getDataMapping();
			if (dataMapping != null) {
				Map<String, Object> requestMapping = (Map<String, Object>) dataMapping.get("request");
				if (requestMapping != null && !StringUtils.isEmpty(requestMapping)) {
					ONode ctxNode = PathMapping.toONode(stepContext);
					
					// headers
					request.put("headers",
							PathMapping.transform(ctxNode, stepContext,
									(Map<String, Object>) requestMapping.get("fixedHeaders"),
									(Map<String, Object>) requestMapping.get("headers")));

					// params
					params.putAll(PathMapping.transform(ctxNode, stepContext,
							(Map<String, Object>) requestMapping.get("fixedParams"),
							(Map<String, Object>) requestMapping.get("params")));
					request.put("params", params);

					// body
					Map<String,Object> body = PathMapping.transform(ctxNode, stepContext,
							(Map<String, Object>) requestMapping.get("fixedBody"),
							(Map<String, Object>) requestMapping.get("body"));
					

					// script
					if (requestMapping.get("script") != null) {
						Map<String, Object> scriptCfg = (Map<String, Object>) requestMapping.get("script");
						try {
							Object reqBody = ScriptHelper.execute(scriptCfg, ctxNode, stepContext);
							if (reqBody != null) {
								body.putAll((Map<String, Object>) reqBody);
							}
						} catch (ScriptException e) {
							LogService.setBizId(inputContext.getStepContext().getTraceId());
							LOGGER.warn("execute script failed, {}", e);
							throw new RuntimeException("execute script failed");
						}
					}
					request.put("body", body);
				}
			}
		}

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(config.getBaseUrl() + config.getPath())
				.queryParams(MapUtil.toMultiValueMap(params)).build();
		request.put("url", uriComponents.toUriString());
	}

	private void doResponseMapping(InputConfig aConfig, InputContext inputContext, String responseBody) {
		RequestInputConfig config = (RequestInputConfig) aConfig;
		response.put("body", JSON.parse(responseBody));
		// 数据转换
		if (inputContext != null && inputContext.getStepContext() != null) {
			StepContext<String, Object> stepContext = inputContext.getStepContext();
			Map<String, Object> dataMapping = this.getConfig().getDataMapping();
			if (dataMapping != null) {
				Map<String, Object> responseMapping = (Map<String, Object>) dataMapping.get("response");
				if (responseMapping != null && !StringUtils.isEmpty(responseMapping)) {
					ONode ctxNode = PathMapping.toONode(stepContext);
					
					// headers
					Map<String, Object> fixedHeaders = (Map<String, Object>) responseMapping.get("fixedHeaders");
					Map<String, Object> headerMapping = (Map<String, Object>) responseMapping.get("headers");
					if ((fixedHeaders != null && !fixedHeaders.isEmpty())
							|| (headerMapping != null && !headerMapping.isEmpty())) {
						Map<String, Object> headers = new HashMap<>();
						headers.putAll(PathMapping.transform(ctxNode, stepContext, fixedHeaders, headerMapping));
						response.put("headers", headers);
					}

					// body
					Map<String, Object> fixedBody = (Map<String, Object>) responseMapping.get("fixedBody");
					Map<String, Object> bodyMapping = (Map<String, Object>) responseMapping.get("body");
					Map<String, Object> scriptCfg = (Map<String, Object>) responseMapping.get("script");
					if ((fixedBody != null && !fixedBody.isEmpty()) || (bodyMapping != null && !bodyMapping.isEmpty())
							|| (scriptCfg != null && scriptCfg.get("type") != null
									&& scriptCfg.get("source") != null)) {
						// body
						Map<String, Object> body = new HashMap<>();
						body.putAll(PathMapping.transform(ctxNode, stepContext, fixedBody, bodyMapping));

						// script
						if (scriptCfg != null && scriptCfg.get("type") != null && scriptCfg.get("source") != null) {
							try {
								Object respBody = ScriptHelper.execute(scriptCfg, ctxNode, stepContext);
								if (respBody != null) {
									body.putAll((Map<String, Object>) respBody);
								}
							} catch (ScriptException e) {
								LogService.setBizId(inputContext.getStepContext().getTraceId());
								LOGGER.warn("execute script failed, {}", e);
								throw new RuntimeException("execute script failed");
							}
						}
						response.put("body", body);
					}
				}
			} else {
				response.put("body", JSON.parse(responseBody));
			}
		}
	}

	private Mono<ClientResponse> getClientSpecFromContext(InputConfig aConfig, InputContext inputContext) {
		RequestInputConfig config = (RequestInputConfig) aConfig;
		
		int timeout = config.getTimeout() < 1 ? 3000 : config.getTimeout() > 10000 ? 10000 : config.getTimeout();
		
		HttpMethod method = HttpMethod.valueOf(config.getMethod());
		String url = (String) request.get("url");

		Map<String, Object> headers = (Map<String, Object>) request.get("headers");
		if (headers == null) {
			headers = new HashMap<>();
		}
		if (!headers.containsKey("Content-Type")) {
			// defalut content-type
			headers.put("Content-Type", "application/json; charset=UTF-8");
		}
		headers.put(CommonConstants.HEADER_TRACE_ID, inputContext.getStepContext().getTraceId());
		
		HttpMethod aggrMethod = HttpMethod.valueOf(inputContext.getStepContext().getInputReqAttr("method").toString());
		String aggrPath = (String)inputContext.getStepContext().getInputReqAttr("path");
		String aggrService = aggrPath.split("\\/")[2];
		
		FizzWebClient client = FizzGatewayApplication.appContext.getBean(FizzWebClient.class);
		return client.aggrSend(aggrService, aggrMethod, aggrPath, null, method, url, 
				MapUtil.toHttpHeaders(headers), request.get("body"), (long)timeout);
	}

	private Map<String, Object> getResponses(Map<String, StepResponse> stepContext2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean needRun(StepContext<String, Object> stepContext) {
		Map<String, Object> condition = ((RequestInputConfig) config).getCondition();
		if (CollectionUtils.isEmpty(condition)) {
			// 没有配置condition，直接运行
			return Boolean.TRUE;
		}

		ONode ctxNode = PathMapping.toONode(stepContext);
		try {
			Boolean needRun = ScriptHelper.execute(condition, ctxNode, stepContext, Boolean.class);
			return needRun != null ? needRun : Boolean.TRUE;
		} catch (ScriptException e) {
			LogService.setBizId(inputContext.getStepContext().getTraceId());
			LOGGER.warn("execute script failed", e);
			throw new RuntimeException("execute script failed");
		}
	}

	@Override
	public Mono<Map> run() {
		long t1 = System.currentTimeMillis();
		this.doRequestMapping(config, inputContext);
		inputContext.getStepContext().addElapsedTime(stepResponse.getStepName() + "-" + this.name + "-RequestMapping",
				System.currentTimeMillis() - t1);

		String prefix = stepResponse.getStepName() + "-" + "调用接口";
		long start = System.currentTimeMillis();
		Mono<ClientResponse> clientResponse = this.getClientSpecFromContext(config, inputContext);
		Mono<String> body = clientResponse.flatMap(cr->{
			return Mono.just(cr).doOnError(throwable -> cleanup(cr));
		}).doOnSuccess(cr -> {
			long elapsedMillis = System.currentTimeMillis() - start;
			HttpHeaders httpHeaders = cr.headers().asHttpHeaders();
			Map<String, Object> headers = new HashMap<>();
			httpHeaders.forEach((key, value) -> {
				if (value.size() > 1) {
					headers.put(key, value);
				} else {
					headers.put(key, httpHeaders.getFirst(key));
				}
			});
			headers.put("elapsedTime", elapsedMillis + "ms");
			this.response.put("headers", headers);
			inputContext.getStepContext().addElapsedTime(prefix + request.get("url"),
					elapsedMillis);
		}).flatMap(cr -> cr.bodyToMono(String.class)).doOnSuccess(resp -> {
			long elapsedMillis = System.currentTimeMillis() - start;
			if(inputContext.getStepContext().isDebug()) {
				LogService.setBizId(inputContext.getStepContext().getTraceId());
				LOGGER.info("{} 耗时:{}ms URL={}, reqHeader={} req={} resp={}", prefix, elapsedMillis, request.get("url"), 
						JSON.toJSONString(this.request.get("headers")),
						JSON.toJSONString(this.request.get("body")), resp);
			}
		}).doOnError(ex -> {
			LogService.setBizId(inputContext.getStepContext().getTraceId());
			LOGGER.warn("failed to call {}", request.get("url"), ex);
			long elapsedMillis = System.currentTimeMillis() - start;
			inputContext.getStepContext().addElapsedTime(
					stepResponse.getStepName() + "-" + "调用接口 failed " + request.get("url"), elapsedMillis);
		});

		// fallback handler
		RequestInputConfig reqConfig = (RequestInputConfig) config;
		if (reqConfig.getFallback() != null) {
			Map<String, String> fallback = reqConfig.getFallback();
			String mode = fallback.get("mode");
			if (FALLBACK_MODE_STOP.equals(mode)) {
				body = body.onErrorStop();
			} else if (FALLBACK_MODE_CONTINUE.equals(mode)) {
				body = body.onErrorResume(ex -> {
					return Mono.just(fallback.get("defaultResult"));
				});
			} else {
				body = body.onErrorStop();
			}
		}

		return body.flatMap(item -> {
			Map<String, Object> result = new HashMap<String, Object>();
			result.put("data", item);
			result.put("request", this);

			long t3 = System.currentTimeMillis();
			this.doResponseMapping(config, inputContext, item);
			inputContext.getStepContext().addElapsedTime(
					stepResponse.getStepName() + "-" + this.name + "-ResponseMapping", System.currentTimeMillis() - t3);

			return Mono.just(result);
		});
	}
	
	private void cleanup(ClientResponse clientResponse) {
		if (clientResponse != null) {
			clientResponse.bodyToMono(Void.class).subscribe();
		}
	}

}
