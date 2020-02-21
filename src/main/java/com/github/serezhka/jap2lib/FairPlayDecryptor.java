package com.github.serezhka.jap2lib;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

class FairPlayDecryptor {

    private final byte[] aesKey;
    private final byte[] aesIV;
    private final byte[] sharedSecret;
    private final String streamConnectionID;

    private Cipher aesCtrDecrypt;
    private Cipher aesCbcDecrypt;
    private int nextDecryptCount;
    private byte[] og = new byte[16];

    FairPlayDecryptor(byte[] aesKey, byte[] aesIV, byte[] sharedSecret, String streamConnectionID) throws Exception {
        this.aesKey = aesKey;
        this.aesIV = aesIV;
        this.sharedSecret = sharedSecret;
        this.streamConnectionID = streamConnectionID;

        aesCtrDecrypt = Cipher.getInstance("AES/CTR/NoPadding");
        aesCbcDecrypt = Cipher.getInstance("AES/CBC/NoPadding");

        initAesCtrCipher();
    }

    void decryptVideoData(byte[] videoData) throws Exception {
        if (nextDecryptCount > 0) {
            for (int i = 0; i < nextDecryptCount; i++) {
                videoData[i] = (byte) (videoData[i] ^ og[(16 - nextDecryptCount) + i]);
            }
        }

        int encryptlen = ((videoData.length - nextDecryptCount) / 16) * 16;
        aesCtrDecrypt.update(videoData, nextDecryptCount, encryptlen, videoData, nextDecryptCount);
        System.arraycopy(videoData, nextDecryptCount, videoData, nextDecryptCount, encryptlen);

        int restlen = (videoData.length - nextDecryptCount) % 16;
        int reststart = videoData.length - restlen;
        nextDecryptCount = 0;
        if (restlen > 0) {
            Arrays.fill(og, (byte) 0);
            System.arraycopy(videoData, reststart, og, 0, restlen);
            aesCtrDecrypt.update(og, 0, 16, og, 0);
            System.arraycopy(og, 0, videoData, reststart, restlen);
            nextDecryptCount = 16 - restlen;
        }
    }

    void decryptAudioData(byte[] audioData) throws Exception {
        initAesCbcCipher();
        int encryptedlen = audioData.length / 16 * 16;
        aesCbcDecrypt.update(audioData, 0, encryptedlen, audioData, 0);
    }

    private void initAesCtrCipher() throws Exception {
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

        aesCtrDecrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decryptAesKey, "AES"), new IvParameterSpec(decryptAesIV));
    }

    private void initAesCbcCipher() throws Exception {
        MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
        sha512Digest.update(aesKey);
        sha512Digest.update(sharedSecret);
        byte[] eaesKey = Arrays.copyOfRange(sha512Digest.digest(), 0, 16);

        aesCbcDecrypt.init(Cipher.DECRYPT_MODE, new SecretKeySpec(eaesKey, "AES"), new IvParameterSpec(aesIV));
    }
}
