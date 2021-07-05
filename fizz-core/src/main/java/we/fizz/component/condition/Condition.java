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

package we.fizz.component.condition;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.noear.snack.ONode;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;

import lombok.AllArgsConstructor;
import lombok.Data;
import we.fizz.component.ComponentTypeEnum;
import we.fizz.component.IComponent;
import we.fizz.component.OperatorEnum;
import we.fizz.component.RefDataTypeEnum;
import we.fizz.component.ValueTypeEnum;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.PathMapping;

/**
 * Condition component
 * 
 * @author Francis Dong
 *
 */
@Data
@AllArgsConstructor
public class Condition implements IComponent {

	private static final String type = ComponentTypeEnum.CONDITION.getCode();

	private String desc;

	private ConditionValue value1;

	private OperatorEnum operator;

	private ConditionValue value2;

	@Override
	public ComponentTypeEnum getType() {
		return ComponentTypeEnum.getEnumByCode(type);
	}
	
	/**
	 * Execute condition
	 * 
	 * @return
	 */
	@SuppressWarnings({ "rawtypes" })
	public boolean exec(ONode ctxNode) {
		if (value1 == null || operator == null) {
			return false;
		}

		boolean rs = false;
		try {
			Object v1 = null;
			if (ValueTypeEnum.FIXED.equals(value1.getType())) {
				v1 = value1.getValue();
			} else {
				v1 = PathMapping.getValueByPath(ctxNode, (String) value1.getValue());
				v1 = this.cast(value1.getRefDataType(), v1);
			}

			Object v2 = null;
			if (value2 != null && value2.getType() != null) {
				if (ValueTypeEnum.FIXED.equals(value2.getType())) {
					v2 = value2.getValue();
				} else {
					v2 = PathMapping.getValueByPath(ctxNode, (String) value2.getValue());
					v2 = this.cast(value2.getRefDataType(), v2);
				}
			}

			switch (operator) {
			case EQ:
				if (v1 == null && v2 == null) {
					rs = true;
				} else if (v1 != null && v2 != null) {
					rs = this.compare(v1, v2) == 0;
				}
				break;
			case NE:
				if (v1 == null && v2 == null) {
					rs = false;
				} else if ((v1 == null && v2 != null) || (v1 != null && v2 == null)) {
					rs = true;
				} else if (v1 != null && v2 != null) {
					rs = this.compare(v1, v2) != 0;
				}
				break;
			case GT:
				rs = this.compare(v1, v2) > 0;
				break;
			case GE:
				rs = this.compare(v1, v2) >= 0;
				break;
			case LT:
				rs = this.compare(v1, v2) < 0;
				break;
			case LE:
				rs = this.compare(v1, v2) <= 0;
				break;
			case CONTAINS:
				if (v1 == null) {
					rs = false;
				}
				if (v1 instanceof Collection && !(v2 instanceof Collection)) {
					Collection coll1 = (Collection) v1;
					Object el = v2;
					if (v2 instanceof Integer || v2 instanceof Long) {
						el = Long.valueOf(v2.toString());
					} else if (v2 instanceof Float || v2 instanceof Double) {
						el = Double.valueOf(v2.toString());
					}
					rs = CollectionUtils.contains(coll1.iterator(), el);
				} else if (!(v1 instanceof Collection)) {
					throw new FizzRuntimeException("value1 must be a collection");
				} else if (v2 instanceof Collection) {
					throw new FizzRuntimeException("value2 can not be a collection");
				}
				break;
			case NOT_CONTAIN:
				if (v1 == null) {
					rs = true;
				}
				if (v1 instanceof Collection && !(v2 instanceof Collection)) {
					Collection coll1 = (Collection) v1;
					Object el = v2;
					if (v2 instanceof Integer || v2 instanceof Long) {
						el = Long.valueOf(v2.toString());
					} else if (v2 instanceof Float || v2 instanceof Double) {
						el = Double.valueOf(v2.toString());
					}
					rs = !CollectionUtils.contains(coll1.iterator(), el);
				} else if (!(v1 instanceof Collection)) {
					throw new FizzRuntimeException("value1 must be a collection");
				} else if (v2 instanceof Collection) {
					throw new FizzRuntimeException("value2 can not be a collection");
				}
				break;
			case CONTAINS_ANY:
				if (v1 == null || v2 == null) {
					rs = false;
				}
				if (v1 instanceof Collection && v2 instanceof Collection) {
					Collection coll1 = (Collection) v1;
					Collection coll2 = (Collection) v2;
					rs = CollectionUtils.containsAny(coll1, coll2);
				} else if (!(v1 instanceof Collection)) {
					throw new FizzRuntimeException("value1 must be a collection");
				} else if (!(v2 instanceof Collection)) {
					throw new FizzRuntimeException("value2 must be a collection");
				}
				break;
			case IS_NULL:
				rs = v1 == null;
				break;
			case IS_NOT_NULL:
				rs = v1 != null;
				break;
			case IS_BLANK:
				rs = v1 == null || StringUtils.isBlank(v1.toString());
				break;
			case IS_NOT_BLANK:
				rs = v1 != null && StringUtils.isNotBlank(v1.toString());
				break;
			case IS_EMPTY:
				rs = v1 == null || (v1 instanceof Collection && ((Collection) v1).isEmpty())
						|| (v1 instanceof Map && ((Map) v1).isEmpty());
				break;
			case IS_NOT_EMPTY:
				if (v1 != null) {
					if (v1 instanceof Collection) {
						rs = !((Collection) v1).isEmpty();
					} else if (v1 instanceof Map) {
						rs = !((Map) v1).isEmpty();
					}
				}
				break;
			default:
				break;
			}
		} catch (FizzRuntimeException e) {
			String message = type + ": " + e.getMessage() + ", data=" + JSON.toJSONString(this);
			throw new FizzRuntimeException(message, e.getCause());
		}

		return rs;
	}

	@SuppressWarnings("rawtypes")
	private int compare(Object v1, Object v2) {
		if (v1 == null || v2 == null) {
			throw new FizzRuntimeException("value1 and value2 can not be null");
		}
		if (v1 instanceof Boolean && v2 instanceof Boolean) {
			Boolean n1 = (Boolean) v1;
			Boolean n2 = (Boolean) v2;
			return n1.compareTo(n2);
		} else if ((v1 instanceof Integer || v1 instanceof Long || v1 instanceof Float || v1 instanceof Double)
				&& (v2 instanceof Integer || v2 instanceof Long || v2 instanceof Float || v2 instanceof Double)) {
			// compare value if both are numbers
			Double n1 = Double.valueOf(v1.toString());
			Double n2 = Double.valueOf(v2.toString());
			return n1.compareTo(n2);
		} else if (v1 instanceof String && v2 instanceof String) {
			String s1 = v1.toString();
			String s2 = v2.toString();
			return s1.compareTo(s2);
		} else {
			throw new FizzRuntimeException(
					"types of value1 and value2 are not consistent or not supported for comparision");
		}
	}

	private Object cast(RefDataTypeEnum type, Object val) {
		if (type != null) {
			switch (type) {
			case INT:
				val = Integer.valueOf(val.toString());
				break;
			case LONG:
				val = Long.valueOf(val.toString());
				break;
			case FLOAT:
				val = Float.valueOf(val.toString());
				break;
			case DOUBLE:
				val = Double.valueOf(val.toString());
				break;
			case BOOLEAN:
				val = Boolean.valueOf(val.toString());
				break;
			case STRING:
				val = val.toString();
				break;
			}
		}
		return val;
	}
}
