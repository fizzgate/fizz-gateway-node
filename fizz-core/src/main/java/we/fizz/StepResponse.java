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
import java.util.List;
import java.util.Map;

/**
 *  @author linwaiwai
 */
public class StepResponse {
	private String stepName;
	private Map<String, Map<String, Object>> requests;
	private Map result;
	private boolean stop;
	// circle item
	private Object item;
	// index of circle item
	private Integer index;
	// circle results
	private List<Map<String,Object>> circle;
	// result of condition components
	private List<Map<String,Object>> conditionResults;
	
	public StepResponse(Step aStep, HashMap item, Map<String, Map<String, Object>> requests) {
		setStepName(aStep.getName());
		setResult(item);
		setRequests(requests);
	}
	public StepResponse(Step aStep, HashMap item) {
		setStepName(aStep.getName());
		setResult(item);
	}
	
	public void addRequest(String requestName, Map<String, Object> requestObj) {
		if (this.requests.containsKey(requestName)) {
			this.requests.get(requestName).putAll(requestObj);
		} else {
			this.requests.put(requestName, requestObj);
		}
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
	public Object getItem() {
		return item;
	}
	public void setItem(Object item) {
		this.item = item;
	}
	public List<Map<String, Object>> getCircle() {
		return circle;
	}
	public void setCircle(List<Map<String, Object>> circle) {
		this.circle = circle;
	}
	public Integer getIndex() {
		return index;
	}
	public void setIndex(Integer index) {
		this.index = index;
	}
	public List<Map<String, Object>> getConditionResults() {
		return conditionResults;
	}
	public void setConditionResults(List<Map<String, Object>> conditionResults) {
		this.conditionResults = conditionResults;
	}

}
