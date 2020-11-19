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

package we.legacy;

import org.springframework.lang.Nullable;

import we.util.Constants;
import we.util.ThreadContext;

/**
 * @author hongqiaowei
 */

public class RespEntity {

    private static final String f0 = "{\"msgCode\":";
    private static final String f1 = ",\"message\":\"";
    private static final String f2 = "\"}";

    public int msgCode;

    public String message;

    public String reqId;
    
    public Object _context;

    public RespEntity(int code, String msg, @Nullable String reqId) {
        msgCode = code;
        message = msg;
        this.reqId = reqId;
    }
    
    public RespEntity(int code, String msg, @Nullable String reqId, Object stepContext) {
        msgCode = code;
        message = msg;
        this.reqId = reqId;
        this._context = stepContext;
    }

    private static final String resb = "$resb";

    static {
        StringBuilder b = new StringBuilder(128);
        ThreadContext.set(resb, b);
    }

    @Override
    public String toString() {
        StringBuilder b = ThreadContext.getStringBuilder(resb);
        return b.append(f0).append(msgCode).append(f1).append(reqId).append(Constants.Symbol.SPACE).append(message).append(f2).toString();
    }

    public static String toJson(int code, String msg, @Nullable String reqId) {
        return new RespEntity(code, msg, reqId).toString();
    }
}
