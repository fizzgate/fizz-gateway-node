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

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSON;
import com.fizzgate.fizz.component.ComponentTypeEnum;
import com.fizzgate.fizz.component.IComponent;
import com.fizzgate.fizz.component.OperatorEnum;
import com.fizzgate.fizz.exception.FizzRuntimeException;
import com.fizzgate.fizz.field.RefDataTypeEnum;
import com.fizzgate.fizz.field.ValueTypeEnum;
import com.fizzgate.fizz.input.PathMapping;
import com.fizzgate.fizz.input.extension.request.RequestInput;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Condition component
 * 
 * @author Francis Dong
 *
 */
@Data
public class Condition implements IComponent {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Condition.class);

	private static final String type = ComponentTypeEnum.CONDITION.getCode();

	private String desc;

	private ConditionValue value1;

	private OperatorEnum operator;

	private ConditionValue value2;
	
	public Condition(String desc, ConditionValue value1, OperatorEnum operator, ConditionValue value2) {
		this.desc = desc;
		this.value1 = value1;
		this.operator = operator;
		this.value2 = value2;
	}

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
					break;
				}
				if (v1 instanceof Collection && !(v2 instanceof Collection)) {
					Collection coll1 = (Collection) v1;
					if (v2 instanceof Integer || v2 instanceof Long) {
						Long el = Long.valueOf(v2.toString());
						rs = containsLong(coll1, el);
					} else if (v2 instanceof Float || v2 instanceof Double) {
						Double el = Double.valueOf(v2.toString());
						rs = containsDouble(coll1, el);
					} else {
						rs = CollectionUtils.contains(coll1.iterator(), v2);
					}
				} else if (!(v1 instanceof Collection)) {
					throw new FizzRuntimeException("value1 must be a collection");
				} else if (v2 instanceof Collection) {
					throw new FizzRuntimeException("value2 can not be a collection");
				}
				break;
			case NOTCONTAIN:
				if (v1 == null) {
					rs = true;
					break;
				}
				if (v1 instanceof Collection && !(v2 instanceof Collection)) {
					Collection coll1 = (Collection) v1;
					if (v2 instanceof Integer || v2 instanceof Long) {
						Long el = Long.valueOf(v2.toString());
						rs = !containsLong(coll1, el);
					} else if (v2 instanceof Float || v2 instanceof Double) {
						Double el = Double.valueOf(v2.toString());
						rs = !containsDouble(coll1, el);
					} else {
						rs = !CollectionUtils.contains(coll1.iterator(), v2);
					}
				} else if (!(v1 instanceof Collection)) {
					throw new FizzRuntimeException("value1 must be a collection");
				} else if (v2 instanceof Collection) {
					throw new FizzRuntimeException("value2 can not be a collection");
				}
				break;
			case CONTAINSANY:
				if (v1 == null || v2 == null) {
					rs = false;
					break;
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
			case ISNULL:
				rs = v1 == null;
				break;
			case ISNOTNULL:
				rs = v1 != null;
				break;
			case ISBLANK:
				rs = v1 == null || StringUtils.isBlank(v1.toString());
				break;
			case ISNOTBLANK:
				rs = v1 != null && StringUtils.isNotBlank(v1.toString());
				break;
			case ISEMPTY:
				rs = v1 == null || (v1 instanceof Collection && ((Collection) v1).isEmpty())
						|| (v1 instanceof Map && ((Map) v1).isEmpty());
				break;
			case ISNOTEMPTY:
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
			LOGGER.error(message, e);
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
		if (type != null && val != null) {
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
	
	@SuppressWarnings("rawtypes")
	private boolean containsLong(Collection coll, Long el) {
		if (CollectionUtils.isEmpty(coll)) {
			return false;
		}
		
		for (Object obj : coll) {
			Long obj2 = null;
			if (obj instanceof Integer) {
				obj2 = Long.valueOf(obj.toString());
			}
			if (ObjectUtils.nullSafeEquals(obj2, el)) {
				return true;
			}
		}
		
		return false;
	}
	
	@SuppressWarnings("rawtypes")
	private boolean containsDouble(Collection coll, Double el) {
		if (CollectionUtils.isEmpty(coll)) {
			return false;
		}
		
		for (Object obj : coll) {
			Double obj2 = null;
			if (obj instanceof Float) {
				obj2 = Double.valueOf(obj.toString());
			}
			if (ObjectUtils.nullSafeEquals(obj2, el)) {
				return true;
			}
		}
		
		return false;
	}
}
