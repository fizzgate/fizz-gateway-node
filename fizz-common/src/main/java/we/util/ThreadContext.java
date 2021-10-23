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
import java.util.HashSet;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public abstract class ThreadContext {

	private static       ThreadLocal<Map<String, Object>> tl     = new ThreadLocal<>();

	private static final int     mapCap      = 32;

	private static final String  sb          = "$sb";
	public  static final String  sb0         = "$sb0";
	private static final int     sbCap       = 256;

	private static final String  arrayList   = "arlstT";
	public  static final String  arrayList0  = "arlst0T";
	private static final String  hashMap     = "hsMapT";
	private static final String  hashSet     = "hsSetT";

	private static final String  traId       = "traIdT";

	private ThreadContext() {
	}

	public static void setTraceId(String traceId) {
		set(traId, traceId);
	}

	public String getTraceId() {
		return (String) get(traId);
	}

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
			} catch (InstantiationException | IllegalAccessException e) {
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

	public static <T> ArrayList<T> getArrayList() {
		return getArrayList(arrayList, true);
	}

	public static <T> ArrayList<T> getArrayList(String key) {
		return getArrayList(key, true);
	}

	public static <T> ArrayList<T> getArrayList(String key, boolean clear) {
		ArrayList<T> l = (ArrayList<T>) get(key);
		if (l == null) {
			l = new ArrayList<T>();
			set(key, l);
		} else if (clear) {
			l.clear();
		}
		return l;
	}

	public static <K, V> HashMap<K, V> getHashMap() {
		return getHashMap(hashMap, true);
	}

	public static <K, V> HashMap<K, V> getHashMap(String key) {
		return getHashMap(key, true);
	}

	public static <K, V> HashMap<K, V> getHashMap(String key, boolean clear) {
		HashMap<K, V> m = (HashMap<K, V>) get(key);
		if (m == null) {
			m = new HashMap<K, V>();
			set(key ,m);
		} else if (clear) {
			m.clear();
		}
		return m;
	}

	public static <E> HashSet<E> getHashSet() {
		return getHashSet(hashSet, true);
	}

	public static <E> HashSet<E> getHashSet(String key) {
		return getHashSet(key, true);
	}

	public static <E> HashSet<E> getHashSet(String key, boolean clear) {
		HashSet<E> s = (HashSet<E>) get(key);
		if (s == null) {
			s = new HashSet<E>();
			set(key ,s);
		} else if (clear) {
			s.clear();
		}
		return s;
	}
}
