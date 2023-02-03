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

package com.fizzgate.filter;

import java.util.Map;

/**
 * @author hongqiaowei
 */

@Deprecated
public class FilterResult {

    public String id;

    public boolean success;

    public Throwable cause;

    public Map<String, Object> data;

    public Object get(String key) {
        return data.get(key);
    }

    public static final FilterResult SUCCESS(String filter) {
        FilterResult r = new FilterResult();
        r.id = filter;
        r.success = true;
        return r;
    }

    public static final FilterResult SUCCESS_WITH(String filter, Map<String, Object> data) {
        FilterResult r = new FilterResult();
        r.id = filter;
        r.success = true;
        r.data = data;
        return r;
    }

    public static final FilterResult FAIL(String filter) {
        FilterResult r = new FilterResult();
        r.id = filter;
        r.success = false;
        return r;
    }

    public static final FilterResult FAIL_WITH(String filter, Throwable cause) {
        FilterResult r = new FilterResult();
        r.id = filter;
        r.success = false;
        r.cause = cause;
        return r;
    }
}
