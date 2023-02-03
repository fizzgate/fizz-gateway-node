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

import org.apache.commons.codec.binary.Hex;

import java.security.MessageDigest;

/**
 * @author hongqiaowei
 */

public abstract class DigestUtils extends org.apache.commons.codec.digest.DigestUtils {

    static final String md5digest = "m5dgtT";

    private DigestUtils() {
    }

    public static String md532(String source) {
        byte[] srcBytes = source.getBytes();
        MessageDigest md = (MessageDigest) ThreadContext.get(md5digest);
        if (md == null) {
            md = getMd5Digest();
            ThreadContext.set(md5digest, md);
        } else {
            md.reset();
        }
        md.update(srcBytes);
        byte[] resultBytes = md.digest();
        return Hex.encodeHexString(resultBytes);
    }

    public static String md516(String source) {
        return md532(source).substring(8, 24);
    }

    // public static void main(String[] args) {
    //     StringBuilder b = new StringBuilder(128);
    //     long now = System.currentTimeMillis();
    //     String app       = "appx";
    //     String timestamp = "" + now;
    //     String secretKey = "fb3c057fe6134796acf33d53f240f2a9";
    //     b.append(app).append(Constants.Symbol.UNDERLINE).append(timestamp).append(Constants.Symbol.UNDERLINE).append(secretKey);
    //     System.err.println("timestamp: " + timestamp + ", sign: " + md532(b.toString()));
    // }
}
