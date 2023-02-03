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
package com.fizzgate.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 
 * @author Francis Dong
 *
 */
public class MapUtil {

	private static final String KEY = "key";

	private static final String VALUE = "value";

	public static HttpHeaders toHttpHeaders(Map<String, Object> params) {
		HttpHeaders headers = new HttpHeaders();

		if (params == null || params.isEmpty()) {
			return headers;
		}

		for (Entry<String, Object> entry : params.entrySet()) {
			Object val = entry.getValue();
			List<String> list = new ArrayList<>();
			if (val instanceof List) {
				List<Object> vals = (List<Object>) val;
				for (Object value : vals) {
					if (value != null) {
						list.add(value.toString());
					}
				}
			} else {
				if (val != null) {
					list.add(val.toString());
				}
			}
			if (list.size() > 0) {
				headers.put(entry.getKey(), list);
			}
		}

		return headers;
	}

	public static MultiValueMap<String, String> toMultiValueMap(Map<String, Object> params) {
		MultiValueMap<String, String> mvmap = new LinkedMultiValueMap<>();

		if (params == null || params.isEmpty()) {
			return mvmap;
		}

		for (Entry<String, Object> entry : params.entrySet()) {
			Object val = entry.getValue();
			List<String> list = new ArrayList<>();
			if (val instanceof List) {
				List<Object> vals = (List<Object>) val;
				for (Object value : vals) {
					if (value != null) {
						list.add(value.toString());
					}
				}
			} else {
				if (val != null) {
					list.add(val.toString());
				}
			}
			if (list.size() > 0) {
				mvmap.put(entry.getKey(), list);
			}
		}

		return mvmap;
	}

	public static MultiValueMap<String, Object> toMultipartDataMap(Map<String, Object> params) {
		MultiValueMap<String, Object> mvmap = new LinkedMultiValueMap<>();
		if (params == null || params.isEmpty()) {
			return mvmap;
		}
		for (Entry<String, Object> entry : params.entrySet()) {
			Object val = entry.getValue();
			List<Object> list = new ArrayList<>();
			if (val instanceof List) {
				List<Object> vals = (List<Object>) val;
				for (Object value : vals) {
					if (value != null) {
						list.add(value);
					}
				}
			} else {
				if (val != null) {
					list.add(val.toString());
				}
			}
			if (list.size() > 0) {
				mvmap.put(entry.getKey(), list);
			}
		}
		return mvmap;
	}
	
	/**
	 * Extract form data from multipart map exclude file
	 * 
	 * @param params
	 * @param fileKeyPrefix
	 * @param filePartMap   Map
	 * @return
	 */
	public static Map<String, Object> extractFormData(MultiValueMap<String, Part> params, String fileKeyPrefix,
			Map<String, FilePart> filePartMap) {
		HashMap<String, Object> m = new HashMap<>();
		if (params == null || params.isEmpty()) {
			return m;
		}
		for (Entry<String, List<Part>> entry : params.entrySet()) {
			List<Part> val = entry.getValue();
			if (val != null && val.size() > 0) {
				if (val.size() > 1) {
					List<Object> formFieldValues = new ArrayList<>();
					val.stream().forEach(part -> {
						if (part instanceof FormFieldPart) {
							FormFieldPart p = (FormFieldPart) part;
							formFieldValues.add(p.value());
						} else if (part instanceof FilePart) {
							FilePart fp = (FilePart) part;
							String k = fileKeyPrefix + UUIDUtil.getUUID() + "-" + fp.filename();
							formFieldValues.add(k);
							filePartMap.put(k, fp);
						}
					});
					if (formFieldValues.size() > 0) {
						m.put(entry.getKey(), formFieldValues);
					}
				} else {
					if (val.get(0) instanceof FormFieldPart) {
						FormFieldPart p = (FormFieldPart) val.get(0);
						m.put(entry.getKey(), p.value());
					} else if (val.get(0) instanceof FilePart) {
						FilePart fp = (FilePart) val.get(0);
						String k = fileKeyPrefix + UUIDUtil.getUUID() + "-" + fp.filename();
						m.put(entry.getKey(), k);
						filePartMap.put(k, fp);
					}
				}
			}
		}
		return m;
	}

	/**
	 * Replace file field with FilePart object
	 * 
	 * @param params
	 * @param fileKeyPrefix
	 * @param filePartMap
	 */
	public static void replaceWithFilePart(MultiValueMap<String, Object> params, String fileKeyPrefix,
			Map<String, FilePart> filePartMap) {
		if (params == null || params.isEmpty() || filePartMap == null || filePartMap.isEmpty()) {
			return;
		}

		for (Entry<String, List<Object>> entry : params.entrySet()) {
			List<Object> list = entry.getValue();
			if (list != null && list.size() > 0) {
				List<Object> newlist = new ArrayList<>();
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i).toString().startsWith(fileKeyPrefix)) {
						newlist.add(filePartMap.get(list.get(i).toString()));
					} else {
						newlist.add(list.get(i));
					}
				}
				params.put(entry.getKey(), newlist);
			}
		}
	}

	public static Map<String, Object> toHashMap(MultiValueMap<String, String> params) {
		HashMap<String, Object> m = new HashMap<>();

		if (params == null || params.isEmpty()) {
			return m;
		}

		for (Entry<String, List<String>> entry : params.entrySet()) {
			List<String> val = entry.getValue();
			if (val != null && val.size() > 0) {
				if (val.size() > 1) {
					m.put(entry.getKey(), val);
				} else {
					m.put(entry.getKey(), val.get(0));
				}
			}
		}

		return m;
	}

	public static Map<String, Object> headerToHashMap(HttpHeaders headers) {
		HashMap<String, Object> m = new HashMap<>();

		if (headers == null || headers.isEmpty()) {
			return m;
		}

		for (Entry<String, List<String>> entry : headers.entrySet()) {
			List<String> val = entry.getValue();
			if (val != null && val.size() > 0) {
				if (val.size() > 1) {
					m.put(entry.getKey().toUpperCase(), val);
				} else {
					m.put(entry.getKey().toUpperCase(), val.get(0));
				}
			}
		}

		return m;
	}

	public static Map<String, Object> upperCaseKey(Map<String, Object> m) {
		HashMap<String, Object> rs = new HashMap<>();

		if (m == null || m.isEmpty()) {
			return rs;
		}

		for (Entry<String, Object> entry : m.entrySet()) {
			rs.put(entry.getKey().toUpperCase(), entry.getValue());
		}

		return rs;
	}
	
	public static MultiValueMap<String, Object> upperCaseKey(MultiValueMap<String, Object> m) {
		MultiValueMap<String, Object> rs = new LinkedMultiValueMap<>();

		if (m == null || m.isEmpty()) {
			return rs;
		}

		for (Entry<String, List<Object>> entry : m.entrySet()) {
			rs.put(entry.getKey().toUpperCase(), entry.getValue());
		}

		return rs;
	}

	/**
	 * Set value by path，support multiple levels，eg：a.b.c <br>
	 * Do NOT use this method if field name contains a dot <br>
	 * 
	 * @param data
	 * @param path
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public static void set(Map<String, Object> data, String path, Object value) {
		String[] fields = path.split("\\.");
		if (fields.length < 2) {
			data.put(path, value);
		} else {
			Map<String, Object> next = data;
			for (int i = 0; i < fields.length - 1; i++) {
				Map<String, Object> val = (Map<String, Object>) next.get(fields[i]);
				if (val == null) {
					val = new HashMap<>();
					next.put(fields[i], val);
				}
				if (i == fields.length - 2) {
					val.put(fields[i + 1], value);
					break;
				}
				next = val;
			}
		}
	}

	/**
	 * Get value by path, support multiple levels，eg：a.b.c <br>
	 * Do NOT use this method if field name contains a dot <br>
	 * 
	 * @param data
	 * @param path
	 * @return
	 */
	public static Object get(Map<String, Object> data, String path) {
		String[] fields = path.split("\\.");
		if (fields.length < 2) {
			return data.get(path);
		} else {
			Map<String, Object> next = data;
			for (int i = 0; i < fields.length - 1; i++) {
				if (!(next.get(fields[i]) instanceof Map)) {
					return null;
				}
				Map<String, Object> val = (Map<String, Object>) next.get(fields[i]);
				if (val == null) {
					return null;
				}
				if (i == fields.length - 2) {
					return val.get(fields[i + 1]);
				}
				next = val;
			}
		}
		return null;
	}

	/**
	 * Merge maps, merge src to target
	 * 
	 * @param target
	 * @param src
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> merge(Map<String, Object> target, Map<String, Object> src) {
		if (src == null || src.isEmpty()) {
			return target;
		}
		src.forEach((key, value) -> {
			if (value != null) {
				target.merge(key, value, (oldValue, newValue) -> {
					if (oldValue instanceof Map && newValue instanceof Map) {
						oldValue = merge((Map<String, Object>) oldValue, (Map<String, Object>) newValue);
						return oldValue;
					}
					return newValue;
				});
			} else {
				target.put(key, value);
			}
		});
		return target;
	}

	/**
	 * Convert list to map and merge multiple values<br>
	 * Example: <br>
	 * List as:<br>
	 * [{"key": "abc", "value": "aaa"},{"key": "abc", "value": "xyz"},{"key":
	 * "a123", "value": 666}]<br>
	 * Merge Result:<br>
	 * {"abc": ["aaa","xyz"], "a123": 666} <br>
	 * 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> list2Map(Object obj) {
		// Compatible with older version configuration
		if (obj instanceof Map) {
			return (Map<String, Object>) obj;
		}
		if (obj instanceof List) {
			Map<String, Object> rs = new HashMap<>();
			List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
			if (list == null || list.size() == 0) {
				return rs;
			}
			for (Map<String, Object> m : list) {
				String k = m.get(KEY).toString();
				Object v = m.get(VALUE);
				if (rs.containsKey(k)) {
					List<Object> vals = null;
					if (rs.get(k) instanceof List) {
						vals = (List<Object>) rs.get(k);
					} else {
						vals = new ArrayList<>();
						vals.add(rs.get(k));
					}
					vals.add(v);
					rs.put(k, vals);
				} else {
					rs.put(k, v);
				}
			}
			return rs;
		}
		return null;
	}

	/**
	 * Convert list to MultiValueMap<br>
	 * List format example:<br>
	 * [{"key": "abc", "value": "aaa"},{"key": "abc", "value": "xyz"},,{"key":
	 * "a123", "value": 666}]<br>
	 * 
	 * @param list
	 * @return
	 */
	public static MultiValueMap<String, Object> listToMultiValueMap(List<Map<String, Object>> list) {
		MultiValueMap<String, Object> mvmap = new LinkedMultiValueMap<>();
		if (list == null || list.size() == 0) {
			return mvmap;
		}
		for (Map<String, Object> m : list) {
			mvmap.add(m.get(KEY).toString(), m.get(VALUE));
		}
		return mvmap;
	}

}
