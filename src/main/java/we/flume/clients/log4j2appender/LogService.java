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
