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

public class Result<D> {

    public static final int SUCC = 1;
    public static final int FAIL = 0;

    public int code = -1;

    public String msg;

    public D data;

    public Result() {
    }

    public Result(int code, String msg, D data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result succ() {
        return new Result(SUCC, null, null);
    }

    public static <D> Result<D> succ(D data) {
        Result r = succ();
        r.data = data;
        return r;
    }

    public static Result fail() {
        return new Result(FAIL, null, null);
    }

    public static Result fail(String msg) {
        Result r = fail();
        r.msg = msg;
        return r;
    }

    public static Result with(int code) {
        return new Result(code, null, null);
    }

    public static Result with(int code, String msg) {
        Result r = with(code);
        r.msg = msg;
        return r;
    }

    public static <D> Result<D> with(int code, D data) {
        Result r = with(code);
        r.data = data;
        return r;
    }

    // @Override
    // public String toString() {
    //     return JacksonUtils.writeValueAsString(this);
    // }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        toStringBuilder(b);
        return b.toString();
    }

    public void toStringBuilder(StringBuilder b) {
        b.append("code:").append(code).append(',');
        b.append("msg:") .append(msg) .append(',');
        b.append("data:").append(data);
    }
}
