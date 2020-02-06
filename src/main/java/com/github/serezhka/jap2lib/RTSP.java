package com.github.serezhka.jap2lib;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

class RTSP {

    private String streamConnectionID;
    private byte[] encryptedAESKey;

    void rtspSetup(InputStream in, OutputStream out,
                   int videoDataPort, int videoEventPort, int videoTimingPort, int audioDataPort, int audioControlPort) throws Exception {
        NSDictionary request = (NSDictionary) BinaryPropertyListParser.parse(in);

        if (request.containsKey("ekey")) {
            encryptedAESKey = (byte[]) request.get("ekey").toJavaObject();
        }

        if (request.containsKey("streams")) {
            HashMap stream = (HashMap) ((Object[]) request.get("streams").toJavaObject())[0]; // iter

            int type = (int) stream.get("type");

            switch (type) {
                case 110: {
                    streamConnectionID = Long.toUnsignedString((long) stream.get("streamConnectionID"));

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
                    break;
                }

                case 96: {

                    NSArray streams = new NSArray(1);
                    NSDictionary dataStream = new NSDictionary();
                    dataStream.put("dataPort", audioDataPort);
                    dataStream.put("type", 96);
                    dataStream.put("controlPort", audioControlPort);
                    streams.setValue(0, dataStream);

                    NSDictionary response = new NSDictionary();
                    response.put("streams", streams);
                    BinaryPropertyListWriter.write(out, response);
                    break;
                }

                default:
                    // todo unknown data type
            }
        }
    }

    String getStreamConnectionID() {
        return streamConnectionID;
    }

    byte[] getEncryptedAESKey() {
        return encryptedAESKey;
    }
}
