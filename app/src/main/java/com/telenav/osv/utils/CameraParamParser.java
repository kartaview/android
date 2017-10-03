package com.telenav.osv.utils;

import java.util.HashMap;

/**
 * CameraParamParser
 * Created by Kalman on 19/12/2016.
 */
public class CameraParamParser {

    private static final String TAG = "CameraParamParser";

    public static HashMap<String, String> parse(String flat) {
        HashMap<String, String> result = new HashMap<>();
        String[] params = flat.split(";");
        for (String param : params) {
            String[] items = param.split("=");
            try {
                String key = items[0];
                String value = items[1];
                result.put(key, value);
            } catch (Exception ignored) {
                Log.d(TAG, Log.getStackTraceString(ignored));
            }
        }
        return result;
    }
}
