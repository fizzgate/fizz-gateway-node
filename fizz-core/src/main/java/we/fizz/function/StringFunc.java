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

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.fizz.exception.FizzRuntimeException;

/**
 * String Functions
 * 
 * @author Francis Dong
 *
 */
public class StringFunc implements IFunc {

	private static final Logger LOGGER = LoggerFactory.getLogger(StringFunc.class);

	private static StringFunc singleton;

	public static StringFunc getInstance() {
		if (singleton == null) {
			synchronized (StringFunc.class) {
				if (singleton == null) {
					StringFunc instance = new StringFunc();
					instance.init();
					singleton = instance;
				}
			}
		}
		return singleton;
	}

	private StringFunc() {
	}

	public void init() {
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.concat", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.concatws", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.substring", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.indexOf", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.startsWith", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.endsWith", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.toUpperCase", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.toLowerCase", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.uuid", this);
	}

	/**
	 * Concat strings
	 * 
	 * @param strs
	 * @return
	 */
	public String concat(String... strs) {
		return StringUtils.join(strs);
	}

	/**
	 * Concat with separator
	 * 
	 * @param strs
	 * @return
	 */
	public String concatws(String separator, String... strs) {
		return StringUtils.join(strs, separator);
	}

	/**
	 * Returns a string that is a substring of this string. The substring begins at
	 * the specified {@code beginIndex} and extends to the character at index
	 * {@code endIndex - 1}. Thus the length of the substring is
	 * {@code endIndex-beginIndex}.
	 * 
	 * @param str
	 * @param beginIndex
	 * @param endIndex
	 * @return
	 */
	public String substring(String str, int beginIndex, int... endIndex) {
		if (StringUtils.isBlank(str)) {
			return str;
		}
		if (endIndex != null && endIndex.length > 0) {
			if (endIndex.length > 1) {
				LOGGER.error("invalid argument: endIndex");
				throw new FizzRuntimeException("invalid argument: endIndex");
			}
			return str.substring(beginIndex, endIndex[0]);
		}
		return str.substring(beginIndex);
	}

	/**
	 * Returns the index within this string of the first occurrence of the specified
	 * substring.
	 * 
	 * @param str
	 * @param substr
	 * @return the index of the first occurrence of the specified substring, or
	 *         {@code -1} if there is no such occurrence.
	 */
	public int indexOf(String str, String substr) {
		if (StringUtils.isBlank(str)) {
			return -1;
		}
		return str.indexOf(substr);
	}

	/**
	 * Tests if this string starts with the specified prefix.
	 *
	 * @param prefix the prefix.
	 * @return {@code true} if the character sequence represented by the argument is
	 *         a prefix of the character sequence represented by this string;
	 *         {@code false} otherwise. Note also that {@code true} will be returned
	 *         if the argument is an empty string or is equal to this {@code String}
	 *         object as determined by the {@link #equals(Object)} method.
	 */
	public boolean startsWith(String str, String prefix) {
		if (StringUtils.isBlank(str)) {
			return false;
		}
		return str.startsWith(prefix);
	}

	/**
	 * Tests if this string starts with the specified prefix.
	 *
	 * @param prefix the prefix.
	 * @return {@code true} if the character sequence represented by the argument is
	 *         a prefix of the character sequence represented by this string;
	 *         {@code false} otherwise. Note also that {@code true} will be returned
	 *         if the argument is an empty string or is equal to this {@code String}
	 *         object as determined by the {@link #equals(Object)} method.
	 */
	public boolean endsWith(String str, String suffix) {
		if (StringUtils.isBlank(str)) {
			return false;
		}
		return str.endsWith(suffix);
	}

	public String toUpperCase(String str) {
		if (StringUtils.isBlank(str)) {
			return str;
		}
		return str.toUpperCase();
	}

	public String toLowerCase(String str) {
		if (StringUtils.isBlank(str)) {
			return str;
		}
		return str.toLowerCase();
	}
	
	public String uuid() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

}
