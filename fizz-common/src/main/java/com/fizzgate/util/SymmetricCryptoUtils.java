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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * @author hongqiaowei
 */

public abstract class SymmetricCryptoUtils {

    private SymmetricCryptoUtils() {
    }

    public static Cipher createCipher(SymmetricAlgorithm algorithm, String key, int mode) {
        byte[] keyBytes = SecureUtil.decode(key);
        String algorithmName = algorithm.getValue();
        SecretKey secretKey = KeyUtil.generateKey(algorithmName, keyBytes);

        Cipher cipher = SecureUtil.createCipher(algorithmName);

        byte[] iv = cipher.getIV();
        AlgorithmParameterSpec parameterSpec = null;
        if (StrUtil.startWithIgnoreCase(algorithmName, "AES")) {
            if (iv != null) {
                parameterSpec = new IvParameterSpec(iv);
            }
        } else if (StrUtil.startWithIgnoreCase(algorithmName, "PBE")) {
            if (null == iv) {
                iv = RandomUtil.randomBytes(8);
            }
            parameterSpec = new PBEParameterSpec(iv, 100);
        }

        try {
            if (null == parameterSpec) {
                cipher.init(mode, secretKey);
            } else {
                cipher.init(mode, secretKey, parameterSpec);
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return cipher;
    }

    public static boolean isZeroPadding(SymmetricAlgorithm algorithm) {
        String algorithmName = algorithm.getValue();
        return algorithmName.contains(Padding.ZeroPadding.name());
    }

    public static byte[] paddingWith0(byte[] data, int blockSize) {
        int length = data.length;
        int remainLength = length % blockSize;
        if (remainLength > 0) {
            return ArrayUtil.resize(data, length + blockSize - remainLength);
        } else {
            return data;
        }
    }

    public static byte[] removePadding(byte[] data, int blockSize) {
        if (blockSize > 0) {
            int length = data.length;
            int remainLength = length % blockSize;
            if (remainLength == 0) {
                int i = length - 1;
                while (i >= 0 && 0 == data[i]) {
                    i--;
                }
                return ArrayUtil.resize(data, i + 1);
            }
        }
        return data;
    }
}
