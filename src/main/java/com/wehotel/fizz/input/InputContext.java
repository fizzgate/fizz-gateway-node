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

/**
 * 
 * @author linwaiwai
 *
 */
public class InputContext {
	private StepContext<String, Object> stepContext;
	private StepResponse lastStepResponse = null;
	public InputContext(StepContext<String, Object> stepContext2, StepResponse lastStepResponse2) {
		this.stepContext = stepContext2;
		this.lastStepResponse = lastStepResponse2;
	}
	public StepContext<String, Object> getStepContext() {
		return stepContext;
	}
	public void setStepContext(StepContext<String, Object> stepContext) {
		this.stepContext = stepContext;
	}
	public StepResponse getLastStepResponse() {
		return lastStepResponse;
	}
	public void setLastStepResponse(StepResponse lastStepResponse) {
		this.lastStepResponse = lastStepResponse;
	}
//	public Map<String, Object> getResponses() {
//		 //TODO:
//		if (stepContext  != null) {
//			Map<String, Object> responses = new HashMap<String, Object>();
//			for( String key :stepContext.keySet()) {
//				StepResponse stepResponse = (StepResponse)stepContext.get(key);
//				responses.put(key, stepResponse.getResponse());
//			}
//			return responses;
//		} else {
//			return null;
//		}
//		
//		
//		
//	}
	
}
