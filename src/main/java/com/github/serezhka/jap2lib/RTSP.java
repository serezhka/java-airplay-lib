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

    void rtspSetup(InputStream in, OutputStream out, int dataPort, int eventPort, int timingPort) throws Exception {
        NSDictionary request = (NSDictionary) BinaryPropertyListParser.parse(in);

        if (request.containsKey("ekey")) {
            encryptedAESKey = (byte[]) request.get("ekey").toJavaObject();
        }

        if (request.containsKey("streams")) {
            HashMap stream = (HashMap) ((Object[]) request.get("streams").toJavaObject())[0];

            if (stream.containsKey("streamConnectionID")) {
                streamConnectionID = Long.toUnsignedString((long) stream.get("streamConnectionID"));

                NSArray streams = new NSArray(1);
                NSDictionary dataStream = new NSDictionary();
                dataStream.put("dataPort", dataPort);
                dataStream.put("type", 110);
                streams.setValue(0, dataStream);

                NSDictionary response = new NSDictionary();
                response.put("streams", streams);
                response.put("eventPort", eventPort);
                response.put("timingPort", timingPort);
                BinaryPropertyListWriter.write(out, response);
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
