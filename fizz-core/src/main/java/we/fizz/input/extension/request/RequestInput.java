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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
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
import we.fizz.input.IInput;
import we.fizz.input.InputConfig;
import we.fizz.input.InputContext;
import we.fizz.input.InputType;
import we.fizz.input.PathMapping;
import we.fizz.input.RPCInput;
import we.fizz.input.RPCResponse;
import we.fizz.input.ScriptHelper;
import we.flume.clients.log4j2appender.LogService;
import we.proxy.FizzWebClient;
import we.proxy.http.HttpInstanceService;
import we.util.JacksonUtils;
import we.util.MapUtil;
import we.xml.JsonToXml;
import we.xml.XmlToJson;
import we.xml.XmlToJson.Builder;

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
	private static final String CONTENT_TYPE_TEXT_XML = "text/xml";
	private static final String CONTENT_TYPE_JS = "application/javascript";
	private static final String CONTENT_TYPE_HTML = "text/html";
	private static final String CONTENT_TYPE_TEXT = "text/plain";
	private static final String CONTENT_TYPE_AUTO = "auto";
	private static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";
	private static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";

	private static final String CONTENT_TYPE = "content-type";
	
	private static final Integer SERVICE_TYPE_HTTP = 2;
	
	private String respContentType;
	private String reqContentType;
	
	private String[] xmlArrPaths;
	
	private static Pattern PATH_VAR_PATTERN = Pattern.compile("(\\{)([^/]*)(\\})");
	
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

		ONode ctxNode = null;
		// 数据转换
		if (inputContext != null && inputContext.getStepContext() != null) {
			StepContext<String, Object> stepContext = inputContext.getStepContext();
			ctxNode = PathMapping.toONode(stepContext);
			Map<String, Object> dataMapping = this.getConfig().getDataMapping();
			if (dataMapping != null) {
				Map<String, Object> requestMapping = (Map<String, Object>) dataMapping.get("request");
				if (!CollectionUtils.isEmpty(requestMapping)) {
					reqContentType = (String) requestMapping.get("contentType");

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
					boolean supportMultiLevels = true;
					if (CONTENT_TYPE_MULTIPART_FORM_DATA.equals(reqContentType) || 
							CONTENT_TYPE_FORM_URLENCODED.equals(reqContentType)) {
						supportMultiLevels = false;
					}
					Map<String,Object> body = PathMapping.transform(ctxNode, stepContext,
							(Map<String, Object>) requestMapping.get("fixedBody"),
							(Map<String, Object>) requestMapping.get("body"), supportMultiLevels);
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
					.append(config.getPath().startsWith("/") ? "" : "/").append(setPathVariable(ctxNode, config.getPath()));

			UriComponents uriComponents = UriComponentsBuilder.fromUriString(sb.toString())
					.queryParams(MapUtil.toMultiValueMap(params)).build();

			request.put("url", uriComponents.toUriString());
		} else {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(config.getBaseUrl() + setPathVariable(ctxNode, config.getPath()))
					.queryParams(MapUtil.toMultiValueMap(params)).build();
			request.put("url", uriComponents.toUriString());
		}
	}
	
	private String setPathVariable(ONode ctxNode, String path) {
		if (ctxNode == null || StringUtils.isBlank(path)) {
			return path;
		}
		String[] paths = path.split("/");
		for (int i = 0; i < paths.length; i++) {
			Matcher matcher = PATH_VAR_PATTERN.matcher(paths[i]);
			if (matcher.find()) {
				String jsonPath = matcher.group(2);
				Object val = PathMapping.getValueByPath(ctxNode, jsonPath);
				if (val != null && !(val instanceof Map) && !(val instanceof List)) {
					paths[i] = matcher.replaceAll(String.valueOf(val));
				}
			}
		}
		return String.join("/", paths);
	}

	@Override
	public void doResponseMapping(InputConfig aConfig, InputContext inputContext, Object responseBody) {

		RequestInputConfig config = (RequestInputConfig) aConfig;
		String cfgContentType = null;
		Map<String, Object> dataMapping = config.getDataMapping();
		Map<String, Object> responseMapping = null;
		if (dataMapping != null) {
			responseMapping = (Map<String, Object>) dataMapping.get("response");
			if (!CollectionUtils.isEmpty(responseMapping)) {
				cfgContentType = (String) responseMapping.get("contentType");
				String paths = (String) responseMapping.get("xmlArrPaths");
				if (StringUtils.isNotBlank(paths)) {
					xmlArrPaths = paths.split(",");
				}
			}
		}

		String ct = null;
		if (cfgContentType == null || CONTENT_TYPE_AUTO.equals(cfgContentType)) {
			ct = this.respContentType;
		} else {
			ct = cfgContentType;
		}
		
		response.put("body", this.parseBody(ct, (String)responseBody));

		// 数据转换
		if (inputContext != null && inputContext.getStepContext() != null) {
			StepContext<String, Object> stepContext = inputContext.getStepContext();
			if (!CollectionUtils.isEmpty(responseMapping)) {
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
		}
	}

	@Override
	protected Mono<RPCResponse> getClientSpecFromContext(InputConfig aConfig, InputContext inputContext) {
		RequestInputConfig config = (RequestInputConfig) aConfig;
		
		int timeout = config.getTimeout() < 1 ? 3000 : config.getTimeout() > 10000 ? 10000 : config.getTimeout();
		
		HttpMethod method = HttpMethod.valueOf(config.getMethod());
		String url = (String) request.get("url");

		Map<String, Object> hds = (Map<String, Object>) request.get("headers");
		if (hds == null) {
			hds = new HashMap<>();
		}
		HttpHeaders headers = MapUtil.toHttpHeaders(hds);

		// default content-type
		if (!headers.containsKey(CommonConstants.HEADER_CONTENT_TYPE)) {
			if (CONTENT_TYPE_XML.equals(reqContentType) || CONTENT_TYPE_TEXT_XML.equals(reqContentType)) {
				headers.add(CommonConstants.HEADER_CONTENT_TYPE, CONTENT_TYPE_XML);
			} else if (CONTENT_TYPE_MULTIPART_FORM_DATA.equals(reqContentType)) {
				headers.add(CommonConstants.HEADER_CONTENT_TYPE, CONTENT_TYPE_MULTIPART_FORM_DATA);
			} else if (CONTENT_TYPE_FORM_URLENCODED.equals(reqContentType)) {
				headers.add(CommonConstants.HEADER_CONTENT_TYPE, CONTENT_TYPE_FORM_URLENCODED);
			} else {
				headers.add(CommonConstants.HEADER_CONTENT_TYPE, CommonConstants.CONTENT_TYPE_JSON);
			}
		}
		
		// add default headers
		SystemConfig systemConfig = this.getCurrentApplicationContext().getBean(SystemConfig.class);
		for (String hdr : systemConfig.getProxySetHeaders()) {
			if(inputContext.getStepContext().getInputReqHeader(hdr) != null) {
				headers.addIfAbsent(hdr, (String) inputContext.getStepContext().getInputReqHeader(hdr));
			}
		}
		
		headers.remove(CommonConstants.HEADER_CONTENT_LENGTH);
		headers.add(CommonConstants.HEADER_TRACE_ID, inputContext.getStepContext().getTraceId());
		request.put("headers", MapUtil.headerToHashMap(headers));
		
		Object body = null;
		if (CONTENT_TYPE_XML.equals(reqContentType) || CONTENT_TYPE_TEXT_XML.equals(reqContentType)) {
			// convert JSON to XML if it is XML content type
			request.put("jsonBody", request.get("body"));
			String jsonStr = JSON.toJSONString(request.get("body"));
			LOGGER.info("jsonBody={}", jsonStr);
			JsonToXml jsonToXml = new JsonToXml.Builder(jsonStr).build();
			body = jsonToXml.toString();
			request.put("body", body);
			LOGGER.info("body={}", body);
			LOGGER.info("headers={}", JSON.toJSONString(headers));
		} else if (CONTENT_TYPE_MULTIPART_FORM_DATA.equals(reqContentType)) {
			MultiValueMap<String, Object> mpDataMap = MapUtil
					.toMultipartDataMap((Map<String, Object>) request.get("body"));
			MapUtil.replaceWithFilePart(mpDataMap, CommonConstants.FILE_KEY_PREFIX,
					inputContext.getStepContext().getFilePartMap());
			body = BodyInserters.fromMultipartData(mpDataMap);
		} else if (CONTENT_TYPE_FORM_URLENCODED.equals(reqContentType)) {
			body = BodyInserters.fromFormData(MapUtil.toMultiValueMap((Map<String, Object>) request.get("body")));
		} else {
			body = JSON.toJSONString(request.get("body"));
		}
		
		HttpMethod aggrMethod = HttpMethod.valueOf(inputContext.getStepContext().getInputReqAttr("method").toString());
		String aggrPath = (String)inputContext.getStepContext().getInputReqAttr("path");
		String aggrService = aggrPath.split("\\/")[2];
		
		FizzWebClient client = this.getCurrentApplicationContext().getBean(FizzWebClient.class);
		// Mono<ClientResponse> clientResponse = client.aggrSend(aggrService, aggrMethod, aggrPath, null, method, url,
		// 		headers, body, (long)timeout);

		Mono<ClientResponse> clientResponse = client.send(inputContext.getStepContext().getTraceId(), method, url, headers, body, (long)timeout);
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
				case CONTENT_TYPE_TEXT_XML:
					Builder builder = new XmlToJson.Builder(responseBody);
					if (this.xmlArrPaths != null && this.xmlArrPaths.length > 0) {
						for (int j = 0; j < this.xmlArrPaths.length; j++) {
							String p = this.xmlArrPaths[j];
							builder = builder.forceList(p);
						}
					}
					body = builder.build().toJson().toMap();
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

	@SuppressWarnings("unused")
	private void cleanup(ClientResponse clientResponse) {
		if (clientResponse != null) {
			clientResponse.bodyToMono(Void.class).subscribe();
		}
	}

	@SuppressWarnings("rawtypes")
	public static Class inputConfigClass (){
		return RequestInputConfig.class;
	}

}
