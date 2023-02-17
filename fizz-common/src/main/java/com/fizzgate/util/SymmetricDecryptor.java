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

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * @author hongqiaowei
 */

public class SymmetricDecryptor {

    public  final SymmetricAlgorithm algorithm;

    public  final String             secretKey;

    private final Cipher             cipher;

    private final boolean            isZeroPadding;

    public SymmetricDecryptor(SymmetricAlgorithm algorithm, String key) {
        this.algorithm = algorithm;
        secretKey = key;
        cipher = SymmetricCryptoUtils.createCipher(algorithm, key, Cipher.DECRYPT_MODE);
        isZeroPadding = SymmetricCryptoUtils.isZeroPadding(algorithm);
    }

    /**
     * @param data can be hex or base64 string
     */
    public String decrypt(String data) {
        byte[] decode = SecureUtil.decode(data);
        return StrUtil.str(decrypt(decode), CharsetUtil.CHARSET_UTF_8);
    }

    public byte[] decrypt(byte[] data) {
        int blockSize = cipher.getBlockSize();
        byte[] decryptData;
        try {
            decryptData = cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        if (isZeroPadding) {
            return SymmetricCryptoUtils.removePadding(decryptData, blockSize);
        } else {
            return decryptData;
        }
    }
}
