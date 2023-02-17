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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fizzgate.fizz.exception.FizzRuntimeException;

/**
 * Date Functions
 * 
 * @author Francis Dong
 *
 */
public class DateFunc implements IFunc {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateFunc.class);

	public static final String DEFAULT_TIMEZONE = "GMT+08:00";

	private static DateFunc singleton;

	public static DateFunc getInstance() {
		if (singleton == null) {
			synchronized (DateFunc.class) {
				if (singleton == null) {
					DateFunc instance = new DateFunc();
					instance.init();
					singleton = instance;
				}
			}
		}
		return singleton;
	}

	private DateFunc() {
	}

	public void init() {
		FuncExecutor.register(NAME_SPACE_PREFIX + "date.timestamp", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "date.getTime", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "date.now", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "date.add", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "date.formatTs", this);
		FuncExecutor.register(NAME_SPACE_PREFIX + "date.changePattern", this);
	}

	/**
	 * Date pattern<br>
	 * yyyy-MM-dd
	 */
	public final static String DATE_FORMAT = "yyyy-MM-dd";

	/**
	 * Time pattren<br>
	 * HH:mm:ss
	 */
	public final static String TIME_FORMAT = "HH:mm:ss";

	/**
	 * Short time pattren<br>
	 * HH:mm
	 */
	public final static String SHORT_TIME_FORMAT = "HH:mm";

	/**
	 * Date time pattern<br>
	 * yyyy-MM-dd HH:mm:ss
	 */
	public final static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Returns current timestamp (Milliseconds)
	 * 
	 * @return
	 */
	public long timestamp() {
		return System.currentTimeMillis();
	}
	
	/**
	 * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
	 * represented by this Date object.
	 * 
	 * @param date     date string
	 * @param pattern  Date time pattern
	 * @param timeZone [optional] timeZone
	 * @return
	 */
	public Long getTime(String date, String pattern, String... timeZone) {
		if (StringUtils.isBlank(date)) {
			return null;
		}
		Date d = parse(date, pattern, timeZone);
		return d.getTime();
	}

	/**
	 * Returns current time with the given pattern<br>
	 * Frequently-used pattern:<br>
	 * yyyy-MM-dd HH:mm:ss<br>
	 * yyyy-MM-dd<br>
	 * HH:mm:ss<br>
	 * HH:mm<br>
	 * yyyy-MM-dd HH:mm:ss Z<br>
	 * 
	 * @param pattern  the pattern describing the date and time format, dafault
	 *                 yyyy-MM-dd HH:mm:ss
	 * @param timeZone [optional] timeZone
	 * @return
	 */
	public String now(String pattern, String... timeZone) {
		return formatDate(new Date(), pattern, timeZone);
	}

	/**
	 * Adds or subtracts the specified amount of time to the given calendar field,
	 * based on the calendar's rules. For example, to subtract 5 hours from the
	 * current time of the calendar, you can achieve it by calling:
	 * <p>
	 * <code>add("2021-08-04 14:23:12", "yyyy-MM-dd HH:mm:ss", 4, -5)</code>.
	 * 
	 * @param date     date string
	 * @param pattern  date pattern of the given date string
	 * @param field    the calendar field, <br>
	 *                 1 for millisecond<br>
	 *                 2 for second<br>
	 *                 3 for minute<br>
	 *                 4 for hour<br>
	 *                 5 for date<br>
	 *                 6 for month<br>
	 *                 7 for year<br>
	 * @param amount   the amount of date or time to be added to the field
	 * @param timeZone [optional] timeZone
	 * @return
	 */
	public String add(String date, String pattern, int field, int amount, String... timeZone) {
		if (StringUtils.isBlank(date)) {
			return null;
		}
		Date d = parse(date, pattern, timeZone);
		if (d != null) {
			// convert to calendar field
			int calField = 0;
			switch (field) {
			case 1:
				calField = Calendar.MILLISECOND;
				break;
			case 2:
				calField = Calendar.SECOND;
				break;
			case 3:
				calField = Calendar.MINUTE;
				break;
			case 4:
				calField = Calendar.HOUR;
				break;
			case 5:
				calField = Calendar.DATE;
				break;
			case 6:
				calField = Calendar.MONTH;
				break;
			case 7:
				calField = Calendar.YEAR;
				break;
			default:
				LOGGER.error("invalid field, date={} pattern={} filed={}", date, pattern, field);
				throw new FizzRuntimeException(
						"invalid field, date=" + date + "pattern=" + pattern + " filed=" + field);
			}
			return formatDate(addToFiled(d, calField, amount), pattern, timeZone);
		}
		return null;
	}

	/**
	 * Format the a timestamp to the given pattern
	 * 
	 * @param timestamp
	 * @param pattern
	 * @param timeZone  [optional] timeZone
	 * @return
	 */
	public String formatTs(Long timestamp, String pattern, String... timeZone) {
		if (timestamp == null) {
			return null;
		}
		return formatDate(new Date(timestamp), pattern, timeZone);
	}

	/**
	 * Format the a time with source pattern to the target pattern
	 * 
	 * @param dateStr       date
	 * @param sourcePattern source pattern
	 * @param targetPattern target pattern
	 * @param timeZone      [optional] timeZone
	 * @return
	 */
	public String changePattern(String dateStr, String sourcePattern, String targetPattern, String... timeZone) {
		if (StringUtils.isBlank(dateStr)) {
			return null;
		}
		return formatDate(parse(dateStr, sourcePattern, timeZone), targetPattern, timeZone);
	}

	/**
	 * Adds or subtracts the specified amount of time to the given calendar field
	 * 
	 * @param date   a Date
	 * @param field  field that the times to be add to, such as: Calendar.SECOND,
	 *               Calendar.YEAR
	 * @param amount the amount of date or time to be added to the field
	 * @return
	 */
	private Date addToFiled(Date date, int field, int amount) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(field, amount);
		return cal.getTime();
	}

	/**
	 * Parse string to Date
	 * 
	 * @param dateStr  String to be parsed
	 * @param pattern  pattern of dateStr
	 * @param timeZone [optional] timeZone
	 * @return
	 */
	private Date parse(String dateStr, String pattern, String... timeZone) {
		if (StringUtils.isBlank(dateStr)) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(pattern == null ? DATE_TIME_FORMAT : pattern);
		if (timeZone != null && timeZone.length > 0) {
			sdf.setTimeZone(TimeZone.getTimeZone(timeZone[0]));
		} else {
			sdf.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
		}
		try {
			return sdf.parse(dateStr);
		} catch (ParseException e) {
			LOGGER.error("Parse date error, dateStr={} pattern={}", dateStr, pattern, e);
			throw new FizzRuntimeException("Parse date error, dateStr=" + dateStr + " pattern=" + pattern, e);
		}
	}

	/**
	 * Format date with the given pattern<br>
	 * Frequently-used pattern:<br>
	 * yyyy-MM-dd HH:mm:ss<br>
	 * yyyy-MM-dd<br>
	 * HH:mm:ss<br>
	 * HH:mm<br>
	 * yyyy-MM-dd HH:mm:ss Z<br>
	 * 
	 * @param pattern  [optional] the pattern describing the date and time format,
	 *                 dafault yyyy-MM-dd HH:mm:ss
	 * @param timeZone [optional] timeZone
	 * @return
	 */
	private String formatDate(Date date, String pattern, String... timeZone) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(pattern == null ? DATE_TIME_FORMAT : pattern);
		if (timeZone != null && timeZone.length > 0) {
			sdf.setTimeZone(TimeZone.getTimeZone(timeZone[0]));
		} else {
			sdf.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
		}
		return sdf.format(date);
	}

}
