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

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Math Functions
 * 
 * @author Francis Dong
 *
 */
@SuppressWarnings("unused")
public class MathFunc implements IFunc {

	private static final Logger LOGGER = LoggerFactory.getLogger(MathFunc.class);

	private static MathFunc singleton;

	public static MathFunc getInstance() {
		if (singleton == null) {
			synchronized (MathFunc.class) {
				if (singleton == null) {
					MathFunc instance = new MathFunc();
					instance.init();
					singleton = instance;
				}
			}
		}
		return singleton;
	}

	private MathFunc() {
	}

	public void init() {
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.absExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.negateExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.addExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.subtractExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.multiplyExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.maxExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.minExact", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.mod", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.pow", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.sqrt", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.random", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.absDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.negateDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.addDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.subtractDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.multiplyDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.divideDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.maxDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.minDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.scaleDecimal", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.compare", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.equals", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.lt", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.le", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.gt", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "math.ge", this);
	}

	public long absExact(long a) {
		return Math.abs(a);
	}

	public long negateExact(long a) {
		return Math.negateExact(a);
	}

	public long addExact(long x, long y) {
		return Math.addExact(x, y);
	}

	public long subtractExact(long x, long y) {
		return Math.subtractExact(x, y);
	}

	public long multiplyExact(long x, long y) {
		return Math.multiplyExact(x, y);
	}

	public long maxExact(long x, long y) {
		return Math.max(x, y);
	}

	public long minExact(long x, long y) {
		return Math.min(x, y);
	}

	public long mod(long x, long y) {
		return Math.floorMod(x, y);
	}

	public double pow(double a, double b) {
		return Math.pow(a, b);
	}

	public double sqrt(double a) {
		return Math.sqrt(a);
	}

	/**
	 * Returns a {@code double} value with a positive sign, greater than or equal to
	 * {@code 0.0} and less than {@code 1.0}. Returned values are chosen
	 * pseudorandomly with (approximately) uniform distribution from that range.
	 * 
	 * @return
	 */
	public double random() {
		return Math.random();
	}

	public double absDecimal(double a) {
		return BigDecimal.valueOf(a).abs().doubleValue();
	}

	public double negateDecimal(double a) {
		return BigDecimal.valueOf(a).negate().doubleValue();
	}

	public double addDecimal(double x, double y) {
		return BigDecimal.valueOf(x).add(BigDecimal.valueOf(y)).doubleValue();
	}

	public double subtractDecimal(double x, double y) {
		return BigDecimal.valueOf(x).subtract(BigDecimal.valueOf(y)).doubleValue();
	}

	public double multiplyDecimal(double x, double y) {
		return BigDecimal.valueOf(x).multiply(BigDecimal.valueOf(y)).doubleValue();
	}

	public double divideDecimal(double x, double y) {
		return BigDecimal.valueOf(x).divide(BigDecimal.valueOf(y)).doubleValue();
	}

	public double maxDecimal(double x, double y) {
		return BigDecimal.valueOf(x).max(BigDecimal.valueOf(y)).doubleValue();
	}

	public double minDecimal(double x, double y) {
		return BigDecimal.valueOf(x).min(BigDecimal.valueOf(y)).doubleValue();
	}

	public double scaleDecimal(double a, int scale) {
		return BigDecimal.valueOf(a).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	/**
	 * Compares number x with the specified number y.
	 * 
	 * @param x number
	 * @param y number
	 * @return -1, 0, or 1 as x is numerically less than, equal to, or greater than
	 *         y.
	 */
	public int compare(double x, double y) {
		return BigDecimal.valueOf(x).compareTo(BigDecimal.valueOf(y));
	}
	
	public boolean equals(double x, double y) {
		return BigDecimal.valueOf(x).equals(BigDecimal.valueOf(y));
	}
	
	/**
	 * Checks if x is less than y
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean lt(double x, double y) {
		return BigDecimal.valueOf(x).compareTo(BigDecimal.valueOf(y)) == -1;
	}
	
	/**
	 * Checks if x is less than or equals y
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean le(double x, double y) {
		return BigDecimal.valueOf(x).compareTo(BigDecimal.valueOf(y)) <= 0;
	}
	
	/**
	 * Checks if x is greater than y
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean gt(double x, double y) {
		return BigDecimal.valueOf(x).compareTo(BigDecimal.valueOf(y)) == 1;
	}
	
	/**
	 * Checks if x is greater than or equals y
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean ge(double x, double y) {
		return BigDecimal.valueOf(x).compareTo(BigDecimal.valueOf(y)) >= 0;
	}

}
