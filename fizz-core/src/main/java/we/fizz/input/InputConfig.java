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

package we.fizz.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import we.fizz.component.IComponent;

/**
 * 
 * @author linwaiwai
 *
 */
public class InputConfig {

	private InputType type;
	protected Map<String, Object> dataMapping;
	protected Map<String, Object> configMap;
	private Map<String, Object> condition;
	private List<IComponent> components;

	public Map<String, Object> getCondition() {
		return condition;
	}

	public void setCondition(Map<String, Object> condition) {
		this.condition = condition;
	}
	
	public InputConfig(Map aConfigMap) {
		configMap = aConfigMap;
	}

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

	private Map<String,String> fallback = new HashMap<String, String>();

	public Map<String, String> getFallback() {
		return fallback;
	}

	public void setFallback(Map<String, String> fallback) {
		this.fallback = fallback;
	}

	public List<IComponent> getComponents() {
		return components;
	}

	public void setComponents(List<IComponent> components) {
		this.components = components;
	}

	public void parse(){

	}

}
