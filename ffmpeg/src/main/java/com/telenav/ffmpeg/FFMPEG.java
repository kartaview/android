package com.telenav.ffmpeg;

public class FFMPEG {
    private static final String TAG = "FFMPEG";

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swscale");
        System.loadLibrary("avfilter");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swresample");
        System.loadLibrary("SDL2");
        System.loadLibrary("encode");
    }

    //JNI
    public native int initial(String folder);

    public native int[] encode(byte[] yuvimage);

    public native int close();
}
