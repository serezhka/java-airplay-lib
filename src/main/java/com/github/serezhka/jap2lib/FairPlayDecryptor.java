package com.github.serezhka.jap2lib;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

class FairPlayDecryptor {

    private Cipher aesCtr128Decrypt;
    private int nextDecryptCount;
    private byte[] og = new byte[16];

    FairPlayDecryptor(byte[] aesKey, byte[] sharedSecret, String streamConnectionID) throws Exception {
        MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
        sha512Digest.update(aesKey);
        sha512Digest.update(sharedSecret);
        byte[] eaesKey = sha512Digest.digest();

        byte[] skey = ("AirPlayStreamKey" + streamConnectionID).getBytes(StandardCharsets.UTF_8);
        sha512Digest.update(skey);
        sha512Digest.update(eaesKey, 0, 16);
        byte[] hash1 = sha512Digest.digest();

        byte[] siv = ("AirPlayStreamIV" + streamConnectionID).getBytes(StandardCharsets.UTF_8);
        sha512Digest.update(siv);
        sha512Digest.update(eaesKey, 0, 16);
        byte[] hash2 = sha512Digest.digest();

        byte[] decryptAesKey = new byte[16];
        byte[] decryptAesIV = new byte[16];
        System.arraycopy(hash1, 0, decryptAesKey, 0, 16);
        System.arraycopy(hash2, 0, decryptAesIV, 0, 16);

        aesCtr128Decrypt = Cipher.getInstance("AES/CTR/NoPadding");
        aesCtr128Decrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptAesKey, "AES"), new IvParameterSpec(decryptAesIV));
    }

    byte[] decrypt(byte[] input) throws Exception {
        if (nextDecryptCount > 0) {
            for (int i = 0; i < nextDecryptCount; i++) {
                input[i] = (byte) (input[i] ^ og[(16 - nextDecryptCount) + i]);
            }
        }

        int encryptlen = ((input.length - nextDecryptCount) / 16) * 16;
        aesCtr128Decrypt.update(input, nextDecryptCount, encryptlen, input, nextDecryptCount);
        System.arraycopy(input, nextDecryptCount, input, nextDecryptCount, encryptlen);

        int restlen = (input.length - nextDecryptCount) % 16;
        int reststart = input.length - restlen;
        nextDecryptCount = 0;
        if (restlen > 0) {
            Arrays.fill(og, (byte) 0);
            System.arraycopy(input, reststart, og, 0, restlen);
            aesCtr128Decrypt.update(og, 0, 16, og, 0);
            System.arraycopy(og, 0, input, reststart, restlen);
            nextDecryptCount = 16 - restlen;
        }

        return input;
    }
}
