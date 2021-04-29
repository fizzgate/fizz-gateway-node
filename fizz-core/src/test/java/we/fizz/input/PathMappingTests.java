package we.fizz.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;


class PathMappingTests {
	@Test
	void contextLoads() {
	}
	
	@Test
	void testSetByPath() {
		
		ONode target = ONode.load(new HashMap());
		
		Map<String, Object> m = new HashMap<>();
		m.put("a", "1");
		m.put("b", "1");
		
		PathMapping.setByPath(target, "data.id", "2", true);
		PathMapping.setByPath(target, "data", m, false);
		
		assertEquals("1", target.get("data").get("a").getString());
		assertEquals("1", target.get("data").get("b").getString());
		assertEquals("2", target.get("data").get("id").getString());
		
		
		List<String> list = new ArrayList<>();
		list.add("YYYY");
		PathMapping.setByPath(target, "data.zzz", list, true);
		
		List<String> list2 = new ArrayList<>();
		list2.add("XXXX");
		PathMapping.setByPath(target, "data.zzz", list2, true);
		
		
		List<String> actualList = (List<String>) target.get("data").get("zzz").toData();
		assertTrue(actualList.contains("YYYY"));
		assertTrue(actualList.contains("XXXX"));
		
		
		List<String> list3 = new ArrayList<>();
		list3.add("vvvv");
		PathMapping.setByPath(target, "data.ppp", list3, true);
		
		
		Map<String, Object> m3 = new HashMap<>();
		m3.put("sss", "sss");
		PathMapping.setByPath(target, "data.ppp", m3, true);
		
		List<String> list4 = new ArrayList<>();
		list4.add("kkk");
		PathMapping.setByPath(target, "data.ppp", list4, true);
		
		List<String> actualList2 = (List<String>) target.get("data").get("ppp").toData();
		assertTrue(actualList2.contains("kkk"));
		
		
	}
	

	@Test
	void testHandlePath() {
		LinkedHashMap<String,String> pathMap = new LinkedHashMap<>();
		pathMap.put("step1.request1.request.headers", "step1.requests.request1.request.headers");
		pathMap.put("step1.request1.requestHeaders", "step1.requests.request1.request.headers");
		pathMap.put("step1.requests.request1.requestHeaders", "step1.requests.request1.request.headers");
		
		pathMap.put("step1.request1.request.params", "step1.requests.request1.request.params");
		pathMap.put("step1.request1.requestParams", "step1.requests.request1.request.params");
		pathMap.put("step1.requests.request1.requestParams", "step1.requests.request1.request.params");
		
		pathMap.put("step1.request1.request.body", "step1.requests.request1.request.body");
		pathMap.put("step1.request1.requestBody", "step1.requests.request1.request.body");
		pathMap.put("step1.requests.request1.requestBody", "step1.requests.request1.request.body");
		
		pathMap.put("step1.request1.response.headers", "step1.requests.request1.response.headers");
		pathMap.put("step1.request1.responseHeaders", "step1.requests.request1.response.headers");
		pathMap.put("step1.requests.request1.responseHeaders", "step1.requests.request1.response.headers");
		
		pathMap.put("step1.request1.response.body", "step1.requests.request1.response.body");
		pathMap.put("step1.request1.responseBody", "step1.requests.request1.response.body");
		pathMap.put("step1.requests.request1.responseBody", "step1.requests.request1.response.body");
		
		pathMap.put("input.requestHeaders", "input.request.headers");
		pathMap.put("input.requestParams", "input.request.params");
		pathMap.put("input.requestBody", "input.request.body");
		pathMap.put("input.responseHeaders", "input.response.headers");
		pathMap.put("input.responseBody", "input.response.body");
		
		pathMap.put("step1.request1.request.headers.Aa", "step1.requests.request1.request.headers.AA");
		pathMap.put("step1.request1.response.headers.aa", "step1.requests.request1.response.headers.AA");
		pathMap.put("input.requestHeaders.aa", "input.request.headers.AA");
		pathMap.put("input.responseHeaders.aA", "input.response.headers.AA");
		
		
		for (Entry<String, String> entry : pathMap.entrySet()) {
			Assertions.assertEquals(entry.getValue(), PathMapping.handlePath(entry.getKey()));
		}
	}
	
	@Test
	void testSelect() {
		Map<String,Object> m = new HashMap<>();
		ONode ctxNode = PathMapping.toONode(m);
		PathMapping.setByPath(ctxNode, "step1.requests.request1.request.headers.abc", "1", true);
		PathMapping.setByPath(ctxNode, "step1.requests.request1.request.headers.name1", "ken", true);
		PathMapping.setByPath(ctxNode, "input.request.headers.abc", "1", true);
		PathMapping.setByPath(ctxNode, "input.request.headers.name1", "ken", true);
		List<String> vals = new ArrayList<>();
		vals.add("Ken");
		vals.add("Kelly");
		PathMapping.setByPath(ctxNode, "step1.requests.request1.request.headers.name2", vals, true);
		PathMapping.setByPath(ctxNode, "input.request.headers.name2", vals, true);
		
		
		
		ONode abc = PathMapping.select(ctxNode, "step1.requests.request1.request.headers.abc");
		ONode name1 = PathMapping.select(ctxNode, "step1.requests.request1.request.headers.name1");
		ONode inputAbc = PathMapping.select(ctxNode, "input.request.headers.abc");
		ONode inputAbcName1 = PathMapping.select(ctxNode, "input.request.headers.name1");
		ONode name2 = PathMapping.select(ctxNode, "step1.requests.request1.request.headers.name2");
		ONode inputAbcName2 = PathMapping.select(ctxNode, "input.request.headers.name2");
		assertEquals("1", (String)abc.toData());
		assertEquals("ken", (String)name1.toData());
		assertEquals("1", (String)inputAbc.toData());
		assertEquals("ken", (String)inputAbcName1.toData());
		assertEquals(2, ((List)name2.toData()).size());
		assertEquals(2, ((List)inputAbcName2.toData()).size());

		abc = PathMapping.select(ctxNode, "step1.requests.request1.request.headers.abc[0]");
		name1 = PathMapping.select(ctxNode, "step1.requests.request1.request.headers.name1[0]");
		inputAbc = PathMapping.select(ctxNode, "input.request.headers.abc[0]");
		inputAbcName1 = PathMapping.select(ctxNode, "input.request.headers.name1[0]");
		name2 = PathMapping.select(ctxNode, "step1.requests.request1.request.headers.name2[0]");
		inputAbcName2 = PathMapping.select(ctxNode, "input.request.headers.name2[0]");
		assertEquals("1", (String)abc.toData());
		assertEquals("ken", (String)name1.toData());
		assertEquals("1", (String)inputAbc.toData());
		assertEquals("ken", (String)inputAbcName1.toData());
		assertEquals("Ken", (String)name2.toData());
		assertEquals("Ken", (String)inputAbcName2.toData());
		
		PathMapping.setByPath(ctxNode, "step1.requests.request1.request.headers.TEST", "1", true);
		Object abcVal1 = PathMapping.getValueByPath(ctxNode, "step1.requests.request1.request.headers.test[0]|123");
		Object abcVal2 = PathMapping.getValueByPath(ctxNode, "step1.requests.request1.request.headers.test[3]|123456");
		assertEquals("1", (String)abcVal1);
		assertEquals("123456", (String)abcVal2);

		
	}
	
	
}