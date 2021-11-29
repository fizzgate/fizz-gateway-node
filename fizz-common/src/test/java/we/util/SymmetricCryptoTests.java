package we.util;

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SymmetricCryptoTests {

    @Test
    public void encryptTest() {
        String secretKey = "1gG1dVcEaQz8JyifTHeEnQ==";
        SymmetricEncryptor symmetricEncryptor = new SymmetricEncryptor(SymmetricAlgorithm.AES, secretKey);
        String encrypt = symmetricEncryptor.base64encrypt("abc");

        SymmetricDecryptor symmetricDecryptor = new SymmetricDecryptor(SymmetricAlgorithm.AES, secretKey);
        String decrypt = symmetricDecryptor.decrypt(encrypt);

        Assertions.assertEquals("abc", decrypt);

        byte[] encryptBytes = symmetricEncryptor.encrypt("123".getBytes());
        byte[] decryptBytes = symmetricDecryptor.decrypt(encryptBytes);
        Assertions.assertEquals("123", new String(decryptBytes));
    }
}
