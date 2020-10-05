package com.github.serezhka.jap2lib.rtsp;

public class AudioStreamInfo implements MediaStreamInfo {

    private final AudioFormat audioFormat;

    public AudioStreamInfo() {
        audioFormat = null;
    }

    public AudioStreamInfo(long audioFormatCode) {
        this.audioFormat = AudioFormat.fromCode(audioFormatCode);
    }

    @Override
    public StreamType getStreamType() {
        return StreamType.AUDIO;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public enum AudioFormat {
        PCM_8000_16_1(0x4),
        PCM_8000_16_2(0x8),
        PCM_16000_16_1(0x10),
        PCM_16000_16_2(0x20),
        PCM_24000_16_1(0x40),
        PCM_24000_16_2(0x80),
        PCM_32000_16_1(0x100),
        PCM_32000_16_2(0x200),
        PCM_44100_16_1(0x400),
        PCM_44100_16_2(0x800),
        PCM_44100_24_1(0x1000),
        PCM_44100_24_2(0x2000),
        PCM_48000_16_1(0x4000),
        PCM_48000_16_2(0x8000),
        PCM_48000_24_1(0x10000),
        PCM_48000_24_2(0x20000),
        ALAC_44100_16_2(0x40000),
        ALAC_44100_24_2(0x80000),
        ALAC_48000_16_2(0x100000),
        ALAC_48000_24_2(0x200000),
        AAC_LC_44100_2(0x400000),
        AAC_LC_48000_2(0x800000),
        AAC_ELD_44100_2(0x1000000),
        AAC_ELD_48000_2(0x2000000),
        AAC_ELD_16000_1(0x4000000),
        AAC_ELD_24000_1(0x8000000),
        OPUS_16000_1(0x10000000),
        OPUS_24000_1(0x20000000),
        OPUS_48000_1(0x40000000),
        AAC_ELD_44100_1(0x80000000),
        AAC_ELD_48000_1(0x100000000L);

        private final long code;

        AudioFormat(long code) {
            this.code = code;
        }

        public static AudioFormat fromCode(long code) {
            for (AudioFormat format : AudioFormat.values()) {
                if (format.code == code) {
                    return format;
                }
            }
            throw new IllegalArgumentException("Unknown audio format with code: " + code);
        }
    }
}
