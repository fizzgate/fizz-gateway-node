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

import java.util.Map;

/**
 * @author hongqiaowei
 */

public class ReactiveResult<D> extends Result<D> {

    public Map<Object, Object> context;

    public ReactiveResult() {
        super();
    }

    public ReactiveResult(int code, String msg, D data, Throwable t, Map<Object, Object> context) {
        super(code, msg, data, t);
        this.context = context;
    }

    public static <D> ReactiveResult<D> succ() {
        return new ReactiveResult<D>(SUCC, null, null, null, null);
    }

    public static <D> ReactiveResult<D> succ(D data) {
        ReactiveResult<D> r = succ();
        r.data = data;
        return r;
    }

    public static <D> ReactiveResult<D> fail() {
        return new ReactiveResult<D>(FAIL, null, null, null, null);
    }

    public static <D> ReactiveResult<D> fail(String msg) {
        ReactiveResult<D> r = fail();
        r.msg = msg;
        return r;
    }

    public static <D> ReactiveResult<D> fail(Throwable t) {
        ReactiveResult<D> r = fail();
        r.t = t;
        return r;
    }

    public static <D> ReactiveResult<D> with(int code) {
        return new ReactiveResult<D>(code, null, null, null, null);
    }

    public static <D> ReactiveResult<D> with(int code, String msg) {
        ReactiveResult<D> r = with(code);
        r.msg = msg;
        return r;
    }

    public static <D> ReactiveResult<D> with(int code, D data) {
        ReactiveResult<D> r = with(code);
        r.data = data;
        return r;
    }

    public static <D> ReactiveResult<D> with(int code, Throwable t) {
        ReactiveResult<D> r = with(code);
        r.t = t;
        return r;
    }

    @Override
    public void toStringBuilder(StringBuilder b) {
        super.toStringBuilder(b);
        if (context != null) {
            b.append(',').append("ctx=").append(context);
        }
    }
}
