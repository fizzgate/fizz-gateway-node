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
package we.fizz.function;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import we.fizz.input.PathMapping;

/**
 * 
 * @author Francis Dong
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class ListFuncTests {
	@Test
	void contextLoads() {
	}

	private Map<String, Object> createRecord(String key, Object value) {
		Map<String, Object> m = new HashMap<>();
		m.put(key, value);
		return m;
	}
	
	private Map<String, Object> createRecord2(int index) {
		Map<String, Object> m = new HashMap<>();
		m.put("a", "a" + index);
		m.put("b", "b" + index);
		m.put("c", "c" + index);
		m.put("d", "d" + index);
		m.put("e", "e" + index);
		return m;
	}

	@Test
	void testExpand() {
		List<List<Object>> data = new ArrayList<>();

		List<Object> subList1 = new ArrayList<>();
		subList1.add(createRecord("a", "a1"));
		subList1.add(createRecord("a", "a2"));
		subList1.add(createRecord("a", "a3"));

		List<Object> subList2 = new ArrayList<>();
		subList2.add(createRecord("a", "a4"));
		subList2.add(createRecord("a", "a5"));
		subList2.add(createRecord("a", "a6"));

		data.add(subList1);
		data.add(subList2);

		ONode ctxNode = ONode.load(new HashMap());
		PathMapping.setByPath(ctxNode, "test.data", data, true);

		String funcExpression = "fn.list.expand({test.data})";
		List<Object> result = (List<Object>) FuncExecutor.getInstance().exec(ctxNode, funcExpression);
		assertEquals(6, result.size());
		assertEquals("a2", ((Map<String, Object>) result.get(1)).get("a").toString());
		assertEquals("a4", ((Map<String, Object>) result.get(3)).get("a").toString());
	}
	
	
	@Test
	void testMerge() {
		List<Object> subList1 = new ArrayList<>();
		subList1.add(createRecord("a", "a1"));
		subList1.add(createRecord("a", "a2"));
		subList1.add(createRecord("a", "a3"));

		List<Object> subList2 = new ArrayList<>();
		subList2.add(createRecord("a", "a4"));
		subList2.add(createRecord("a", "a5"));
		subList2.add(createRecord("a", "a6"));


		ONode ctxNode = ONode.load(new HashMap());
		PathMapping.setByPath(ctxNode, "test.data1", subList1, true);
		PathMapping.setByPath(ctxNode, "test.data2", subList2, true);

		String funcExpression = "fn.list.merge({test.data1}, {test.data2})";
		List<Object> result = (List<Object>) FuncExecutor.getInstance().exec(ctxNode, funcExpression);
		assertEquals(6, result.size());
		assertEquals("a2", ((Map<String, Object>) result.get(1)).get("a").toString());
		assertEquals("a4", ((Map<String, Object>) result.get(3)).get("a").toString());
	}
	
	@Test
	void testExtract() {
		List<Object> subList1 = new ArrayList<>();
		subList1.add(createRecord2(1));
		subList1.add(createRecord2(2));
		subList1.add(createRecord2(3));
		subList1.add(createRecord2(4));
		subList1.add(createRecord2(5));


		ONode ctxNode = ONode.load(new HashMap());
		PathMapping.setByPath(ctxNode, "test.data", subList1, true);

		String funcExpression = "fn.list.extract({test.data}, \"c\",\"b\",  \"e\")";
		List<Object> result = (List<Object>) FuncExecutor.getInstance().exec(ctxNode, funcExpression);
		assertEquals(5, result.size());
		assertEquals("c2", ((Map<String, Object>) result.get(1)).get("c").toString());
		assertEquals("e4", ((Map<String, Object>) result.get(3)).get("e").toString());
		assertEquals(null, ((Map<String, Object>) result.get(3)).get("a"));
		assertEquals(null, ((Map<String, Object>) result.get(3)).get("d"));
//		System.out.println(result);
	}

}