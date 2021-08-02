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

import we.fizz.component.ComponentHelper;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.extension.request.RequestInput;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author linwaiwai
 *
 */
public class InputFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(InputFactory.class);
	public static Map<InputType, Class> inputClasses = new HashMap<InputType, Class>();
	public static void registerInput(InputType type, Class inputClass){
		inputClasses.put(type, inputClass);
	}
	public static void unregisterInput(InputType type){
		inputClasses.remove(type);
	}
	public static InputConfig createInputConfig(Map config)  {
		String type = (String) config.get("type");
		InputType typeEnum = InputType.valueOf(type.toUpperCase());
		InputConfig inputConfig = null;
		if (inputClasses.containsKey(typeEnum)){
			Class<?> InputClass = inputClasses.get(typeEnum);

			try {
				Method inputConfigClassMethod = InputClass.getMethod("inputConfigClass");
				Class<?> InputConfigClass = (Class<?>) inputConfigClassMethod.invoke(null);
				Constructor constructor = null;
				constructor = InputConfigClass.getDeclaredConstructor(Map.class);
				constructor.setAccessible(true);
				inputConfig =  (InputConfig) constructor.newInstance(config);
			} catch (Exception e) {
				LOGGER.error("failed to create input config, error: {}", e.getMessage(), e);
				throw new FizzRuntimeException("failed to create input config, message: " + e.getMessage(), e);
			}
			inputConfig.setType(typeEnum);
			inputConfig.setDataMapping((Map<String, Object>) config.get("dataMapping"));
			inputConfig.setComponents(ComponentHelper.buildComponents((List<Map<String, Object>>) config.get("components")));
			inputConfig.parse();
			return inputConfig;
		}
		return null;
	}
	
	public static Input createInput(String type) {
		InputType typeEnum = InputType.valueOf(type.toUpperCase());
		Input input = null;
		if (inputClasses.containsKey(typeEnum)) {
			Class<?> InputClass = inputClasses.get(typeEnum);
			Constructor constructor = null;
			try {
				constructor = InputClass.getDeclaredConstructor();
				constructor.setAccessible(true);
				input =  (Input) constructor.newInstance();
				return input;
			} catch (Exception e) {
				LOGGER.error("failed to create input config, error: {}", e.getMessage(), e);
				throw new FizzRuntimeException("failed to create input config, message: " + e.getMessage(), e);
			}
		}
		return null;
	}

}
