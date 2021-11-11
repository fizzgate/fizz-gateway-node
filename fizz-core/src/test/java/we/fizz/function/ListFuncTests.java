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

import com.alibaba.fastjson.JSON;

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

	private Map<String, Object> createRecord3(int index, boolean isDest) {
		Map<String, Object> m = new HashMap<>();
		m.put("a", "a" + index);
		String s = isDest ? "" + index : index + "-abc";
		m.put("b", "b" + s);
		m.put("c", "c" + s);
		if (!isDest) {
			m.put("d", "d" + s);
			m.put("e", "e" + s);
		}
		return m;
	}

	@Test
	void testJoin() {
		List<Object> list1 = new ArrayList<>();
		list1.add(createRecord3(1, true));
		list1.add(createRecord3(2, true));
		list1.add(createRecord3(3, true));
		list1.add(createRecord3(4, true));
		list1.add(createRecord3(5, true));

		List<Object> list2 = new ArrayList<>();
		list2.add(createRecord3(1, false));
		list2.add(createRecord3(2, false));
		list2.add(createRecord3(3, false));
		list2.add(createRecord3(4, false));

		ONode ctxNode = ONode.load(new HashMap());
		PathMapping.setByPath(ctxNode, "test.list1", list1, true);
		PathMapping.setByPath(ctxNode, "test.list2", list2, true);

		String funcExpression = "fn.list.join({test.list1}, {test.list2},\"a\",\"c\",\"d\")";
		List<Object> result = (List<Object>) FuncExecutor.getInstance().exec(ctxNode, funcExpression);
		assertEquals(5, result.size());
		assertEquals("a2", ((Map<String, Object>) result.get(1)).get("a").toString());
		assertEquals("d4-abc", ((Map<String, Object>) result.get(3)).get("d").toString());
		// System.out.println(JSON.toJSONString(result));
		
		String json1 = "[\n"
				+ "        {\n"
				+ "            \"itemType\": \"3\",\n"
				+ "            \"currCd\": \"156\",\n"
				+ "            \"batchDate\": \"20190331\",\n"
				+ "            \"itemCd\": \"ORGAP0008\",\n"
				+ "            \"itemVal\": 0.7189,\n"
				+ "            \"itemNm\": \"balance rate\",\n"
				+ "            \"valueStr\": \"0.7189\",\n"
				+ "            \"orgCd1\": \"230009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"itemType\": \"3\",\n"
				+ "            \"currCd\": \"156\",\n"
				+ "            \"batchDate\": \"20190331\",\n"
				+ "            \"itemCd\": \"ORGAP0008\",\n"
				+ "            \"itemVal\": 0.7040,\n"
				+ "            \"itemNm\": \"balance rate\",\n"
				+ "            \"valueStr\": \"0.7040\",\n"
				+ "            \"orgCd1\": \"231009999\"\n"
				+ "        }\n"
				+ "    ]";
		
		String json2 = "[\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name1\",\n"
				+ "            \"orgCd\": \"230009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name2\",\n"
				+ "            \"orgCd\": \"231009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name3\",\n"
				+ "            \"orgCd\": \"232009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name3\",\n"
				+ "            \"orgCd\": \"233009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name4\",\n"
				+ "            \"orgCd\": \"234009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name5\",\n"
				+ "            \"orgCd\": \"235009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name6\",\n"
				+ "            \"orgCd\": \"236009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name7\",\n"
				+ "            \"orgCd\": \"237009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name8\",\n"
				+ "            \"orgCd\": \"238009999\"\n"
				+ "        },\n"
				+ "        {\n"
				+ "            \"orgName\": \"Name9\",\n"
				+ "            \"orgCd\": \"239009999\"\n"
				+ "        }\n"
				+ "    ]";
		
		
		ONode ctxNode2 = ONode.load(new HashMap());
		PathMapping.setByPath(ctxNode2, "test.list1", JSON.parseArray(json1, Map.class), true);
		PathMapping.setByPath(ctxNode2, "test.list2", JSON.parseArray(json2, Map.class), true);
		String funcExpression2 = "fn.list.join({test.list1},{test.list2},\"orgCd1:orgCd\")";
		List<Object> result2 = (List<Object>) FuncExecutor.getInstance().exec(ctxNode2, funcExpression2);
		System.out.println(JSON.toJSONString(result2));
		assertEquals("Name1", ((Map<String, Object>) result2.get(0)).get("orgName").toString());
	}
	
}