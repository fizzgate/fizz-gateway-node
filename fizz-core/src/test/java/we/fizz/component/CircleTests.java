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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import we.fizz.StepContext;
import we.fizz.component.circle.Circle;
import we.fizz.component.circle.CircleItem;
import we.fizz.component.condition.Condition;
import we.fizz.component.condition.ConditionValue;
import we.fizz.field.FixedDataTypeEnum;
import we.fizz.field.RefDataTypeEnum;
import we.fizz.field.ValueTypeEnum;
/**
 * 
 * @author Francis Dong
 *
 */
import we.fizz.input.PathMapping;

class CircleTests {
	@Test
	void contextLoads() {
	}

	@SuppressWarnings("rawtypes")
	@Test
	void testNextFixedDataSource() {
		ONode ctxNode = ONode.load(new HashMap());

		// FIXED data source
		Circle c = new Circle(null, ValueTypeEnum.FIXED, 3, null, null);
		CircleItem circleItem = c.next(ctxNode);
		assertEquals(1, (Integer) circleItem.getItem());

		circleItem = c.next(ctxNode);
		assertEquals(2, (Integer) circleItem.getItem());

		circleItem = c.next(ctxNode);
		assertEquals(3, (Integer) circleItem.getItem());

		circleItem = c.next(ctxNode);
		assertEquals(null, circleItem);

	}

	@Test
	void testNextRefDataSource() {
		ONode ctxNode = ONode.load(new HashMap());

		List<String> list1 = new ArrayList<>();
		list1.add("1");
		list1.add("2");
		list1.add("3");
		PathMapping.setByPath(ctxNode, "data.list1", list1, true);

		// REF data source
		Circle c = new Circle(null, ValueTypeEnum.REF, "data.list1", null, null);
		CircleItem circleItem = c.next(ctxNode);
		assertEquals("1", (String) circleItem.getItem());

		circleItem = c.next(ctxNode);
		assertEquals("2", (String) circleItem.getItem());

		circleItem = c.next(ctxNode);
		assertEquals("3", (String) circleItem.getItem());

		circleItem = c.next(ctxNode);
		assertEquals(null, circleItem);

	}

	@Test
	void testExecCondition() {
		ONode ctxNode = ONode.load(new HashMap());

		List<String> list1 = new ArrayList<>();
		list1.add("0");
		list1.add("1");
		list1.add("2");
		list1.add("3");
		list1.add("4");

		PathMapping.setByPath(ctxNode, "data.list1", list1, true);

		ConditionValue bValue1 = new ConditionValue(ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "3");
		ConditionValue bValue2 = new ConditionValue(ValueTypeEnum.REF, RefDataTypeEnum.STRING, "item");
		Condition c1 = new Condition(null, bValue1, OperatorEnum.NE, bValue2);

		List<Condition> execConditions = new ArrayList<>();
		execConditions.add(c1);

		Circle circle = new Circle(null, ValueTypeEnum.REF, "data.list1", execConditions, null);

		for (int i = 0; i < 5; i++) {
			CircleItem circleItem = circle.next(ctxNode);
			PathMapping.setByPath(ctxNode, "item", circleItem.getItem(), true);
			PathMapping.setByPath(ctxNode, "index", circleItem.getIndex(), true);
			boolean rs = circle.canExec(circleItem.getIndex(), ctxNode, new StepContext<String, Object>(),
					new StepContextPosition("step1", null));
			assertEquals(i, circleItem.getIndex());
			if (i < 3) {
				assertEquals(true, rs);
			}
			if (i == 3) {
				assertEquals(false, rs);
				break;
			}
		}

	}

	@Test
	void testBreakCondition() {
		ONode ctxNode = ONode.load(new HashMap());

		List<String> list1 = new ArrayList<>();
		list1.add("0");
		list1.add("1");
		list1.add("2");
		list1.add("3");
		list1.add("4");

		PathMapping.setByPath(ctxNode, "data.list1", list1, true);

		ConditionValue bValue1 = new ConditionValue(ValueTypeEnum.FIXED, FixedDataTypeEnum.STRING, "3");
		ConditionValue bValue2 = new ConditionValue(ValueTypeEnum.REF, RefDataTypeEnum.STRING, "item");
		Condition c1 = new Condition(null, bValue1, OperatorEnum.EQ, bValue2);

		List<Condition> breakConditions = new ArrayList<>();
		breakConditions.add(c1);

		Circle circle = new Circle(null, ValueTypeEnum.REF, "data.list1", null, breakConditions);

		for (int i = 0; i < 5; i++) {
			CircleItem circleItem = circle.next(ctxNode);
			PathMapping.setByPath(ctxNode, "item", circleItem.getItem(), true);
			PathMapping.setByPath(ctxNode, "index", circleItem.getIndex(), true);

			boolean rs = circle.breakCircle(circleItem.getIndex(), ctxNode, new StepContext<String, Object>(),
					new StepContextPosition("step1", null));
			assertEquals(i, circleItem.getIndex());
			if (i < 3) {
				assertEquals(false, rs);
			}
			if (i == 3) {
				assertEquals(true, rs);
				break;
			}
		}

	}

}