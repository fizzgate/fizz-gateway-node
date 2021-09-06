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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.noear.snack.ONode;

import we.fizz.input.PathMapping;

/**
 * 
 * @author Francis Dong
 *
 */
class StringFuncTests {
	
	@Test
	void contextLoads() {
	}


	@Test
	void testEquals() {
		String funcExpression = "fn.string.equals(\"abc\", \"abc\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(true, result);
	}
	
	@Test
	void testEquals2() {
		String funcExpression = "fn.string.equals(null, \"abc\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(false, result);
	}
	
	@Test
	void testEquals3() {
		String funcExpression = "fn.string.equals(\"ab\\\"c\", \"abc\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(false, result);
	}
	
	@Test
	void testEqualsIgnoreCase() {
		String funcExpression = "fn.string.equalsIgnoreCase(\"abc\", \"Abc\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(true, result);
	}
	
	@Test
	void testcompare() {
		String funcExpression = "fn.string.compare(\"abc\", \"cde\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(-1, result);
	}
	
	@Test
	void testConcat() {
		String funcExpression = "fn.string.concat(\"2021-07-09 22:44:55\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55", result.toString());
	}
	
	
	@Test
	void testConcat2() {
		String funcExpression = "fn.string.concat(\"2021-07-09 22:44:55\", \"yyyy-MM-dd HH:mm:ss\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55yyyy-MM-dd HH:mm:ss", result.toString());
	}
	
	@Test
	void testConcat3() {
		String funcExpression = "fn.string.concat(\"2021-07-09 22:44:55\", \"yyyy-MM-dd HH:mm:ss\",1)";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55yyyy-MM-dd HH:mm:ss1", result.toString());
	}
	
	@Test
	void testConcatws() {
		String funcExpression = "fn.string.concatws(\",\" ,  \"2021-07-09 22:44:55\", \"yyyy-MM-dd HH:mm:ss\")";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55,yyyy-MM-dd HH:mm:ss", result.toString());
	}
	
	@Test
	void testSubstring() {
		String funcExpression = "fn.string.substring(\"2021-07-09 22:44:55\",  1  ,   4)";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55".substring(1, 4), result.toString());
	}
	
	@Test
	void testSubstring2() {
		ONode ctxNode = ONode.load(new HashMap());
		
		Map<String, Object> m = new HashMap<>();
		m.put("a", "1");
		m.put("b", "1");
		
		PathMapping.setByPath(ctxNode, "data.dateStr", "2021-07-09 22:44:55", true);
		PathMapping.setByPath(ctxNode, "data.startIndex", 1, true);
		PathMapping.setByPath(ctxNode, "data", m, false);
		
		String funcExpression = "fn.string.substring({data.dateStr},  {data.startIndex})";
//		String funcExpression = "fn.string.substring(\"2021-07-09 22:44:55\",  1)";
		Object result = FuncExecutor.getInstance().exec(ctxNode, funcExpression);
		assertEquals("2021-07-09 22:44:55".substring(1), result.toString());
	}
	
	@Test
	void testIndexOf() {
		String funcExpression = "fn.string.indexOf(\"2021-07-09 22:44:55\",  \"07\")";
		int result = (int)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55".indexOf("07"), result);
	}
	
	@Test
	void testStartsWith() {
		String funcExpression = "fn.string.startsWith(\"2021-07-09 22:44:55\",  \"2021\")";
		boolean result = (boolean)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55".startsWith("2021"), result);
	}
	
	@Test
	void testEndsWith() {
		String funcExpression = "fn.string.endsWith(\"2021-07-09 22:44:55\",  \"44:55\")";
		boolean result = (boolean)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("2021-07-09 22:44:55".endsWith("44:55"), result);
	}
	
	@Test
	void testToUpperCase() {
		String funcExpression = "fn.string.toUpperCase(\"aBc\")";
		String result = (String)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("aBc".toUpperCase(), result);
	}
	
	@Test
	void testToLowerCase() {
		String funcExpression = "fn.string.toLowerCase(\"aBc\")";
		String result = (String)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("aBc".toLowerCase(), result);
	}
	
	@Test
	void testToString() {
		String funcExpression = "fn.string.toString(\"aBc\")";
		String result = (String)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("aBc", result);
	}
	
	@Test
	void testToString2() {
		String funcExpression = "fn.string.toString(true)";
		String result = (String)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("true", result);
	}
	
	@Test
	void testToString3() {
		String funcExpression = "fn.string.toString(234)";
		String result = (String)FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("234", result);
	}

}