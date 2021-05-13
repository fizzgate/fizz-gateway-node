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

    public Throwable t;

    public Map<Object, Object> context;

    public ReactiveResult() {
        super();
    }

    public ReactiveResult(int code, String msg, D data, Throwable t) {
        super(code, msg, data);
        this.t = t;
    }

    public static ReactiveResult succ() {
        return new ReactiveResult(SUCC, null, null, null);
    }

    public static <D> ReactiveResult<D> succ(D data) {
        ReactiveResult rr = succ();
        rr.data = data;
        return rr;
    }

    public static ReactiveResult fail() {
        return new ReactiveResult(FAIL, null, null, null);
    }

    public static ReactiveResult fail(String msg) {
        ReactiveResult rr = fail();
        rr.msg = msg;
        return rr;
    }

    public static ReactiveResult fail(Throwable t) {
        ReactiveResult rr = fail();
        rr.t = t;
        return rr;
    }

    public static ReactiveResult with(int code) {
        return new ReactiveResult(code, null, null, null);
    }

    public static ReactiveResult with(int code, String msg) {
        ReactiveResult rr = with(code);
        rr.msg = msg;
        return rr;
    }

    public static <D> ReactiveResult<D> with(int code, D data) {
        ReactiveResult rr = with(code);
        rr.data = data;
        return rr;
    }

    public static <D> ReactiveResult<D> with(int code, Throwable t) {
        ReactiveResult rr = with(code);
        rr.t = t;
        return rr;
    }

    @Override
    public void toStringBuilder(StringBuilder b) {
        super.toStringBuilder(b);
        b.append(',');
        b.append("context:")  .append(context).append(',');
        b.append("throwable:").append(t == null ? t : t.getMessage());
    }
}
