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
package com.wehotel.util;

import com.wehotel.util.Constants.DatetimePattern;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lancer
 */

public abstract class DateTimeUtils {

	private static       Map<String, DateTimeFormatter> dateTimeFormatters = new HashMap<>();

	private static       ZoneId                         defaultZone        = ZoneId.systemDefault();
	
	private static final String                         zeroTimeSuffix     = " 00:00:00";

	public static DateTimeFormatter getDateTimeFormatter(String pattern) {
		DateTimeFormatter f = dateTimeFormatters.get(pattern);
		if (f == null) {
			f = DateTimeFormatter.ofPattern(pattern);
			dateTimeFormatters.put(pattern, f);
		}
		return f;
	}

	public static Date from(Instant i) {
		return new Date(i.toEpochMilli());
	}

	public static Date from(LocalDateTime ldt) {
		return from(ldt.atZone(defaultZone).toInstant());
	}

	public static LocalDateTime from(Date d) {
		return LocalDateTime.ofInstant(d.toInstant(), defaultZone);
	}

	public static LocalDateTime from(long l) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), defaultZone);
	}

	public static long toMillis(LocalDateTime ldt) {
		return ldt.atZone(defaultZone).toInstant().toEpochMilli();
	}

	public static long toMillis(String dateTime, String... pattern) {
		if (dateTime.length() == 10) {
			dateTime += zeroTimeSuffix;
		}
		String p = DatetimePattern.DP19;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		DateTimeFormatter f = getDateTimeFormatter(p);
		LocalDateTime ldt = LocalDateTime.parse(dateTime, f);
		return ldt.atZone(defaultZone).toInstant().toEpochMilli();
	}

	public static String toDate(long mills, String... pattern) {
		String p = DatetimePattern.DP10;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(mills), defaultZone);
		DateTimeFormatter f = getDateTimeFormatter(p);
		return ldt.format(f);
	}
	
	public static long until(LocalDate thatDate) {
		return LocalDate.now().until(thatDate, ChronoUnit.DAYS);
	}

	public static long from(LocalDate thatDate) {
		return thatDate.until(LocalDate.now(), ChronoUnit.DAYS);
	}

	public static long until(LocalDate startDate, LocalDate endDate) {
		return startDate.until(endDate, ChronoUnit.DAYS);
	}
	
	public static LocalDate date2localDate(Date date) {
		return date.toInstant().atZone(defaultZone).toLocalDate();
	}

	public static Date localDate2date(LocalDate localDate) {
		ZonedDateTime zonedDateTime = localDate.atStartOfDay(defaultZone);
		return Date.from(zonedDateTime.toInstant());
	}
	
	public static String localDate2str(LocalDate date, String... pattern) {
		String p = DatetimePattern.DP10;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		DateTimeFormatter f = getDateTimeFormatter(p);
		return date.format(f);
	}

	public static String localDateTime2str(LocalDateTime localDateTime, String... pattern) {
		String p = DatetimePattern.DP23;
		if (pattern.length != 0) {
			p = pattern[0];
		}
		DateTimeFormatter f = getDateTimeFormatter(p);
		return localDateTime.format(f);
	}
	
	public static List<String> datesBetween(String start, String end) {
		LocalDate sd = LocalDate.parse(start);
		LocalDate ed = LocalDate.parse(end);
		long dist = ChronoUnit.DAYS.between(sd, ed);
		if (dist == 0) {
			return Collections.EMPTY_LIST;
		} else if (dist < 0) {
			LocalDate x = ed;
			ed = sd;
			sd = x;
			dist = Math.abs(dist);
		}
		long max = dist + 1;
		return Stream.iterate(sd, d -> {
			return d.plusDays(1);
		}).limit(max).map(LocalDate::toString).collect(Collectors.toList());
	}
	
	public static class LocalDateAndStr {
		public LocalDate d;
		public String s;

		public LocalDateAndStr(LocalDate d, String s) {
			this.d = d;
			this.s = s;
		}
	}
	
	public static List<LocalDateAndStr> datesBetween0(String start, String end) {
		LocalDate sd = LocalDate.parse(start);
		LocalDate ed = LocalDate.parse(end);
		long dist = ChronoUnit.DAYS.between(sd, ed);
		if (dist == 0) {
			return Collections.EMPTY_LIST;
		} else if (dist < 0) {
			LocalDate x = ed;
			ed = sd;
			sd = x;
			dist = Math.abs(dist);
		}
		long max = dist + 1;
		return Stream.iterate(sd, d -> {
			return d.plusDays(1);
		}).limit(max).map((LocalDate e) -> {
			return new LocalDateAndStr(e, e.toString());
		}).collect(Collectors.toList());
	}

	public static LocalDate beforeNow(long offsetDays) {
		return LocalDate.now().minusDays(offsetDays);
	}

	public static LocalDateTime beforeNowNoTime(long offsetDays) {
		return LocalDate.now().minusDays(offsetDays).atTime(0, 0, 0, 0);
	}
}
