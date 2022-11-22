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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.ConfigurableApplicationContext;

import reactor.core.publisher.Mono;
import we.fizz.component.ComponentExecutor;
import we.fizz.component.ComponentResult;
import we.fizz.component.IComponent;
import we.fizz.component.StepContextPosition;
import we.fizz.input.Input;
import we.fizz.input.InputConfig;
import we.fizz.input.InputContext;
import we.fizz.input.InputFactory;
import we.fizz.input.InputType;

/**
 * 
 * @author linwaiwai
 * @author Francis Dong
 *
 */
public class Step {
	private SoftReference<Pipeline> weakPipeline;
	private String name;
	
	// 是否在执行完当前step就返回
	private boolean stop; 
	
	private Map<String, Object> dataMapping;
	
	private Map<String, InputConfig> requestConfigs = new HashMap<String, InputConfig>();
	
	private List<IComponent> components;

	public List<IComponent> getComponents() {
		return components;
	}

	public void setComponents(List<IComponent> components) {
		this.components = components;
	}

	public SoftReference<Pipeline> getWeakPipeline() {
		return weakPipeline;
	}

	public void setWeakPipeline(SoftReference<Pipeline> weakPipeline) {
		this.weakPipeline = weakPipeline;
	}
	
	public ConfigurableApplicationContext getCurrentApplicationContext() {
		return this.getWeakPipeline() != null  ? this.getWeakPipeline().get().getApplicationContext(): null;
	}

	public static class Builder {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Step read(Map<String, Object> config, SoftReference<Pipeline> weakPipeline) {
			Step step = new Step();
			step.setWeakPipeline(weakPipeline);
			List<Map> requests = (List<Map>) config.get("requests");
			if (CollectionUtils.isNotEmpty(requests)) {
				for (Map requestConfig : requests) {
					InputConfig inputConfig = InputFactory.createInputConfig(requestConfig);
					step.addRequestConfig((String) requestConfig.get("name"), inputConfig);
				}
			}
			step.setComponents(ComponentExecutor.buildComponents((List<Map<String, Object>>) config.get("components")));
			return step;
		}
	}
	
	private StepContext<String, Object> stepContext;

	public StepContext<String, Object> getStepContext(){
		return this.stepContext;
	}

	private StepResponse lastStepResponse = null;
	private Map<String, Input> inputs = new HashMap<String, Input>();
	public void beforeRun(StepContext<String, Object> stepContext2, StepResponse response ) {
		stepContext = stepContext2;
		lastStepResponse = response;
		StepResponse stepResponse = (StepResponse) stepContext.get(this.name);
		Map<String, InputConfig> configs = this.getRequestConfigs();
		for(String configName :configs.keySet()) {
			InputConfig inputConfig = configs.get(configName);
			InputType type = inputConfig.getType();
			Input input = InputFactory.createInput(type.toString());
			input.setWeakStep(new SoftReference<Step>(this));
			input.setConfig(inputConfig);
			input.setName(configName);
			input.setStepResponse(stepResponse);
			InputContext context = new InputContext(stepContext, lastStepResponse);
			input.beforeRun(context); 
			inputs.put(input.getName(), input);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Mono> run() {
		List<Mono> monos = new ArrayList<Mono>();  
		for(String requestName :inputs.keySet()) {
			Input input = inputs.get(requestName);
			List<IComponent> components = input.getConfig().getComponents();
			if (components != null && components.size() > 0) {
				StepContextPosition stepCtxPos = new StepContextPosition(name, requestName);
				Mono<Object> result = ComponentExecutor.exec(components, stepContext, stepCtxPos, (ctx, pos) -> {
					if (input.needRun(ctx)) {
						return input.run();
					}
					Map<String, Object> inputResult = new HashMap<String, Object>();
					inputResult.put("data", new HashMap<String, Object>());
					inputResult.put("request", input);
					return Mono.just(inputResult);
				}).flatMap(r -> {
					if (r instanceof ComponentResult) {
						Map<String, Object> inputResult = new HashMap<String, Object>();
						inputResult.put("data", new HashMap<String, Object>());
						inputResult.put("request", input);
						return Mono.just(inputResult);
					} else {
						return Mono.just(r);
					}
				});
				monos.add(result);
			} else {
				if (input.needRun(stepContext)) {
					Mono<Map> singleMono = input.run();
					monos.add(singleMono);
				}
			}
		}
		return monos;	
	}
	
	

	public void afeterRun() {
		
	}
	
	public InputConfig addRequestConfig(String name,  InputConfig requestConfig) {
		return requestConfigs.put(name, requestConfig);
	}
 

	public Map<String, InputConfig> getRequestConfigs() {
		return requestConfigs;
	}


	public String getName() {
		if (name == null) {
			return name = "step" + (int)(Math.random()*100);
		}
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public Map<String, Object> getDataMapping() {
		return dataMapping;
	}

	public void setDataMapping(Map<String, Object> dataMapping) {
		this.dataMapping = dataMapping;
	}


}

