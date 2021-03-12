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
import we.fizz.input.ClientInputConfig;
import we.fizz.input.Input;
import we.fizz.input.InputConfig;
import we.fizz.input.PathMapping;
import we.fizz.input.ScriptHelper;
import we.flume.clients.log4j2appender.LogService;
import we.util.JacksonUtils;
import we.util.JsonSchemaUtils;
import we.util.MapUtil;

/**
 * 
 * @author linwaiwai
 * @author francis
 * @author zhongjie
 *
 */
public class Pipeline {
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
		ClientInputConfig config = (ClientInputConfig)input.getConfig();
		this.initialStepContext(clientInput);
		this.stepContext.setDebug(config.isDebug());
		
		if(traceId != null) {
			this.stepContext.setTraceId(traceId);
		}
		
		long t1 = System.currentTimeMillis();
		List<String> validateErrorList = inputValidate(input, clientInput);
		this.stepContext.addElapsedTime("入参校验", System.currentTimeMillis()-t1);
		
		if (!CollectionUtils.isEmpty(validateErrorList)) {
			long t2 = System.currentTimeMillis();
			String validateMsg = StringUtils.collectionToCommaDelimitedString(validateErrorList);
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
			step1.beforeRun(stepContext, null);
			Mono<List<StepResponse>> result = createStep(step1).expand(response -> {
				if (opSteps.isEmpty() || response.isStop()) {
					return Mono.empty();
				}
				Step step = opSteps.pop();
				step.beforeRun(stepContext, response);
				return createStep(step);
			}).flatMap(response -> Flux.just(response)).collectList();
			return result.flatMap(clientResponse -> {
				return handleOutput(input);
			});
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

	@SuppressWarnings("unchecked")
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
	public void initialStepContext(Map<String,Object> clientInput) {
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
			inputRequest.put("body", clientInput.get("body"));
		}
		stepContext.put("input", input);
	}


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
		if (input != null && input.getConfig() != null && input.getConfig().getDataMapping() != null) {
			Map<String, Object> responseMapping = (Map<String, Object>) input.getConfig().getDataMapping()
					.get("response");
			if (validateResponse != null) {
                responseMapping = validateResponse;
            }
			if (responseMapping != null && !StringUtils.isEmpty(responseMapping)) {
				ONode ctxNode = PathMapping.toONode(stepContext);
				
				// headers
				Map<String, Object> headers = PathMapping.transform(ctxNode, stepContext,
						(Map<String, Object>) responseMapping.get("fixedHeaders"),
						(Map<String, Object>) responseMapping.get("headers"));
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
		
		Object respBody = response.get("body");
		// 测试模式返回StepContext
		if (stepContext.returnContext() && respBody instanceof Map) {
			Map<String, Object> t = (Map<String, Object>) respBody;
			t.put(stepContext.CONTEXT_FIELD, stepContext);
		}
		
		aggResult.setBody(response.get("body"));
		aggResult.setHeaders(MapUtil.toMultiValueMap((Map<String, Object>) response.get("headers")));
		return aggResult;
	}

	@SuppressWarnings("unchecked")
	private List<String> inputValidate(Input input, Map<String, Object> clientInput) {
		try {
			InputConfig config = input.getConfig();
			if (config instanceof ClientInputConfig) {
				Map<String, Object> langDef = ((ClientInputConfig) config).getLangDef();
				this.handleLangDef(langDef);

				Map<String, Object> headersDef = ((ClientInputConfig) config).getHeadersDef();
				if (!CollectionUtils.isEmpty(headersDef)) {
					// 验证headers入参是否符合要求
					List<String> errorList = JsonSchemaUtils.validateAllowValueStr(JSON.toJSONString(headersDef), JSON.toJSONString(clientInput.get("headers")));
					if (!CollectionUtils.isEmpty(errorList)) {
						return errorList;
					}
				}

				Map<String, Object> paramsDef = ((ClientInputConfig) config).getParamsDef();
				if (!CollectionUtils.isEmpty(paramsDef)) {
					// 验证params入参是否符合要求
					List<String> errorList = JsonSchemaUtils.validateAllowValueStr(JSON.toJSONString(paramsDef), JSON.toJSONString(clientInput.get("params")));
					if (!CollectionUtils.isEmpty(errorList)) {
						return errorList;
					}
				}

				Map<String, Object> bodyDef = ((ClientInputConfig) config).getBodyDef();
				if (!CollectionUtils.isEmpty(bodyDef)) {
					// 验证body入参是否符合要求
					List<String> errorList = JsonSchemaUtils.validate(JSON.toJSONString(bodyDef), JSON.toJSONString(clientInput.get("body")));
					if (!CollectionUtils.isEmpty(errorList)) {
						return errorList;
					}
				}

				Map<String, Object> scriptValidate = ((ClientInputConfig) config).getScriptValidate();
				if (!CollectionUtils.isEmpty(scriptValidate)) {
					ONode ctxNode = PathMapping.toONode(stepContext);
					// 验证入参是否符合脚本要求
					try {
						List<String> errorList = (List<String>) ScriptHelper.execute(scriptValidate, ctxNode, stepContext, List.class);
						if (!CollectionUtils.isEmpty(errorList)) {
							return errorList;
						}
					} catch (ScriptException e) {
						LOGGER.warn("execute script failed, {}", JacksonUtils.writeValueAsString(scriptValidate), e);
						throw new ExecuteScriptException(e, stepContext, scriptValidate);
					}
				}
			}
			return null;
		} finally {
			I18nUtils.removeContextLocale();
		}
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
}
