package com.github.serezhka.jap2lib;

import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.Utils;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;

class Pairing {

    private static final Logger log = LoggerFactory.getLogger(Pairing.class);

    private final KeyPair keyPair;

    private byte[] edTheirs;
    private byte[] ecdhOurs;
    private byte[] ecdhTheirs;
    private byte[] ecdhSecret;

    private boolean pairVerified;

    Pairing() {
        this.keyPair = new KeyPairGenerator().generateKeyPair();
    }

    void info(OutputStream out) throws IOException {
        NSArray displays = new NSArray(1);
        NSDictionary display = new NSDictionary();
        display.put("features", 14);
        display.put("height", 1080);
        display.put("heightPhysical", false);
        display.put("heightPixels", 1080);
        display.put("maxFPS", 30);
        display.put("overscanned", false);
        display.put("refreshRate", 60);
        display.put("rotation", false);
        display.put("uuid", "e5f7a68d-7b0f-4305-984b-974f677a150b");
        display.put("width", 1920);
        display.put("widthPhysical", false);
        display.put("widthPixels", 1920);
        displays.setValue(0, display);

        NSDictionary serverInfo = new NSDictionary();
        serverInfo.put("displays", displays);
        serverInfo.put("features", 130367356919L);
        serverInfo.put("keepAliveSendStatsAsBody", 1);
        serverInfo.put("model", "AppleTV2,1");
        serverInfo.put("name", "Apple TV");
        serverInfo.put("pi", "b08f5a79-db29-4384-b456-a4784d9e6055");
        serverInfo.put("sourceVersion", "220.68");
        serverInfo.put("statusFlags", 68);
        serverInfo.put("vv", 2);
        BinaryPropertyListWriter.write(out, serverInfo);
    }

    void pairSetup(OutputStream out) throws IOException {
        out.write(((EdDSAPublicKey) keyPair.getPublic()).getAbyte());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    void pairVerify(InputStream request, OutputStream response) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, SignatureException, BadPaddingException, IllegalBlockSizeException, IOException {
        int flag = request.read();
        if (flag > 0) {
            request.skip(3);
            request.read(ecdhTheirs = new byte[32]);
            request.read(edTheirs = new byte[32]);

            Curve25519 curve25519 = Curve25519.getInstance(Curve25519.BEST);
            Curve25519KeyPair curve25519KeyPair = curve25519.generateKeyPair();

            ecdhOurs = curve25519KeyPair.getPublicKey();
            ecdhSecret = curve25519.calculateAgreement(ecdhTheirs, curve25519KeyPair.getPrivateKey());
            log.info("Shared secret: " + Utils.bytesToHex(ecdhSecret));

            Cipher aesCtr128Encrypt = initCipher();

            byte[] dataToSign = new byte[64];
            System.arraycopy(ecdhOurs, 0, dataToSign, 0, 32);
            System.arraycopy(ecdhTheirs, 0, dataToSign, 32, 32);

            EdDSAEngine edDSAEngine = new EdDSAEngine();
            edDSAEngine.initSign(keyPair.getPrivate());
            byte[] signature = edDSAEngine.signOneShot(dataToSign);

            byte[] encryptedSignature = aesCtr128Encrypt.doFinal(signature);

            byte[] responseContent = new byte[ecdhOurs.length + encryptedSignature.length];
            System.arraycopy(ecdhOurs, 0, responseContent, 0, ecdhOurs.length);
            System.arraycopy(encryptedSignature, 0, responseContent, ecdhOurs.length, encryptedSignature.length);

            response.write(responseContent);
        } else {
            request.skip(3);
            byte[] signature = new byte[64];
            request.read(signature);

            Cipher aesCtr128Encrypt = initCipher();

            byte[] sigBuffer = new byte[64];
            aesCtr128Encrypt.update(sigBuffer);
            sigBuffer = aesCtr128Encrypt.doFinal(signature);

            byte[] sigMessage = new byte[64];
            System.arraycopy(ecdhTheirs, 0, sigMessage, 0, 32);
            System.arraycopy(ecdhOurs, 0, sigMessage, 32, 32);

            EdDSAEngine edDSAEngine = new EdDSAEngine();
            EdDSAPublicKey edDSAPublicKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(edTheirs, EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)));
            edDSAEngine.initVerify(edDSAPublicKey);

            pairVerified = edDSAEngine.verifyOneShot(sigMessage, sigBuffer);
            log.info("Pair verified: " + pairVerified);
        }
    }

    boolean isPairVerified() {
        return pairVerified;
    }

    byte[] getSharedSecret() {
        return ecdhSecret;
    }

    private Cipher initCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
        sha512Digest.update("Pair-Verify-AES-Key".getBytes(StandardCharsets.UTF_8));
        sha512Digest.update(ecdhSecret);
        byte[] sharedSecretSha512AesKey = Arrays.copyOfRange(sha512Digest.digest(), 0, 16);

        sha512Digest.update("Pair-Verify-AES-IV".getBytes(StandardCharsets.UTF_8));
        sha512Digest.update(ecdhSecret);
        byte[] sharedSecretSha512AesIV = Arrays.copyOfRange(sha512Digest.digest(), 0, 16);

        Cipher aesCtr128Encrypt = Cipher.getInstance("AES/CTR/NoPadding");
        aesCtr128Encrypt.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sharedSecretSha512AesKey, "AES"), new IvParameterSpec(sharedSecretSha512AesIV));
        return aesCtr128Encrypt;
    }
}
