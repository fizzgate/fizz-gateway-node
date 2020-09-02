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

package com.wehotel.fizz;

import java.util.HashMap;
import java.util.Map;

/**
 *  @author linwaiwai
 */
public class StepResponse {
	private String stepName;
	private Map<String, Map<String, Object>> requests;
	private Map result;
	private boolean stop;
	
	public StepResponse(Step aStep, HashMap item, Map<String, Map<String, Object>> requests) {
		setStepName(aStep.getName());
		setResult(item);
		setRequests(requests);
	}
	public StepResponse(Step aStep, HashMap item) {
		setStepName(aStep.getName());
		setResult(item);
	}
	
	public boolean isStop() {
		return stop;
	}
	public void setStop(boolean stop) {
		this.stop = stop;
	}
	public String getStepName() {
		return stepName;
	}
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}
	public Map<String, Map<String, Object>> getRequests() {
		return requests;
	}
	public void setRequests(Map<String, Map<String, Object>> requests) {
		this.requests = requests;
	}
	public Map getResult() {
		return result;
	}
	public void setResult(Map result) {
		this.result = result;
	}

}
