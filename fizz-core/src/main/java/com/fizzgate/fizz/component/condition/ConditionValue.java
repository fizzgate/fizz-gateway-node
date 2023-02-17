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

package com.fizzgate.fizz.component.condition;

import com.fizzgate.fizz.field.FixedDataTypeEnum;
import com.fizzgate.fizz.field.RefDataTypeEnum;
import com.fizzgate.fizz.field.ValueTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Condition value
 * 
 * @author Francis Dong
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConditionValue {

	private ValueTypeEnum type;

	private FixedDataTypeEnum fixedDataType;

	private RefDataTypeEnum refDataType;

	private Object value;

	public ConditionValue(ValueTypeEnum type, FixedDataTypeEnum fixedDataType, Object value) {
		this.type = type;
		this.fixedDataType = fixedDataType;
		this.value = value;
	}
	
	public ConditionValue(ValueTypeEnum type, RefDataTypeEnum refDataType, Object value) {
		this.type = type;
		this.refDataType = refDataType;
		this.value = value;
	}

}
