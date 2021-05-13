package we.flume.clients.log4j2appender;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/** for internal use */
public abstract class ThreadContext {

	private static ThreadLocal<Map<String, Object>> tl = new ThreadLocal<>();
	private static final int mapCap = 16;
	private static final float mapLoadFactor = 1.0f;

	private static final String sb = "sb";
	private static final int sbCap = 256;

	public static StringBuilder getStringBuilder() {
		return getStringBuilder(true);
	}

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
	
	public static SimpleDateFormat getSimpleDateFormat(String pattern) {
		Map<String, Object> m = getMap();
		SimpleDateFormat sdf = (SimpleDateFormat) m.get(pattern);
		if (sdf == null) {
			sdf = new SimpleDateFormat(pattern);
			m.put(pattern, sdf);
		}
		return sdf;
	}
	
	public static Object get(String key, Class<?> clz) {
		Object obj = get(key);
		if (obj == null) {
			try {
				obj = clz.newInstance();
				set(key, obj);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		return obj;
	}

	private static Map<String, Object> getMap() {
		Map<String, Object> m = tl.get();
		if (m == null) {
			m = new HashMap<>(mapCap, mapLoadFactor);
			tl.set(m);
		}
		return m;
	}
	
	public static Object get(String key) {
		return getMap().get(key);
	}

	public static void set(String key, Object obj) {
		getMap().put(key, obj);
	}
}
