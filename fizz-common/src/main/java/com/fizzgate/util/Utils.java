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

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author hongqiaowei
 */

public abstract class Utils {

    private Utils() {
    }

    public static void addTo(StringBuilder b, String k, char c, boolean v,       String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, char v,          String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, int v,           String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, long v,          String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, float v,         String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, double v,        String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, String v,        String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, LocalTime v,     String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, LocalDate v,     String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static void addTo(StringBuilder b, String k, char c, LocalDateTime v, String separator) { b.append(k).append(c).append(v).append(separator); }

    public static void addTo(StringBuilder b, String k, char c, Object v,        String separator) {
        b.append(k).append(c).append(v).append(separator);
    }

    public static String extract(String str, char separator, int nx) {
        int begin = 0, end = 0, n = 0, ny = nx + 1, l = str.length();
        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);
            if (c == separator) {
                n++;
                if (n == nx) {
                    begin = i + 1;
                } else if (n == ny) {
                    end = i;
                    break;
                }
            }
        }
        if (begin == 0) {
            return Consts.S.EMPTY;
        } else if (end == 0) {
            end = l;
        }
        return str.substring(begin, end);
    }

    public static String initials2lowerCase(String s) {
        if (StringUtils.isBlank(s)) {
            return s;
        }
        int cp = s.codePointAt(0);
        if (cp < 65 || cp > 90) {
            return s;
        }
        char[] ca = s.toCharArray();
        ca[0] += 32;
        return String.valueOf(ca);
    }

    public static void threadCurrentStack2stringBuilder(StringBuilder b) {
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        if (stackTraces != null) {
            for (int i = 0; i < stackTraces.length; i++) {
                b.append(stackTraces[i]).append(Consts.S.LF);
            }
        }
    }

    public static RuntimeException runtimeExceptionWithoutStack(String msg) {
        return new RuntimeException(msg, null, false, false) {
        };
    }

    public static Exception exceptionWithoutStack(String msg) {
        return new Exception(msg, null, false, false) {
        };
    }

    public static Throwable throwableWithoutStack(String msg) {
        return new Throwable(msg, null, false, false) {
        };
    }

    public static String getMessage(Throwable t) {
        String message = t.getMessage();
        if (message == null) {
            StringBuilder b = new StringBuilder();
            b.append(t.toString()).append(Consts.S.LF);
            b.append("at ").append(t.getStackTrace()[0].toString());
            message = b.toString();
        }
        return message;
    }
}
