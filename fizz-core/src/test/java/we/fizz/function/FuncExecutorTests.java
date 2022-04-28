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

import org.junit.jupiter.api.Test;

/**
 * 
 * @author Francis Dong
 *
 */
class FuncExecutorTests {
	
	@Test
	void contextLoads() {
	}


	@Test
	void testNest() {
		String funcExpression = "fn.codec.md5(fn.date.add(  fn.date.add(\"2021-07-09 22:44:55\", \"yyyy-MM-dd HH:mm:ss\", 1, fn.math.addExact(999,1 )   ), \"yyyy-MM-dd HH:mm:ss\", fn.math.addExact(0,1), 1000 ))";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(CodecFunc.getInstance().md5("2021-07-09 22:44:57"), result);
	}
	
	@Test
	void testNest2() {
		String funcExpression = "fn.string.toUpperCase(fn.codec.sha256(fn.string.concat(\"a\",\"b\",fn.string.toString(fn.date.timestamp()))))";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		System.out.println(result);
	}
	
	@Test
	void testNest3() {
		String funcExpression = "fn.string.toUpperCase(fn.string.concat(\"a\",\"b\", fn.string.concat())))";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals("AB", result);
	}
	
	@Test
	void testNest4() {
		String funcExpression = "fn.string.toUpperCase(fn.codec.sha256(fn.string.concat(\"a\",fn.string.concat(),\"b\")))";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		System.out.println(result);
	}
	
	@Test
	void testNest5() {
		String funcExpression = "fn.string.toUpperCase(fn.codec.sha256(fn.string.concat(\"a\",fn.string.toString(fn.date.timestamp()),fn.string.concat(fn.string.concat(fn.string.concat(fn.string.concat())), \"c\"),\"b\")))";
		Object result = FuncExecutor.getInstance().exec(null, funcExpression);
		System.out.println(result);
	}
	

}