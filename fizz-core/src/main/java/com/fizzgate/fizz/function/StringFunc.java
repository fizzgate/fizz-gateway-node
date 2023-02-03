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
package com.fizzgate.fizz.function;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fizzgate.fizz.exception.FizzRuntimeException;

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
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.equals", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.equalsIgnoreCase", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.compare", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.concat", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.concatws", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.substring", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.indexOf", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.startsWith", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.endsWith", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.toUpperCase", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.toLowerCase", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.uuid", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.toString", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.replace", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.replaceAll", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "string.replaceFirst", this);
	}

	/**
	 * <p>
	 * Compares two Strings, returning {@code true} if they represent equal
	 * sequences of Strings.
	 * </p>
	 *
	 * <p>
	 * {@code null}s are handled without exceptions. Two {@code null} references are
	 * considered to be equal. The comparison is <strong>case sensitive</strong>.
	 * </p>
	 *
	 * <pre>
	 * equals(null, null)   = true
	 * equals(null, "abc")  = false
	 * equals("abc", null)  = false
	 * equals("abc", "abc") = true
	 * equals("abc", "ABC") = false
	 * </pre>
	 *
	 * @param str1 the first String, may be {@code null}
	 * @param str2 the second String, may be {@code null}
	 * @return {@code true} if the Strings are equal (case-sensitive), or both
	 *         {@code null}
	 */
	public boolean equals(String str1, String str2) {
		return StringUtils.equals(str1, str2);
	}

	/**
	 * <p>
	 * Compares two Strings, returning {@code true} if they represent equal
	 * sequences of Strings, ignoring case.
	 * </p>
	 *
	 * <p>
	 * {@code null}s are handled without exceptions. Two {@code null} references are
	 * considered equal. The comparison is <strong>case insensitive</strong>.
	 * </p>
	 *
	 * <pre>
	 * equalsIgnoreCase(null, null)   = true
	 * equalsIgnoreCase(null, "abc")  = false
	 * equalsIgnoreCase("abc", null)  = false
	 * equalsIgnoreCase("abc", "abc") = true
	 * equalsIgnoreCase("abc", "ABC") = true
	 * </pre>
	 *
	 * @param str1 the first String, may be {@code null}
	 * @param str2 the second String, may be {@code null}
	 * @return {@code true} if the Strings are equal (case-insensitive), or both
	 *         {@code null}
	 */
	public boolean equalsIgnoreCase(String str1, String str2) {
		return StringUtils.equalsIgnoreCase(str1, str2);
	}

	/**
	 * Compare two Strings lexicographically
	 * 
	 * @param str1
	 * @param str2
	 * @return -1, 0, 1, if {@code str1} is respectively less, equal or greater than
	 *         {@code str2}
	 */
	public int compare(String str1, String str2) {
		int n = StringUtils.compare(str1, str2);
		return n == 0 ? 0 : (n > 0 ? 1 : -1);
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

	/**
	 * 
	 * @return UUID
	 */
	public String uuid() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	public String toString(Object obj) {
		return obj == null ? null : obj.toString();
	}

	/**
	 * Replaces each substring of this string that matches the literal target
	 * sequence with the specified literal replacement sequence. The replacement
	 * proceeds from the beginning of the string to the end, for example, replacing
	 * "aa" with "b" in the string "aaa" will result in "ba" rather than "ab".
	 *
	 * @param str         String
	 * @param target      The sequence of char values to be replaced
	 * @param replacement The replacement sequence of char values
	 * @return
	 */
	public String replace(String str, String target, String replacement) {
		return str.replace(target, replacement);
	}

	/**
	 * Replaces each substring of this string that matches the given regular
	 * expression with the given replacement.
	 * 
	 * @param str         String
	 * @param regex       the regular expression to which this string is to be
	 *                    matched
	 * @param replacement the string to be substituted for each match
	 * @return
	 */
	public String replaceAll(String str, String regex, String replacement) {
		return str.replaceAll(regex, replacement);
	}

	/**
	 * Replaces the first substring of this string that matches the given regular
	 * expression with the given replacement.
	 * 
	 * @param str         String
	 * @param regex       the regular expression to which this string is to be
	 *                    matched
	 * @param replacement the string to be substituted for the first match
	 * @return
	 */
	public String replaceFirst(String str, String regex, String replacement) {
		return str.replaceFirst(regex, replacement);
	}

}
