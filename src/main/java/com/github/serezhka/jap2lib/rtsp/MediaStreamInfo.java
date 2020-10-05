package com.github.serezhka.jap2lib.rtsp;

public interface MediaStreamInfo {

    StreamType getStreamType();

    enum StreamType {
        AUDIO,
        VIDEO
    }
}
