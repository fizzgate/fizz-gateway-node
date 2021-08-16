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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.script.ScriptException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;

import we.schema.util.I18nUtils;
import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.constants.CommonConstants;
import we.exception.ExecuteScriptException;
import we.exception.RedirectException;
import we.exception.StopAndResponseException;
import we.fizz.component.ComponentHelper;
import we.fizz.component.IComponent;
import we.fizz.component.StepContextPosition;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.ClientInputConfig;
import we.fizz.input.Input;
import we.fizz.input.InputConfig;
import we.fizz.input.PathMapping;
import we.fizz.input.ScriptHelper;
import we.flume.clients.log4j2appender.LogService;
import we.schema.util.PropertiesSupportUtils;
import we.util.JacksonUtils;
import we.util.JsonSchemaUtils;
import we.util.MapUtil;
import we.xml.JsonToXml;
import we.xml.XmlToJson;
import we.xml.XmlToJson.Builder;

/**
 * 
 * @author linwaiwai
 * @author Francis Dong
 * @author zhongjie
 *
 */
public class Pipeline {
	private static final String CONTENT_TYPE_XML = "application/xml";
	private ConfigurableApplicationContext applicationContext;
	private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);
	private LinkedList<Step> steps = new LinkedList<Step>();
	private StepContext<String, Object> stepContext = new StepContext<>();
	public void addStep(Step step) {
		steps.add(step);
	}
	
	static void displayValue(String n) {
	    System.out.println("input : " + n);
	}
	
	public StepContext<String, Object> getStepContext(){
		return this.stepContext;
	}
	
	public Mono<AggregateResult> run(Input input, Map<String, Object> clientInput, String traceId) {
		return this.runPipeline(input, clientInput, traceId).onErrorResume((ex) -> {
			String message = ex.getMessage();
			if (ex.getMessage() == null) {
				message = "failed to run aggregation pipeline, message: " + ex.toString();
				StackTraceElement[] stacks = ex.getStackTrace();
				if (stacks != null && stacks.length > 0) {
					message = message + " at " + stacks[0];
				}
			}
			if (ex instanceof StopAndResponseException) {
				throw (StopAndResponseException) ex;
			}
			if (ex instanceof RedirectException) {
				throw (RedirectException) ex;
			}
			if (ex instanceof ExecuteScriptException) {
				throw (ExecuteScriptException) ex;
			}
			if (ex instanceof FizzRuntimeException && ex.getMessage() != null) {
				FizzRuntimeException e = (FizzRuntimeException) ex;
				if (e.getStepContext() == null) {
					e.setStepContext(stepContext);
				}
				throw e;
			} else {
				throw new FizzRuntimeException(message, ex, stepContext);
			}
		});
	}
	
	public Mono<AggregateResult> runPipeline(Input input, Map<String, Object> clientInput, String traceId) {
		ClientInputConfig config = (ClientInputConfig)input.getConfig();
		this.initialStepContext(clientInput, config);
		this.stepContext.setDebug(config.isDebug());
		this.stepContext.setApplicationContext(applicationContext);
		
		if(traceId != null) {
			this.stepContext.setTraceId(traceId);
		}
		
		long t1 = System.currentTimeMillis();

		@SuppressWarnings("unchecked")
		String validateMsg = this.inputValidate(input,
				(Map<String, Object>)((Map<String, Object>)this.stepContext.get("input")).get("request"));

		this.stepContext.addElapsedTime("入参校验", System.currentTimeMillis()-t1);
		
		if (StringUtils.hasText(validateMsg)) {
			long t2 = System.currentTimeMillis();
			// 将验证错误信息放入上下文
			stepContext.put("validateMsg", validateMsg);
			Map<String, Object> validateResponse = config.getValidateResponse();
			// 数据转换
			AggregateResult aggregateResult = this.doInputDataMapping(input, validateResponse);
			this.stepContext.addElapsedTime("入参校验结果转换", System.currentTimeMillis() - t2);
			return Mono.just(aggregateResult);
		}

		if(CollectionUtils.isEmpty(steps)) {
			return handleOutput(input);
		}else {			
			LinkedList<Step> opSteps = (LinkedList<Step>) steps.clone();
			Step step1 = opSteps.removeFirst();
			Mono<List<StepResponse>> result = runStep(step1, null).expand(lastStepResponse -> {
				if (opSteps.isEmpty() || lastStepResponse.isStop()) {
					return Mono.empty();
				}
				Step step = opSteps.pop();
				return runStep(step, lastStepResponse);
			}).flatMap(response -> Flux.just(response)).collectList();
			return result.flatMap(clientResponse -> {
				return handleOutput(input);
			});
		}
	}
	
	private Mono<StepResponse> runStep(Step step, StepResponse lastStepResponse){
		StepResponse stepResponse = new StepResponse(step, null, new HashMap<String, Map<String, Object>>());
		stepContext.put(step.getName(), stepResponse);
		List<IComponent> components = step.getComponents();
		if (components != null && components.size() > 0) {
			StepContextPosition stepCtxPos = new StepContextPosition(step.getName());
			return ComponentHelper.run(components, stepContext, stepCtxPos, (ctx, pos) -> {
				step.beforeRun(stepContext, null);
				return createStep(step).flatMap(r -> {
					ctx.addStepCircleResult(pos.getStepName());
					return Mono.just(r);
				});
			}).flatMap(sr -> Mono.just((StepResponse)sr));
		} else {
			step.beforeRun(stepContext, null);
			return createStep(step);
		}
	}
	
	private Mono<AggregateResult> handleOutput(Input input){
		// 数据转换
		long t3 = System.currentTimeMillis();
		AggregateResult aggResult = this.doInputDataMapping(input, null);
		this.stepContext.addElapsedTime(input.getName()+"聚合接口响应结果数据转换", System.currentTimeMillis() - t3);
		if(this.stepContext.isDebug() || LOGGER.isDebugEnabled()) {
			LogService.setBizId(this.stepContext.getTraceId());
			String jsonString = JSON.toJSONString(aggResult);
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("aggResult {}", jsonString);
				LOGGER.debug("stepContext {}", JSON.toJSONString(stepContext));	
			}else {				
				LOGGER.info("aggResult {}", jsonString);
				LOGGER.info("stepContext {}", JSON.toJSONString(stepContext));			
			}
		}
		return Mono.just(aggResult);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Mono<StepResponse> createStep(Step step) {
		long start = System.currentTimeMillis();
		List<Mono> monos = step.run();
		Mono<Map>[] monoArray = monos.stream().toArray(Mono[]::new);
		Mono<StepResponse>result = Flux.merge(monoArray).reduce(new HashMap(), (item1, item2)-> {
			Input input = (Input)item2.get("request");
			item1.put(input.getName() , item2.get("data"));
			return item1;
		}).flatMap(item -> {
			// stepResult 数据转换
			long t1 = System.currentTimeMillis();
			StepResponse stepResponse = this.doStepDataMapping(step);
			stepResponse.setStop(step.isStop());
			long t2 = System.currentTimeMillis();
			this.stepContext.addElapsedTime(step.getName() + "结果数据转换", t2 - t1);
			this.stepContext.addElapsedTime(step.getName() + "耗时", System.currentTimeMillis() - start);

			return Mono.just(stepResponse);
		});
		return result;
	}
	
	/**
	 * 初始化上下文
	 * @param clientInput 客户端提交上来的信息
	 */
	public void initialStepContext(Map<String,Object> clientInput, ClientInputConfig config) {
		Map<String,Object> input = new HashMap<>();
		Map<String,Object> inputRequest = new HashMap<>();
		Map<String,Object> inputResponse = new HashMap<>();
		input.put("request", inputRequest);
		input.put("response", inputResponse);
		if(clientInput != null) {
			inputRequest.put("path", clientInput.get("path"));
			inputRequest.put("method", clientInput.get("method"));
			inputRequest.put("headers", clientInput.get("headers"));
			inputRequest.put("params", clientInput.get("params"));
			stepContext.addFilePartMap((Map<String, FilePart>) clientInput.get("filePartMap"));
			
			if (CONTENT_TYPE_XML.equals(config.getContentType()) || (StringUtils.isEmpty(config.getContentType())
					&& isXmlContentType((String) clientInput.get("contentType")))) {
				String[] paths = null;
				if (!StringUtils.isEmpty(config.getXmlArrPaths())) {
					paths = config.getXmlArrPaths().split(",");
				}
				Builder builder = new XmlToJson.Builder((String) clientInput.get("body"));
				if (paths != null && paths.length > 0) {
					for (int j = 0; j < paths.length; j++) {
						String p = paths[j];
						builder = builder.forceList(p);
					}
				}
				inputRequest.put("body", builder.build().toJson().toMap());
			} else if (clientInput.get("body") instanceof Map) {
				inputRequest.put("body", clientInput.get("body"));
			} else {
				inputRequest.put("body", JSON.parse((String) clientInput.get("body")));
			}
		}
		stepContext.put("input", input);
	}

	private boolean isXmlContentType(String contentType) {
		if (contentType != null) {
			String[] cts = contentType.split(";");
			for (int i = 0; i < cts.length; i++) {
				if (CONTENT_TYPE_XML.equals(cts[i])) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private StepResponse doStepDataMapping(Step step) {
		StepResponse stepResponse = (StepResponse)stepContext.get(step.getName());
		if (step.getDataMapping() != null) {
			Map<String, Object> responseMapping = (Map<String, Object>) step.getDataMapping().get("response");
			if(responseMapping != null && !StringUtils.isEmpty(responseMapping)) {
				ONode ctxNode = PathMapping.toONode(stepContext);
				
				// body
				stepResponse.setResult(PathMapping.transform(ctxNode, stepContext,
						(Map<String, Object>) responseMapping.get("fixedBody"),
						(Map<String, Object>) responseMapping.get("body")));
				
				// script
				if(responseMapping.get("script") != null) {
					Map<String, Object> scriptCfg = (Map<String, Object>) responseMapping.get("script");
					try {
						Map<String, Object> stepBody = ScriptHelper.execute(scriptCfg, ctxNode, stepContext, Map.class);
						if(stepBody != null) {
							stepResponse.setResult(stepBody);
						}
					} catch (ScriptException e) {
						LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(scriptCfg), e);
						throw new ExecuteScriptException(e, stepContext, scriptCfg);
					}
				}
			}
		}
		return stepResponse;
	}

    /**
     * 当validateResponse不为空表示验参失败，使用该配置响应数据
     */
	@SuppressWarnings("unchecked")
	private AggregateResult doInputDataMapping(Input input, Map<String, Object> validateResponse) {
		AggregateResult aggResult = new AggregateResult();
		Map<String, Map<String,Object>> group = (Map<String, Map<String, Object>>) stepContext.get("input");
		if(group == null) {
			group = new HashMap<String, Map<String,Object>>();
			stepContext.put("input", group);
		}
		Map<String,Object> response = null;
		if(group.get("response") == null) {
			response = new HashMap<>();
			group.put("response", response);
		}
		response = group.get("response");
		String respContentType = null;
		if (input != null && input.getConfig() != null && input.getConfig().getDataMapping() != null) {
			Map<String, Object> responseMapping = (Map<String, Object>) input.getConfig().getDataMapping()
					.get("response");
			if (validateResponse != null) {
                responseMapping = validateResponse;
            }
			if (!CollectionUtils.isEmpty(responseMapping)) {
				respContentType = (String) responseMapping.get("contentType");
				ONode ctxNode = PathMapping.toONode(stepContext);
				
				// headers
				Map<String, Object> headers = PathMapping.transform(ctxNode, stepContext,
						MapUtil.upperCaseKey((Map<String, Object>) responseMapping.get("fixedHeaders")),
						MapUtil.upperCaseKey((Map<String, Object>) responseMapping.get("headers")), false);
				if (headers.containsKey(CommonConstants.WILDCARD_TILDE)
						&& headers.get(CommonConstants.WILDCARD_TILDE) instanceof Map) {
					response.put("headers", headers.get(CommonConstants.WILDCARD_TILDE));
				} else {
					response.put("headers", headers);
				}

				// body
				Map<String,Object> body = PathMapping.transform(ctxNode, stepContext,
								(Map<String, Object>) responseMapping.get("fixedBody"),
								(Map<String, Object>) responseMapping.get("body"));
				if (body.containsKey(CommonConstants.WILDCARD_TILDE)) {
					response.put("body", body.get(CommonConstants.WILDCARD_TILDE));
				} else {
					// script
					if (responseMapping.get("script") != null) {
						Map<String, Object> scriptCfg = (Map<String, Object>) responseMapping.get("script");
						try {
							Object respBody = ScriptHelper.execute(scriptCfg, ctxNode, stepContext);
							if(respBody != null) {
								body.putAll((Map<String, Object>) respBody);
							}
						} catch (ScriptException e) {
							LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(scriptCfg), e);
							throw new ExecuteScriptException(e, stepContext, scriptCfg);
						}
					}
					response.put("body", body);
				}
			}
		}
		
		HttpHeaders httpHeaders = MapUtil.toHttpHeaders((Map<String, Object>) response.get("headers"));
		if (CONTENT_TYPE_XML.equals(respContentType) && !httpHeaders.containsKey(CommonConstants.HEADER_CONTENT_TYPE)) {
			httpHeaders.add(CommonConstants.HEADER_CONTENT_TYPE.toUpperCase(), CONTENT_TYPE_XML);
			response.put(CommonConstants.HEADER_CONTENT_TYPE.toUpperCase(), CONTENT_TYPE_XML);
		}
		
		// convert JSON to XML if it is XML content type
		if(CONTENT_TYPE_XML.equals(respContentType)) {
			Object respBody = response.get("body");
			response.put("jsonBody", respBody);
			JsonToXml jsonToXml = new JsonToXml.Builder(JSON.toJSONString(respBody)).build();
			response.put("body", jsonToXml.toString());
		}
		
		Object respBody = response.get("body");
		// 测试模式返回StepContext
		if (stepContext.returnContext() && respBody instanceof Map) {
			Map<String, Object> t = (Map<String, Object>) respBody;
			t.put(stepContext.CONTEXT_FIELD, stepContext);
		}
		
		aggResult.setBody(response.get("body"));
		aggResult.setHeaders(httpHeaders);
		return aggResult;
	}

	private static final String LANGUAGE_CHINESE = "zh";
    private static final String LANGUAGE_ENGLISH = "en";
	enum ValidateType {
		/**
		 * Header
		 */
		HEADER("请求头", "Header"),
		/**
		 * Query param
		 */
		QUERY_PARAM("Query参数", "Query param"),
		/**
		 * Body
		 */
		BODY("请求体", "Body"),
        /**
         * Script
         */
        SCRIPT("脚本校验", "Script");

		ValidateType(String tipZh, String tipEn) {
			this.tipZh = tipZh;
			this.tipEn = tipEn;
		}

		String tipZh;
		String tipEn;

		public String getTip() {
			String language = I18nUtils.getContextLocale().getLanguage();
			if (LANGUAGE_CHINESE.equals(language)) {
				return tipZh;
			} else if (LANGUAGE_ENGLISH.equals(language)) {
				return tipEn;
			}
			return tipZh;
		}
	}

	String inputValidate(Input input, Map<String, Object> clientInput) {
		try {
			InputConfig config = input.getConfig();
			if (config instanceof ClientInputConfig) {
				Map<String, Object> langDef = ((ClientInputConfig) config).getLangDef();
				this.handleLangDef(langDef);

				Pair<ValidateType, List<String>> validateTypeAndValidateErrorListPair =
						this.doInputValidate((ClientInputConfig) config, clientInput);
				if (validateTypeAndValidateErrorListPair == null) {
					return null;
				}
				return String.format("%s: %s", validateTypeAndValidateErrorListPair.getFirst().getTip(),
                        StringUtils.collectionToCommaDelimitedString(validateTypeAndValidateErrorListPair.getSecond()));
			}
			return null;
		} finally {
			I18nUtils.removeContextLocale();
		}
	}

	private Pair<ValidateType, List<String>> doInputValidate(ClientInputConfig config, Map<String, Object> clientInput) {
		Map<String, Object> headersDef = config.getHeadersDef();
		if (!CollectionUtils.isEmpty(headersDef)) {
			// 验证headers入参是否符合要求
			List<String> errorList;
			PropertiesSupportUtils.setContextSupportPropertyUpperCase();
			try {
				errorList = JsonSchemaUtils.validateAllowValueStr(JSON.toJSONString(headersDef),
						JSON.toJSONString(clientInput.get("headers")));
			} finally {
				PropertiesSupportUtils.removeContextSupportPropertyUpperCase();
			}

			if (!CollectionUtils.isEmpty(errorList)) {
				return Pair.of(ValidateType.HEADER, errorList);
			}
		}

		Map<String, Object> paramsDef = config.getParamsDef();
		if (!CollectionUtils.isEmpty(paramsDef)) {
			// 验证params入参是否符合要求
			List<String> errorList = JsonSchemaUtils.validateAllowValueStr(JSON.toJSONString(paramsDef),
					JSON.toJSONString(clientInput.get("params")));
			if (!CollectionUtils.isEmpty(errorList)) {
				return Pair.of(ValidateType.QUERY_PARAM, errorList);
			}
		}

		Map<String, Object> bodyDef = config.getBodyDef();
		if (!CollectionUtils.isEmpty(bodyDef)) {
			// 验证body入参是否符合要求
			List<String> errorList = JsonSchemaUtils.validate(JSON.toJSONString(bodyDef),
					JSON.toJSONString(clientInput.get("body")));
			if (!CollectionUtils.isEmpty(errorList)) {
				return Pair.of(ValidateType.BODY, errorList);
			}
		}

		Map<String, Object> scriptValidate = config.getScriptValidate();
		if (!CollectionUtils.isEmpty(scriptValidate)) {
			ONode ctxNode = PathMapping.toONode(stepContext);
			// 验证入参是否符合脚本要求
			try {
				@SuppressWarnings("unchecked")
				List<String> errorList = (List<String>) ScriptHelper.execute(scriptValidate, ctxNode, stepContext, List.class);
				if (!CollectionUtils.isEmpty(errorList)) {
					return Pair.of(ValidateType.SCRIPT, errorList);
				}
			} catch (ScriptException e) {
				LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(scriptValidate), e);
				throw new ExecuteScriptException(e, stepContext, scriptValidate);
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void handleLangDef(Map<String, Object> langDef) {
		if (!CollectionUtils.isEmpty(langDef)) {
			// 存在提示语言定义配置
			Object langParamObj = langDef.get("langParam");
			String langParam = null;
			if (langParamObj instanceof String) {
				langParam = (String) langParamObj;
			}
			Object langMappingObj = langDef.get("langMapping");
			Map<String, Object> langMapping = null;
			if (langMappingObj instanceof Map) {
				langMapping = (Map<String, Object>) langMappingObj;
			}
			if (langParam != null && !CollectionUtils.isEmpty(langMapping)) {
				ONode ctxNode = PathMapping.toONode(stepContext);
				Map<String, Object> langParamMap = new HashMap<>(2);
				langParamMap.put("langParam", langParam);
				Map<String, Object> transformMap = PathMapping.transform(ctxNode, stepContext, null, langParamMap);
				Object langParamValue = transformMap.get("langParam");
				if (langParamValue != null) {
					// 判断使用哪种语言
					Object zh = langMapping.get("zh");
					if (zh != null && zh.toString().equals(langParamValue.toString())) {
						I18nUtils.setContextLocale(new Locale("zh"));
					}
					Object en = langMapping.get("en");
					if (en != null && en.toString().equals(langParamValue.toString())) {
						I18nUtils.setContextLocale(new Locale("en"));
					}
				}
			}
		}
	}

	public void setApplicationContext(ConfigurableApplicationContext appContext) {
		this.applicationContext = appContext;
	}

	public ConfigurableApplicationContext getApplicationContext() {
		return this.applicationContext;
	}
}
