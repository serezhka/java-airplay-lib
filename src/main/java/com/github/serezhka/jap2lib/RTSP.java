package com.github.serezhka.jap2lib;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2lib.rtsp.MediaStreamInfo;
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo;
import net.i2p.crypto.eddsa.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

class RTSP {

    private static final Logger log = LoggerFactory.getLogger(RTSP.class);

    private String streamConnectionID;
    private byte[] encryptedAESKey;
    private byte[] eiv;

    MediaStreamInfo getMediaStreamInfo(InputStream rtspSetupPayload) throws Exception {
        NSDictionary rtspSetup = (NSDictionary) BinaryPropertyListParser.parse(rtspSetupPayload);

        log.debug("Binary property list parsed:\n{}", rtspSetup.toXMLPropertyList());

        if (rtspSetup.containsKey("streams")) {
            // assume one stream info per RTSP SETUP request
            HashMap stream = (HashMap) ((Object[]) rtspSetup.get("streams").toJavaObject())[0];
            int type = (int) stream.get("type");
            switch (type) {

                // video
                case 110:
                    if (stream.containsKey("streamConnectionID")) {
                        streamConnectionID = Long.toUnsignedString((long) stream.get("streamConnectionID"));
                    }
                    return new VideoStreamInfo(streamConnectionID);

                // audio
                case 96:
                    AudioStreamInfo.AudioStreamInfoBuilder builder = new AudioStreamInfo.AudioStreamInfoBuilder();
                    if (stream.containsKey("ct")) {
                        int compressionType = (int) stream.get("ct");
                        builder.compressionType(AudioStreamInfo.CompressionType.fromCode(compressionType));
                    }
                    if (stream.containsKey("audioFormat")) {
                        long audioFormatCode = (int) stream.get("audioFormat"); // FIXME int or long ?!
                        builder.audioFormat(AudioStreamInfo.AudioFormat.fromCode(audioFormatCode));
                    }
                    if (stream.containsKey("spf")) {
                        int samplesPerFrame = (int) stream.get("spf");
                        builder.samplesPerFrame(samplesPerFrame);
                    }
                    return builder.build();

                default:
                    log.error("Unknown stream type: {}", type);
            }
        }
        return null;
    }

    void setup(InputStream in) throws Exception {
        NSDictionary request = (NSDictionary) BinaryPropertyListParser.parse(in);

        if (request.containsKey("ekey")) {
            encryptedAESKey = (byte[]) request.get("ekey").toJavaObject();
            log.info("Encrypted AES key: " + Utils.bytesToHex(encryptedAESKey));
        }

        if (request.containsKey("eiv")) {
            eiv = (byte[]) request.get("eiv").toJavaObject();
            log.info("AES eiv: " + Utils.bytesToHex(eiv));
        }
    }

    void setupVideo(OutputStream out, int videoDataPort, int videoEventPort, int videoTimingPort) throws IOException {
        NSArray streams = new NSArray(1);
        NSDictionary dataStream = new NSDictionary();
        dataStream.put("dataPort", videoDataPort);
        dataStream.put("type", 110);
        streams.setValue(0, dataStream);

        NSDictionary response = new NSDictionary();
        response.put("streams", streams);
        response.put("eventPort", videoEventPort);
        response.put("timingPort", videoTimingPort);
        BinaryPropertyListWriter.write(out, response);
    }

    void setupAudio(OutputStream out, int audioDataPort, int audioControlPort) throws IOException {
        NSArray streams = new NSArray(1);
        NSDictionary dataStream = new NSDictionary();
        dataStream.put("dataPort", audioDataPort);
        dataStream.put("type", 96);
        dataStream.put("controlPort", audioControlPort);
        streams.setValue(0, dataStream);

        NSDictionary response = new NSDictionary();
        response.put("streams", streams);
        BinaryPropertyListWriter.write(out, response);
    }

    String getStreamConnectionID() {
        return streamConnectionID;
    }

    byte[] getEncryptedAESKey() {
        return encryptedAESKey;
    }

    byte[] getEiv() {
        return eiv;
    }
}
