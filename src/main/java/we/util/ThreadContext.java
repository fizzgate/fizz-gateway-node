/*
 *  Copyright (C) 2020 the original author or authors.
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

package we.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public abstract class ThreadContext {

	private static ThreadLocal<Map<String, Object>> tl = new ThreadLocal<>();
	private static final int mapCap = 32;

	private static final String sb = "$sb";
	private static final int sbCap = 256;

	/** use me carefully! */
	public static StringBuilder getStringBuilder() {
		return getStringBuilder(true);
	}

	/** use me carefully! */
	public static StringBuilder getStringBuilder(boolean clean) {
		Map<String, Object> m = getMap();
		StringBuilder b = (StringBuilder) m.get(sb);
		if (b == null) {
			b = new StringBuilder(sbCap);
			m.put(sb, b);
		} else {
			if (clean) {
				b.delete(0, b.length());
			}
		}
		return b;
	}
	
	public static StringBuilder getStringBuilder(String key) {
		StringBuilder b = (StringBuilder) get(key);
		if (b == null) {
			b = new StringBuilder(sbCap);
			Map<String, Object> m = getMap();
			m.put(key, b);
		} else {
			b.delete(0, b.length());
		}
		return b;
	}
	
	/** for legacy code. */
	public static SimpleDateFormat getSimpleDateFormat(String pattern) {
		Map<String, Object> m = getMap();
		SimpleDateFormat sdf = (SimpleDateFormat) m.get(pattern);
		if (sdf == null) {
			sdf = new SimpleDateFormat(pattern);
			m.put(pattern, sdf);
		}
		return sdf;
	}

	private static Map<String, Object> getMap() {
		Map<String, Object> m = tl.get();
		if (m == null) {
			m = new HashMap<>(mapCap);
			tl.set(m);
		}
		return m;
	}
	
	public static Object get(String key) {
		return getMap().get(key);
	}
	
	public static <T> T get(String key, Class<T> clz) {
		T t = (T) get(key);
		if (t == null) {
			try {
				t = clz.newInstance();
				set(key, t);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		return t;
	}

	public static void set(String key, Object obj) {
		getMap().put(key, obj);
	}
	
	public static Object remove(String key) {
		return getMap().remove(key);
	}

	public static <T> ArrayList<T> getArrayList(String key, Class<T> elementType) {
		return getArrayList(key, elementType, true);
	}

	public static <T> ArrayList<T> getArrayList(String key, Class<T> elementType, boolean clear) {
		ArrayList<T> l = (ArrayList) get(key);
		if (l == null) {
			l = new ArrayList<>();
			set(key, l);
		} else if (clear) {
			l.clear();
		}
		return l;
	}
}
