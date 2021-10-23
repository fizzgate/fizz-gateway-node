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

package we.api.pairing;

import we.util.Consts;
import we.util.ThreadContext;

/**
 * @author hongqiaowei
 */

public abstract class ApiPairingUtils extends org.apache.commons.codec.digest.DigestUtils {

    private ApiPairingUtils() {
    }

    public static String sign(String app, String timestamp, String secretKey) {
        StringBuilder b = ThreadContext.getStringBuilder(ThreadContext.sb0);
        b.append(app)      .append(Consts.S.UNDER_LINE)
         .append(timestamp).append(Consts.S.UNDER_LINE)
         .append(secretKey);
        return sha256Hex(b.toString());
    }

    public static String sign(String app, long timestamp, String secretKey) {
        return sign(app, String.valueOf(timestamp), secretKey);
    }

    public static boolean checkSign(String app, String timestamp, String secretKey, String sign) {
        String s = sign(app, timestamp, secretKey);
        return s.equals(sign);
    }

    public static boolean checkSign(String app, long timestamp, String secretKey, String sign) {
        return checkSign(app, String.valueOf(timestamp), secretKey, sign);
    }
}
