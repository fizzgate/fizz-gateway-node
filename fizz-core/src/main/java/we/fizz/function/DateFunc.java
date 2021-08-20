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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import we.fizz.exception.FizzRuntimeException;

/**
 * Date Functions
 * 
 * @author Francis Dong
 *
 */
public class DateFunc implements IFunc {

	private static final Logger LOGGER = LoggerFactory.getLogger(DateFunc.class);

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
	 * Returns current time with the given pattern<br>
	 * Frequently-used pattern:<br>
	 * yyyy-MM-dd HH:mm:ss<br>
	 * yyyy-MM-dd<br>
	 * HH:mm:ss<br>
	 * HH:mm<br>
	 * yyyy-MM-dd HH:mm:ss Z<br>
	 * 
	 * @param pattern [optional] the pattern describing the date and time format,
	 *                dafault yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public String now(String pattern) {
		return formatDate(new Date(), pattern);
	}

	/**
	 * Adds or subtracts the specified amount of time to the given calendar field,
	 * based on the calendar's rules. For example, to subtract 5 hours from the
	 * current time of the calendar, you can achieve it by calling:
	 * <p>
	 * <code>add("2021-08-04 14:23:12", "yyyy-MM-dd HH:mm:ss", 4, -5)</code>.
	 * 
	 * @param date    date string
	 * @param pattern date pattern of the given date string
	 * @param field   the calendar field, <br>
	 *                1 for millisecond<br>
	 *                2 for second<br>
	 *                3 for minute<br>
	 *                4 for hour<br>
	 *                5 for date<br>
	 *                6 for month<br>
	 *                7 for year<br>
	 * @param amount  the amount of date or time to be added to the field
	 * @return
	 */
	public String add(String date, String pattern, int field, int amount) {
		Date d = parse(date, pattern);
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
			return formatDate(addToFiled(d, calField, amount), pattern);
		}
		return null;
	}

	/**
	 * Format the a timestamp to the given pattern
	 * 
	 * @param timestamp
	 * @param pattern
	 * @return
	 */
	public String formatTs(long timestamp, String pattern) {
		return formatDate(new Date(timestamp), pattern);
	}

	/**
	 * Format the a time with source pattern to the target pattern
	 * 
	 * @param dateStr       date
	 * @param sourcePattern source pattern
	 * @param targetPattern target pattern
	 * @return
	 */
	public String changePattern(String dateStr, String sourcePattern, String targetPattern) {
		return formatDate(parse(dateStr, sourcePattern), targetPattern);
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
	 * @param dateStr String to be parsed
	 * @param pattern pattern of dateStr
	 * @return
	 */
	private Date parse(String dateStr, String pattern) {
		SimpleDateFormat df = new SimpleDateFormat(pattern == null ? DATE_TIME_FORMAT : pattern);
		try {
			return df.parse(dateStr);
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
	 * @param pattern [optional] the pattern describing the date and time format,
	 *                dafault yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	private String formatDate(Date date, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern == null ? DATE_TIME_FORMAT : pattern);
		return sdf.format(date);
	}

}
