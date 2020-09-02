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
package com.wehotel.fizz.input;

import java.util.HashMap;
import java.util.Map;

import com.wehotel.fizz.StepContext;
import com.wehotel.fizz.StepResponse;

import reactor.core.publisher.Mono;

/**
 * 
 * @author linwaiwai
 *
 */
public class Input {
	protected String name;
	protected InputConfig config;
	protected InputContext inputContext;
	protected StepResponse lastStepResponse = null;
	protected StepResponse stepResponse;
	
	public void setConfig(InputConfig inputConfig) {
		config = inputConfig;
	}
	public InputConfig getConfig() {
		return config;
	}

	public void beforeRun(InputContext context) {
		this.inputContext = context;
	}

	public String getName() {
		if (name == null) {
			return name = "input" + (int)(Math.random()*100);
		}
		return name;
	}

	/**
	 * 检查该Input是否需要运行，默认都运行
	 * @stepContext Step上下文
	 * @return TRUE：运行
	 */
	public boolean needRun(StepContext<String, Object> stepContext) {
		return Boolean.TRUE;
	}

	public Mono<Map> run() {
		return null;
	}
	public void setName(String configName) {
		this.name = configName;
		
	}
	
	public StepResponse getStepResponse() {
		return stepResponse;
	}
	public void setStepResponse(StepResponse stepResponse) {
		this.stepResponse = stepResponse;
	}
	
	 
	
}
