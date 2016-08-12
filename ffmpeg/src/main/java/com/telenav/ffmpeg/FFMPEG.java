package com.telenav.ffmpeg;

public class FFMPEG {
    private static final String TAG = "FFMPEG";

    static {
//        System.loadLibrary("x264");
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
    public native int initial(int width, int height, String filePath);

    public native int encode(byte[] yuvimage);

    public native int flush();

    public native int close();


    public native int nextFile(String s, int width, int height);
}
