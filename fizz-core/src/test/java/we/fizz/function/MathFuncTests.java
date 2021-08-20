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
class MathFuncTests {
	@Test
	void contextLoads() {
	}
	
	@Test
	void testAbsExact() {
		String funcExpression = "fn.math.absExact(-3)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(3, result);
	}
	
	@Test
	void testNegateExact() {
		String funcExpression = "fn.math.negateExact(4)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(-4, result);
	}
	
	@Test
	void testNegateExact2() {
		String funcExpression = "fn.math.negateExact(-4)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(4, result);
	}
	
	@Test
	void testAddExact() {
		String funcExpression = "fn.math.addExact(14,-1)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(13, result);
	}
	
	@Test
	void testSubtractExact() {
		String funcExpression = "fn.math.subtractExact(14,-1)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(15, result);
	}
	
	@Test
	void testMultiplyExact() {
		String funcExpression = "fn.math.multiplyExact(14,2)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(28, result);
	}
	
	@Test
	void testMaxExact() {
		String funcExpression = "fn.math.maxExact(14,2)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(14, result);
	}
	
	@Test
	void testMinExact() {
		String funcExpression = "fn.math.minExact(14,2)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(2, result);
	}

	@Test
	void testMod() {
		String funcExpression = "fn.math.mod(13,2)";
		long result = (long) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(1, result);
	}

	@Test
	void testPow() {
		String funcExpression = "fn.math.pow(2,3)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(8, result);
	}

	@Test
	void testSqrt() {
		String funcExpression = "fn.math.sqrt(4)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(2, result);
	}
	
	@Test
	void testAbsDecimal() {
		String funcExpression = "fn.math.absDecimal(-4)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(4, result);
	}
	
	@Test
	void testNegateDecimal() {
		String funcExpression = "fn.math.negateDecimal(4)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(-4, result);
	}
	
	@Test
	void testSubtractDecimal() {
		String funcExpression = "fn.math.subtractDecimal(4,1.3)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(2.7, result);
	}
	
	@Test
	void testMultiplyDecimal() {
		String funcExpression = "fn.math.multiplyDecimal(4,2.2)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(8.8, result);
	}
	
	@Test
	void testDivideDecimal() {
		String funcExpression = "fn.math.divideDecimal(4.8,2)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(2.4, result);
	}
	
	@Test
	void testMaxDecimal() {
		String funcExpression = "fn.math.maxDecimal(4.8,2)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(4.8, result);
	}
	
	@Test
	void testMinDecimal() {
		String funcExpression = "fn.math.minDecimal(4.8,2)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(2, result);
	}
	
	@Test
	void testScaleDecimal() {
		String funcExpression = "fn.math.scaleDecimal(4.8456,2)";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		assertEquals(4.85, result);
	}
	
	@Test
	void testRandom() {
		String funcExpression = "fn.math.random()";
		double result = (double) FuncExecutor.getInstance().exec(null, funcExpression);
		// System.out.println(result);
	}

}