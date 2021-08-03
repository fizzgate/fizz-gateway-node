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

package we.flume.clients.log4j2appender;

import we.constants.CommonConstants;

public enum LogService {

	BIZ_ID, HANDLE_STGY, APP;

	public static void cleanBizId() {
		setBizId(null);
	}

	public static Object getBizId() {
		return ThreadContext.get(Constants.BIZ_ID);
	}

	public static void setBizId(Object bizId) {
		ThreadContext.set(Constants.BIZ_ID, bizId);
		if (bizId != null) {
			org.apache.logging.log4j.ThreadContext.put(CommonConstants.TRACE_ID, String.valueOf(bizId));
		}
	}

	public static String toKF(String topic) {
		return topic;
	}

	public static String toESaKF(String topic) {
		return Constants.AND + topic;
	}
	
	public static class Constants {
		static final String BIZ_ID = "bizId";
		static final char   AND    = '&';
	}
}
