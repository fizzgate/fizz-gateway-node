/*
 *  Copyright (C) 2021 the original author or authors.
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
package we.fizz.field;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author Francis Dong
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfig {

	private ValueTypeEnum type;

	private FixedDataTypeEnum fixedDataType;

	private RefDataTypeEnum refDataType;

	private Object value;

	public FieldConfig(Map<String, Object> configMap) {
		if (configMap != null && !configMap.isEmpty()) {
			if (configMap.containsKey("type")) {
				this.type = ValueTypeEnum.getEnumByCode(configMap.get("type").toString());
			}
			if (configMap.containsKey("fixedDataType")) {
				this.fixedDataType = FixedDataTypeEnum.getEnumByCode(configMap.get("fixedDataType").toString());
			}
			if (configMap.containsKey("refDataType")) {
				this.refDataType = RefDataTypeEnum.getEnumByCode(configMap.get("refDataType").toString());
			}
			if (configMap.containsKey("value")) {
				this.value = configMap.get("value");
			}
		}
	}
}
