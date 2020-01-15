package com.github.serezhka.jap2lib;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Responds on pairing setup, fairplay setup requests, decrypts data
 */
public class AirPlay {

    private final Pairing pairing;
    private final FairPlay fairplay;
    private final RTSP rtsp;

    private FairPlayDecryptor fairPlayDecryptor;

    public AirPlay() {
        pairing = new Pairing();
        fairplay = new FairPlay();
        rtsp = new RTSP();
    }

    /**
     * {@code /info}
     * <p>
     * Writes server info to output stream
     */
    public void info(OutputStream out) throws Exception {
        pairing.info(out);
    }

    /**
     * {@code /pair-setup}
     * <p>
     * Writes EdDSA public key bytes to output stream
     */
    public void pairSetup(OutputStream out) throws Exception {
        pairing.pairSetup(out);
    }

    /**
     * {@code /pair-verify}
     * <p>
     * On first request writes curve25519 public key + encrypted signature bytes to output stream;
     * On second request verifies signature
     */
    public void pairVerify(InputStream in, OutputStream out) throws Exception {
        pairing.pairVerify(in, out);
    }

    /**
     * Pair was verified successfully
     */
    public boolean isPairVerified() {
        return pairing.isPairVerified();
    }

    /**
     * {@code /fp-setup}
     * <p>
     * Writes fp-setup response bytes to output stream
     */
    public void fairPlaySetup(InputStream in, OutputStream out) throws Exception {
        fairplay.fairPlaySetup(in, out);
    }

    /**
     * {@code RTSP SETUP}
     * <p>
     * Writes RSTP SETUP response bytes to output stream
     */
    public void rtspSetup(InputStream in, OutputStream out, int dataPort, int eventPort, int timingPort) throws Exception {
        rtsp.rtspSetup(in, out, dataPort, eventPort, timingPort);
    }

    public byte[] getFairPlayAesKey() {
        return fairplay.decryptAesKey(rtsp.getEncryptedAESKey());
    }

    /**
     * @return {@code true} if we got shared secret during pairing & stream connection id during fair play setup
     */
    public boolean isFairPlayReady() {
        return pairing.getSharedSecret() != null && rtsp.getStreamConnectionID() != null;
    }

    public byte[] fairPlayDecrypt(byte[] input) throws Exception {
        if (fairPlayDecryptor == null) {
            if (!isFairPlayReady()) {
                throw new IllegalStateException("FairPlay not ready!");
            }
            fairPlayDecryptor = new FairPlayDecryptor(getFairPlayAesKey(), pairing.getSharedSecret(), rtsp.getStreamConnectionID());
        }
        return fairPlayDecryptor.decrypt(input);
    }
}
