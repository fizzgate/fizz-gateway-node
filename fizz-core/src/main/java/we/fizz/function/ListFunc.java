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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List Functions
 * 
 * @author Francis Dong
 *
 */
public class ListFunc implements IFunc {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListFunc.class);

	private static ListFunc singleton;

	public static ListFunc getInstance() {
		if (singleton == null) {
			synchronized (ListFunc.class) {
				if (singleton == null) {
					ListFunc instance = new ListFunc();
					instance.init();
					singleton = instance;
				}
			}
		}
		return singleton;
	}

	private ListFunc() {
	}

	public void init() {
		FuncExecutor.register(NAME_SPACE_PREFIX + "list.expand", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "list.merge", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "list.extract", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "list.join", this);
	}

	/**
	 * Expand sublist item to the first level
	 * 
	 * @param data
	 * @return
	 */
	public List<Object> expand(List<List<Object>> data) {
		List<Object> result = new ArrayList<>();
		if (data == null || data.size() == 0) {
			return result;
		}
		for (List<Object> list : data) {
			result.addAll(list);
		}
		return result;
	}

	/**
	 * Merge multiple list into one list
	 * 
	 * @param data
	 * @return
	 */
	public List<Object> merge(List<Object>... data) {
		List<Object> result = new ArrayList<>();
		if (data == null || data.length == 0) {
			return result;
		}
		for (List<Object> list : data) {
			if (list == null || list.size() == 0) {
				continue;
			}
			result.addAll(list);
		}
		return result;
	}

	/**
	 * Extract fields from list
	 * 
	 * @param data
	 * @param fields
	 * @return
	 */
	public List<Map<String, Object>> extract(List<Map<String, Object>> data, String... fields) {
		List<Map<String, Object>> result = new ArrayList<>();
		if (data == null || data.size() == 0) {
			return result;
		}
		if (fields == null || fields.length == 0) {
			return data;
		}
		for (Map<String, Object> m : data) {
			Map<String, Object> r = new HashMap<>();
			for (String field : fields) {
				r.put(field, m.get(field));
			}
			result.add(r);
		}
		return result;
	}

	/**
	 * Merge fields of source list to destination list join by the join field
	 * 
	 * @param dest      destination list
	 * @param src       source list
	 * @param joinField join field
	 * @param fields    fields which will be merge to destination list, all fields
	 *                  will be merged if it is null
	 * @return
	 */
	public List<Map<String, Object>> join(List<Map<String, Object>> dest, List<Map<String, Object>> src,
			String joinField, String... fields) {
		if (dest == null || dest.size() == 0 || src == null || src.size() == 0) {
			return dest;
		}
		Map<String, Map<String, Object>> index = new HashMap<>();
		for (Map<String, Object> record : dest) {
			if (record.get(joinField) != null) {
				index.put(record.get(joinField).toString(), record);
			}
		}

		for (Map<String, Object> m : src) {
			if (m.get(joinField) == null) {
				continue;
			}
			Map<String, Object> record = index.get(m.get(joinField).toString());

			if (fields == null || fields.length == 0) {
				record.putAll(m);
			} else {
				for (String field : fields) {
					record.put(field, m.get(field));
				}
			}

		}
		return dest;
	}

}
