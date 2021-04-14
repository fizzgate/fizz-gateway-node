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

package we.fizz.input.extension.request;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.script.ScriptException;

import org.noear.snack.ONode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.alibaba.fastjson.JSON;

import reactor.core.publisher.Mono;
import we.config.SystemConfig;
import we.constants.CommonConstants;
import we.exception.ExecuteScriptException;
import we.fizz.StepContext;
import we.fizz.StepResponse;
import we.fizz.input.*;
import we.flume.clients.log4j2appender.LogService;
import we.proxy.FizzWebClient;
import we.proxy.http.HttpInstanceService;
import we.util.JacksonUtils;
import we.util.MapUtil;

/**
 * 
 * @author linwaiwai
 * @author Francis Dong
 *
 */
@SuppressWarnings("unchecked")
public class RequestInput extends RPCInput implements IInput{

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestInput.class);
	static public InputType TYPE = new InputType("REQUEST");
	private InputType type;
	protected Map<String, Object> dataMapping;

	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String CONTENT_TYPE_XML = "application/xml";
	private static final String CONTENT_TYPE_JS = "application/javascript";
	private static final String CONTENT_TYPE_HTML = "text/html";
	private static final String CONTENT_TYPE_TEXT = "text/plain";

	private static final String CONTENT_TYPE = "content-type";
	
	private static final Integer SERVICE_TYPE_HTTP = 2;
	
	private String respContentType;

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

	protected void doRequestMapping(InputConfig aConfig, InputContext inputContext) {
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
					Map<String, Object> headers = PathMapping.transform(ctxNode, stepContext,
							MapUtil.upperCaseKey((Map<String, Object>) requestMapping.get("fixedHeaders")),
							MapUtil.upperCaseKey((Map<String, Object>) requestMapping.get("headers")), false);
					if (headers.containsKey(CommonConstants.WILDCARD_TILDE)
							&& headers.get(CommonConstants.WILDCARD_TILDE) instanceof Map) {
						request.put("headers", headers.get(CommonConstants.WILDCARD_TILDE));
					} else {
						request.put("headers", headers);
					}

					// params
					params.putAll(PathMapping.transform(ctxNode, stepContext,
							(Map<String, Object>) requestMapping.get("fixedParams"),
							(Map<String, Object>) requestMapping.get("params"), false));
					if (params.containsKey(CommonConstants.WILDCARD_TILDE)
							&& params.get(CommonConstants.WILDCARD_TILDE) instanceof Map) {
						request.put("params", params.get(CommonConstants.WILDCARD_TILDE));
					} else {
						request.put("params", params);
					}

					// body
					Map<String,Object> body = PathMapping.transform(ctxNode, stepContext,
							(Map<String, Object>) requestMapping.get("fixedBody"),
							(Map<String, Object>) requestMapping.get("body"));
					if (body.containsKey(CommonConstants.WILDCARD_TILDE)) {
						request.put("body", body.get(CommonConstants.WILDCARD_TILDE));
					} else {
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
								LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(scriptCfg), e);
								throw new ExecuteScriptException(e, stepContext, scriptCfg);
							}
						}
						request.put("body", body);
					}
				}
			}
		}
		
		if (config.isNewVersion()) {
			String host = config.getServiceName();
			if (SERVICE_TYPE_HTTP.equals(config.getServiceType().intValue())) {
				HttpInstanceService httpInstanceService = this.getCurrentApplicationContext()
						.getBean(HttpInstanceService.class);
				String instance = httpInstanceService.getInstance(config.getServiceName());
				if (instance != null) {
					host = instance;
				}
			}
			StringBuffer sb = new StringBuffer();
			sb.append(config.getProtocol()).append("://").append(host)
					.append(config.getPath().startsWith("/") ? "" : "/").append(config.getPath());

			UriComponents uriComponents = UriComponentsBuilder.fromUriString(sb.toString())
					.queryParams(MapUtil.toMultiValueMap(params)).build();

			request.put("url", uriComponents.toUriString());
		} else {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(config.getBaseUrl() + config.getPath())
					.queryParams(MapUtil.toMultiValueMap(params)).build();
			request.put("url", uriComponents.toUriString());
		}
	}

	@Override
	public void doResponseMapping(InputConfig aConfig, InputContext inputContext, Object responseBody) {

		RequestInputConfig config = (RequestInputConfig) aConfig;
		response.put("body", this.parseBody(this.respContentType, (String)responseBody));

		// 数据转换
		if (inputContext != null && inputContext.getStepContext() != null) {
			StepContext<String, Object> stepContext = inputContext.getStepContext();
			Map<String, Object> dataMapping = this.getConfig().getDataMapping();
			if (dataMapping != null) {
				Map<String, Object> responseMapping = (Map<String, Object>) dataMapping.get("response");
				if (responseMapping != null && !StringUtils.isEmpty(responseMapping)) {
					ONode ctxNode = PathMapping.toONode(stepContext);
					
					// headers
					Map<String, Object> fixedHeaders = MapUtil.upperCaseKey((Map<String, Object>) responseMapping.get("fixedHeaders"));
					Map<String, Object> headerMapping = MapUtil.upperCaseKey((Map<String, Object>) responseMapping.get("headers"));
					if ((fixedHeaders != null && !fixedHeaders.isEmpty())
							|| (headerMapping != null && !headerMapping.isEmpty())) {
						Map<String, Object> headers = new HashMap<>();
						headers.putAll(PathMapping.transform(ctxNode, stepContext, fixedHeaders, headerMapping, false));
						if (headers.containsKey(CommonConstants.WILDCARD_TILDE)
								&& headers.get(CommonConstants.WILDCARD_TILDE) instanceof Map) {
							response.put("headers", headers.get(CommonConstants.WILDCARD_TILDE));
						} else {
							response.put("headers", headers);
						}
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
						if (body.containsKey(CommonConstants.WILDCARD_TILDE)) {
							response.put("body", body.get(CommonConstants.WILDCARD_TILDE));
						} else {
							// script
							if (scriptCfg != null && scriptCfg.get("type") != null && scriptCfg.get("source") != null) {
								try {
									Object respBody = ScriptHelper.execute(scriptCfg, ctxNode, stepContext);
									if (respBody != null) {
										body.putAll((Map<String, Object>) respBody);
									}
								} catch (ScriptException e) {
									LogService.setBizId(inputContext.getStepContext().getTraceId());
									LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(scriptCfg), e);
									throw new ExecuteScriptException(e, stepContext, scriptCfg);
								}
							}
							response.put("body", body);
						}
					}
				}
			} else {
				response.put("body", responseBody);
			}
		}
	}

	@Override
	protected Mono<RPCResponse> getClientSpecFromContext(InputConfig aConfig, InputContext inputContext) {
		RequestInputConfig config = (RequestInputConfig) aConfig;
		
		int timeout = config.getTimeout() < 1 ? 3000 : config.getTimeout() > 10000 ? 10000 : config.getTimeout();
		
		HttpMethod method = HttpMethod.valueOf(config.getMethod());
		String url = (String) request.get("url");
		String body = JSON.toJSONString(request.get("body"));

		Map<String, Object> hds = (Map<String, Object>) request.get("headers");
		if (hds == null) {
			hds = new HashMap<>();
		}
		HttpHeaders headers = MapUtil.toHttpHeaders(hds);

		if (!headers.containsKey(CommonConstants.HEADER_CONTENT_TYPE)) {
			// default content-type
			headers.add(CommonConstants.HEADER_CONTENT_TYPE, CommonConstants.CONTENT_TYPE_JSON);
		}
		
		// add default headers
		SystemConfig systemConfig = this.getCurrentApplicationContext().getBean(SystemConfig.class);
		for (String hdr : systemConfig.proxySetHeaders) {
			if(inputContext.getStepContext().getInputReqHeader(hdr) != null) {
				headers.addIfAbsent(hdr, (String) inputContext.getStepContext().getInputReqHeader(hdr));
			}
		}
		
		headers.remove(CommonConstants.HEADER_CONTENT_LENGTH);
		headers.add(CommonConstants.HEADER_TRACE_ID, inputContext.getStepContext().getTraceId());
		
		HttpMethod aggrMethod = HttpMethod.valueOf(inputContext.getStepContext().getInputReqAttr("method").toString());
		String aggrPath = (String)inputContext.getStepContext().getInputReqAttr("path");
		String aggrService = aggrPath.split("\\/")[2];
		
//		FizzWebClient client = FizzAppContext.appContext.getBean(FizzWebClient.class);
		FizzWebClient client = this.getCurrentApplicationContext().getBean(FizzWebClient.class);
		Mono<ClientResponse> clientResponse = client.aggrSend(aggrService, aggrMethod, aggrPath, null, method, url,
				headers, body, (long)timeout);
		return clientResponse.flatMap(cr->{
			RequestRPCResponse response = new RequestRPCResponse();
			response.setHeaders(cr.headers().asHttpHeaders());
			response.setBodyMono(cr.bodyToMono(String.class));
			response.setStatus(cr.statusCode());
			return Mono.just(response);
		});


	}

	private Map<String, Object> getResponses(Map<String, StepResponse> stepContext2) {
		// TODO Auto-generated method stub
		return null;
	}

	protected void doOnResponseSuccess(RPCResponse cr, long elapsedMillis) {
		HttpHeaders httpHeaders = (HttpHeaders) cr.getHeaders();
		Map<String, Object> headers = new HashMap<>();
		httpHeaders.forEach((key, value) -> {
			if (value.size() > 1) {
				headers.put(key.toUpperCase(), value);
			} else {
				headers.put(key.toUpperCase(), httpHeaders.getFirst(key));
			}
		});
		headers.put("ELAPSEDTIME", elapsedMillis + "ms");
		this.response.put("headers", headers);
		this.respContentType = httpHeaders.getFirst(CONTENT_TYPE);
		inputContext.getStepContext().addElapsedTime(prefix + request.get("url"),
				elapsedMillis);
	}
	protected Mono<Object> bodyToMono(ClientResponse cr){
		return cr.bodyToMono(String.class);
	}

	protected void doOnBodyError(Throwable ex, long elapsedMillis) {
		LogService.setBizId(inputContext.getStepContext().getTraceId());
		LOGGER.warn("failed to call {}", request.get("url"), ex);
		inputContext.getStepContext().addElapsedTime(
				stepResponse.getStepName() + "-" + "调用接口 failed " + request.get("url"), elapsedMillis);
	}

	// Parse response body according to content-type header
	public Object parseBody(String contentType, String responseBody) {
		String[] cts = contentType.split(";");
		Object body = null;
		for (int i = 0; i < cts.length; i++) {
			String ct = cts[i].toLowerCase();
			switch (ct) {
				case CONTENT_TYPE_JSON:
					body = JSON.parse(responseBody);
					break;
				case CONTENT_TYPE_TEXT:
					// parse text as json if start with "{" and end with "}" or start with "[" and
					// end with "]"
					if ((responseBody.startsWith("{") && responseBody.endsWith("}"))
							|| (responseBody.startsWith("[") && responseBody.endsWith("]"))) {
						try {
							body = JSON.parse(responseBody);
						} catch (Exception e) {
							body = responseBody;
						}
					} else {
						body = responseBody;
					}
					break;
				case CONTENT_TYPE_XML:
					body = responseBody;
					break;
				case CONTENT_TYPE_HTML:
					body = responseBody;
					break;
				case CONTENT_TYPE_JS:
					body = responseBody;
					break;
			}
			if (body != null) {
				break;
			}
		}
		if (body == null) {
			body = responseBody;
		}
		return body;
	}

	protected void doOnBodySuccess(Object resp, long elapsedMillis) {
		if(inputContext.getStepContext().isDebug()) {
			LogService.setBizId(inputContext.getStepContext().getTraceId());
			LOGGER.info("{} 耗时:{}ms URL={}, reqHeader={} req={} resp={}", prefix, elapsedMillis, request.get("url"),
					JSON.toJSONString(this.request.get("headers")),
					JSON.toJSONString(this.request.get("body")), resp);
		}
	}

	private void cleanup(ClientResponse clientResponse) {
		if (clientResponse != null) {
			clientResponse.bodyToMono(Void.class).subscribe();
		}
	}

	public static Class inputConfigClass (){
		return RequestInputConfig.class;
	}

}
