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

/**
 * @author hongqiaowei
 */

public class ReactiveResult<D> {

    public static final int SUCC = 1;
    public static final int FAIL = 0;

    public int code = -1;

    public String msg;

    public D data;

    public Throwable t;

    public ReactiveResult(int code, String msg, D data, Throwable t) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.t = t;
    }

    public static ReactiveResult succ() {
        return new ReactiveResult(SUCC, null, null, null);
    }

    public static <D> ReactiveResult<D> succ(D data) {
        return new ReactiveResult<D>(SUCC, null, data, null);
    }

    public static ReactiveResult fail() {
        return new ReactiveResult(FAIL, null, null, null);
    }

    public static ReactiveResult fail(String msg) {
        return new ReactiveResult(FAIL, msg, null, null);
    }

    public static ReactiveResult fail(Throwable t) {
        return new ReactiveResult(FAIL, null, null, t);
    }

    public static ReactiveResult with(int code) {
        return new ReactiveResult(code, null, null, null);
    }

    public static ReactiveResult with(int code, String msg) {
        return new ReactiveResult(code, msg, null, null);
    }

    public static <D> ReactiveResult<D> with(int code, D data) {
        return new ReactiveResult<D>(code, null, data, null);
    }

    @Override
    public String toString() {
        return JacksonUtils.writeValueAsString(this);
    }
}
