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

package we.util;

import java.util.Collections;
import java.util.Map;

/**
 * @author hongqiaowei
 */

public class Result<D> {

    public static final int SUCC = 1;
    public static final int FAIL = 0;

    public int                 code    = -1;

    public String              msg;

    public D                   data;

    public Throwable           t;

    public Map<Object, Object> context = Collections.emptyMap();

    public Result() {
    }

    public Result(int code, String msg, D data, Throwable t, Map<Object, Object> context) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.t = t;
        this.context = context;
    }

    public static <D> Result<D> succ() {
        return new Result<D>(SUCC, null, null, null, null);
    }

    public static <D> Result<D> succ(D data) {
        Result<D> r = succ();
        r.data = data;
        return r;
    }

    public static <D> Result<D> succ(D data, Map<Object, Object> context) {
        Result<D> r = succ(data);
        r.context = context;
        return r;
    }

    public static <D> Result<D> fail() {
        return new Result<D>(FAIL, null, null, null, null);
    }

    public static <D> Result<D> fail(String msg) {
        Result<D> r = fail();
        r.msg = msg;
        return r;
    }

    public static <D> Result<D> fail(String msg, Map<Object, Object> context) {
        Result<D> r = fail(msg);
        r.context = context;
        return r;
    }

    public static <D> Result<D> fail(Throwable t) {
        Result<D> r = fail();
        r.t = t;
        return r;
    }

    public static <D> Result<D> fail(Throwable t, Map<Object, Object> context) {
        Result<D> r = fail(t);
        r.context = context;
        return r;
    }

    public static <D> Result<D> with(int code) {
        return new Result<D>(code, null, null, null, null);
    }

    public static <D> Result<D> with(int code, Map<Object, Object> context) {
        Result<D> r = with(code);
        r.context = context;
        return r;
    }

    public static <D> Result<D> with(int code, String msg) {
        Result<D> r = with(code);
        r.msg = msg;
        return r;
    }

    public static <D> Result<D> with(int code, String msg, Map<Object, Object> context) {
        Result<D> r = with(code, msg);
        r.context = context;
        return r;
    }

    public static <D> Result<D> with(int code, Throwable t) {
        Result<D> r = with(code);
        r.t = t;
        return r;
    }

    public static <D> Result<D> with(int code, Throwable t, Map<Object, Object> context) {
        Result<D> r = with(code, t);
        r.context = context;
        return r;
    }

    public static <D> Result<D> with(int code, D data) {
        Result<D> r = with(code);
        r.data = data;
        return r;
    }

    public static <D> Result<D> with(int code, D data, Map<Object, Object> context) {
        Result<D> r = with(code, data);
        r.context = context;
        return r;
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
