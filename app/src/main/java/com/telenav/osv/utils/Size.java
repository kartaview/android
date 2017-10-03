package com.telenav.osv.utils;

import android.hardware.Camera;

/**
 * Holds height and width information
 * Created by Kalman on 17/02/2017.
 */
public class Size {

    public static final String TAG = "Size";

    public int width = 0;

    public int height = 0;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size(String size) {
        String[] data = size.split(",");
        this.width = Integer.parseInt(data[0]);
        this.height = Integer.parseInt(data[1]);
    }

    public Size(Camera.Size size) {
        width = size.width;
        height = size.height;
    }

    public Size(android.util.Size size) {
        width = size.getWidth();
        height = size.getHeight();
    }

    @Override
    public String toString() {
        return "Size{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
