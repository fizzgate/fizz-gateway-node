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
package we.fizz.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.validation.constraints.AssertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import we.fizz.component.condition.Condition;
import we.fizz.component.condition.ConditionValue;
import we.fizz.input.PathMapping;

/**
 * 
 * @author Francis Dong
 *
 */
class ConditionTests {
	@Test
	void contextLoads() {
	}

	private static final Boolean TRUE = true;
	private static final Boolean FALSE = false;

	@Test
	void testExec() {
		ONode ctxNode = ONode.load(new HashMap());

		Map<String, Object> m = new HashMap<>();
		m.put("int", 1);
		m.put("long", 2);
		m.put("float", 3.1);
		m.put("double", 4.21);
		m.put("string_abcd", "abcd");
		m.put("string_1", "1");
		m.put("string_8", "8");
		m.put("string_blank", "");
		m.put("bool_true", true);
		m.put("bool_false", false);

		List<String> list1 = new ArrayList<>();
		list1.add("0");
		list1.add("1");
		list1.add("2");
		list1.add("3");
		list1.add("4");

		List<String> list2 = new ArrayList<>();
		list2.add("1");
		list2.add("3");
		list2.add("223");

		List<Integer> intList = new ArrayList<>();
		intList.add(0);
		intList.add(1);
		intList.add(2);
		intList.add(3);
		intList.add(4);

		List<Float> floatList = new ArrayList<>();
		floatList.add(0f);
		floatList.add(1f);
		floatList.add(2f);
		floatList.add(3f);
		floatList.add(4f);

		PathMapping.setByPath(ctxNode, "data.m", m, true);
		PathMapping.setByPath(ctxNode, "data.list1", list1, true);
		PathMapping.setByPath(ctxNode, "data.list2", list2, true);
		PathMapping.setByPath(ctxNode, "data.intList", intList, true);
		PathMapping.setByPath(ctxNode, "data.floatList", floatList, true);
		PathMapping.setByPath(ctxNode, "data.emptyList", new ArrayList<>(), true);

		// boolean
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, TRUE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, TRUE, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, FALSE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, FALSE, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, TRUE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, FALSE, OperatorEnum.EQ, FALSE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, FALSE, ValueTypeEnum.REF,
				RefDataTypeEnum.BOOLEAN, "data.m.bool_true", OperatorEnum.EQ, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, FALSE, ValueTypeEnum.REF,
				RefDataTypeEnum.BOOLEAN, "data.m.bool_false", OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, TRUE, ValueTypeEnum.REF,
				RefDataTypeEnum.BOOLEAN, "data.m.bool_true", OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, FALSE, ValueTypeEnum.REF, null,
				"data.m.a", OperatorEnum.EQ, FALSE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, TRUE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, FALSE, OperatorEnum.GT, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, TRUE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, FALSE, OperatorEnum.GE, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, TRUE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, TRUE, OperatorEnum.GE, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, FALSE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, TRUE, OperatorEnum.LT, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.BOOLEAN, FALSE, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.BOOLEAN, FALSE, OperatorEnum.LE, TRUE });

		// number
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 1, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 2, OperatorEnum.EQ, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 1.000, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 2.1, OperatorEnum.EQ, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1.0, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 1.000, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1.1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 2.1, OperatorEnum.EQ, FALSE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1.1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 2.1, OperatorEnum.GT, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1.1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 0.1, OperatorEnum.GE, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1.1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 0.1, OperatorEnum.LT, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 1.1, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.NUMBER, 4, OperatorEnum.LT, TRUE });

		// collection<String>
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "2", OperatorEnum.CONTAINS, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_1", OperatorEnum.CONTAINS, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.CONTAINS, FALSE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "2", OperatorEnum.NOTCONTAIN, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_1", OperatorEnum.NOTCONTAIN, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.NOTCONTAIN, TRUE });

		// collection contains any
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.ARRAY, "data.list2", OperatorEnum.CONTAINSANY, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.ARRAY, "data.intList", OperatorEnum.CONTAINSANY, FALSE });

		// Collection<int>
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "2", OperatorEnum.CONTAINS, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.REF,
				RefDataTypeEnum.INT, "data.m.int", OperatorEnum.CONTAINS, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.FIXED,
				RefDataTypeEnum.INT, 2, OperatorEnum.CONTAINS, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.FIXED,
				RefDataTypeEnum.INT, 9, OperatorEnum.CONTAINS, FALSE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "2", OperatorEnum.NOTCONTAIN, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.REF,
				RefDataTypeEnum.INT, "data.m.int", OperatorEnum.NOTCONTAIN, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.FIXED,
				RefDataTypeEnum.INT, 2, OperatorEnum.NOTCONTAIN, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.intList", ValueTypeEnum.FIXED,
				RefDataTypeEnum.INT, 9, OperatorEnum.NOTCONTAIN, TRUE });

		// Collection<Float>
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.floatList",
				ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "2", OperatorEnum.CONTAINS, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.floatList", ValueTypeEnum.REF,
				RefDataTypeEnum.INT, "data.m.int", OperatorEnum.CONTAINS, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.floatList",
				ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 2, OperatorEnum.CONTAINS, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.floatList",
				ValueTypeEnum.FIXED, FixedDataTypeEnum.NUMBER, 2.0, OperatorEnum.CONTAINS, TRUE });

		// String
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, null, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, null, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, null, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, null, OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, null, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "1", OperatorEnum.EQ, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "1", OperatorEnum.EQ, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.EQ, FALSE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, null, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, null, OperatorEnum.NE, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, null, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, null, OperatorEnum.NE, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, null, ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "1", OperatorEnum.NE, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "1", OperatorEnum.NE, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.NE, TRUE });

		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "1", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.GT, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "11", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.GT, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "11", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.GE, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "11", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.LT, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "11", ValueTypeEnum.FIXED,
				FixedDataTypeEnum.STRING, "21", OperatorEnum.LE, TRUE });

		// Is null
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNULL, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8",
				ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNULL, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", null, null, null,
				OperatorEnum.ISNULL, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", null, null, null,
				OperatorEnum.ISNULL, FALSE });

		// Is not null
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNOTNULL, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8",
				ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNOTNULL, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", null, null, null,
				OperatorEnum.ISNOTNULL, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", null, null, null,
				OperatorEnum.ISNOTNULL, TRUE });

		// Is Blank
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISBLANK, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8",
				ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISBLANK, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", null, null, null,
				OperatorEnum.ISBLANK, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", null, null, null,
				OperatorEnum.ISBLANK, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_blank", null, null,
				null, OperatorEnum.ISBLANK, TRUE });

		// Is not Blank
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNOTBLANK, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8",
				ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNOTBLANK, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", null, null, null,
				OperatorEnum.ISNOTBLANK, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_8", null, null, null,
				OperatorEnum.ISNOTBLANK, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.STRING, "data.m.string_blank", null, null,
				null, OperatorEnum.ISNOTBLANK, FALSE });

		// Is empty
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISEMPTY, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISEMPTY, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", null, null, null,
				OperatorEnum.ISEMPTY, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", null, null, null,
				OperatorEnum.ISEMPTY, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.emptyList", null, null, null,
				OperatorEnum.ISEMPTY, TRUE });

		// Is not empty
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNOTEMPTY, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", ValueTypeEnum.REF,
				RefDataTypeEnum.STRING, "data.m.string_8", OperatorEnum.ISNOTEMPTY, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list11111", null, null, null,
				OperatorEnum.ISNOTEMPTY, FALSE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.list1", null, null, null,
				OperatorEnum.ISNOTEMPTY, TRUE });
		this.run(ctxNode, new Object[] { ValueTypeEnum.REF, RefDataTypeEnum.ARRAY, "data.emptyList", null, null, null,
				OperatorEnum.ISNOTEMPTY, FALSE });

	}

	private void run(ONode ctxNode, Object[] item) {
		ConditionValue bValue1 = null;
		if (item[1] instanceof FixedDataTypeEnum) {
			bValue1 = new ConditionValue((ValueTypeEnum) item[0], (FixedDataTypeEnum) item[1], item[2]);
		} else {
			bValue1 = new ConditionValue((ValueTypeEnum) item[0], (RefDataTypeEnum) item[1], item[2]);
		}
		ConditionValue bValue2 = null;
		if (item[3] != null) {
			if (item[4] instanceof FixedDataTypeEnum) {
				bValue2 = new ConditionValue((ValueTypeEnum) item[3], (FixedDataTypeEnum) item[4], item[5]);
			} else {
				bValue2 = new ConditionValue((ValueTypeEnum) item[3], (RefDataTypeEnum) item[4], item[5]);
			}
		}
		Condition c = new Condition(null, bValue1, (OperatorEnum) item[6], bValue2);
		boolean rs = c.exec(ctxNode);
		boolean expected = (boolean) item[7];
		assertEquals(expected, rs);
	}

}