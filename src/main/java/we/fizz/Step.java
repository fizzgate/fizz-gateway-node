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

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.alibaba.fastjson.JSON;

import reactor.core.publisher.Mono;
import we.fizz.input.Input;
import we.fizz.input.InputConfig;
import we.fizz.input.InputContext;
import we.fizz.input.InputFactory;
import we.fizz.input.InputType;

/**
 * 
 * @author linwaiwai
 * @author francis
 *
 */
public class Step {
	private SoftReference<Pipeline> weakPipeline;
	private String name;
	
	// 是否在执行完当前step就返回
	private boolean stop; 
	
	private Map<String, Object> dataMapping;
	
	private Map<String, InputConfig> requestConfigs = new HashMap<String, InputConfig>();

	public SoftReference<Pipeline> getWeakPipeline() {
		return weakPipeline;
	}

	public void setWeakPipeline(SoftReference<Pipeline> weakPipeline) {
		this.weakPipeline = weakPipeline;
	}
	
	public ConfigurableApplicationContext getCurrentApplicationContext() {
		return this.getWeakPipeline() != null  ? this.getWeakPipeline().get().getApplicationContext(): null;
	}

	public ConfigurableApplicationContext getCurrentApplicationContext() {
		return this.getWeakPipeline() != null  ? this.getWeakPipeline().get().getApplicationContext(): null;
	}

	public static class Builder {
		public Step read(Map<String, Object> config, SoftReference<Pipeline> weakPipeline) {
			Step step = new Step();
			step.setWeakPipeline(weakPipeline);
			List<Map> requests= (List<Map>) config.get("requests");
			for(Map requestConfig: requests) {
				InputConfig inputConfig = InputFactory.createInputConfig(requestConfig);
				step.addRequestConfig((String)requestConfig.get("name"), inputConfig);
			}
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
		StepResponse stepResponse = new StepResponse(this, null, new HashMap<String, Map<String, Object>>());
		stepContext.put(name, stepResponse);
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

	public List<Mono> run() {
		List<Mono> monos = new ArrayList<Mono>();  
		for(String name :inputs.keySet()) {
			Input input = inputs.get(name);
			if (input.needRun(stepContext)) {
				Mono<Map> singleMono = input.run();
				monos.add(singleMono);
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

