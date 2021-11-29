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

/**
 * Data type of reference value
 * 
 * @author Francis Dong
 *
 */
public enum RefDataTypeEnum {

	INT("int"), LONG("long"), FLOAT("float"), DOUBLE("double"), STRING("string"), BOOLEAN("boolean"), ARRAY("array");

	private String code;

	private RefDataTypeEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
	public static RefDataTypeEnum getEnumByCode(String code) {
		for (RefDataTypeEnum e : RefDataTypeEnum.values()) {
			if (e.getCode().equals(code)) {
				return e;
			}
		}
		return null;
	}
	
}
