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

import java.util.Map;

/**
 * 
 * @author linwaiwai
 *
 */
public class InputFactory {
	public static InputConfig createInputConfig(Map config) {
		String type = (String) config.get("type");
		InputType typeEnum = InputType.valueOf(type.toUpperCase());
		InputConfig inputConfig = null;
		switch(typeEnum) {
			case REQUEST:
				inputConfig = new RequestInputConfig(config);
				
				break;
			case MYSQL:
				inputConfig = new MySQLInputConfig(config);
				break;
		}
		inputConfig.setType(typeEnum);
		inputConfig.setDataMapping((Map<String, Object>) config.get("dataMapping"));
		
		return inputConfig;
	}
	
	public static Input createInput(String type) {
		InputType typeEnum = InputType.valueOf(type.toUpperCase());
		Input input = null;
		switch(typeEnum) {
			case REQUEST:
				input = new RequestInput();
				break;
			case MYSQL:
				input = new MySQLInput();
				break;
		}
		
		return input;
	}

}
