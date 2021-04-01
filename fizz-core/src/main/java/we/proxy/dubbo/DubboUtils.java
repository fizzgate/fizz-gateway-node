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

package we.proxy.dubbo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author linwaiwai
 * @author Francis Dong
 *
 */
public class DubboUtils {

	/*
	 * body json string
	 */
	public static Pair<String[], Object[]> parseDubboParam(Map<String, Object> paramMap, final String parameterTypes) {
		if (StringUtils.isBlank(parameterTypes)) {
			return new ImmutablePair<>(null, null);
		}
		String[] parameter = StringUtils.split(parameterTypes, ',');
		if (parameter.length == 1 && !isBaseType(parameter[0])) {
			return new ImmutablePair<>(parameter, new Object[] { paramMap.get("p1") });
		}
		List<Object> list = new LinkedList<>();
		for (int i = 0; i < parameter.length; i++) {
			list.add(paramMap.get("p" + (i + 1)));
		}
		Object[] objects = list.toArray();
		return new ImmutablePair<>(parameter, objects);
	}

	private static boolean isBaseType(String type) {
		return type.startsWith("java") || type.startsWith("[Ljava");
	}
}
