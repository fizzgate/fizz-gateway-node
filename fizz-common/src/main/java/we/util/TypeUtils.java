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

package we.util;

import java.math.BigDecimal;

/**
 * @author Francis Dong
 */

public abstract class TypeUtils {

	public static boolean isBasicType(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof String) {
			return true;
		}
		if (obj instanceof Integer) {
			return true;
		}
		if (obj instanceof Long) {
			return true;
		}
		if (obj instanceof Double) {
			return true;
		}
		if (obj instanceof Float) {
			return true;
		}
		if (obj instanceof Boolean) {
			return true;
		}
		if (obj instanceof Byte) {
			return true;
		}
		if (obj instanceof Short) {
			return true;
		}
		if (obj instanceof Character) {
			return true;
		}
		if (obj instanceof BigDecimal) {
			return true;
		}
		return false;
	}

}
