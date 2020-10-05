package com.github.serezhka.jap2lib.rtsp;

public class VideoStreamInfo implements MediaStreamInfo {

    private final String streamConnectionID;

    public VideoStreamInfo(String streamConnectionID) {
        this.streamConnectionID = streamConnectionID;
    }

    @Override
    public StreamType getStreamType() {
        return MediaStreamInfo.StreamType.VIDEO;
    }

    public String getStreamConnectionID() {
        return streamConnectionID;
    }
}
