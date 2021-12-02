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

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * @author hongqiaowei
 */

public class SymmetricEncryptor {

    public  final SymmetricAlgorithm algorithm;

    public  final String             secretKey;

    private final Cipher             cipher;

    private final boolean            isZeroPadding;

    public SymmetricEncryptor(SymmetricAlgorithm algorithm, String key) {
        this.algorithm = algorithm;
        secretKey = key;
        cipher = SymmetricCryptoUtils.createCipher(algorithm, key, Cipher.ENCRYPT_MODE);
        isZeroPadding = SymmetricCryptoUtils.isZeroPadding(algorithm);
    }

    public byte[] encrypt(byte[] data) {
        if (isZeroPadding) {
            data = SymmetricCryptoUtils.paddingWith0(data, cipher.getBlockSize());
        }
        try {
            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] encrypt(String data) {
        return encrypt(StrUtil.bytes(data, CharsetUtil.CHARSET_UTF_8));
    }

    public String base64encrypt(String data) {
        return Base64.encode(encrypt(data));
    }

    public String hexEncrypt(String data) {
        return HexUtil.encodeHexStr(encrypt(data));
    }
}
