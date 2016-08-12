package com.telenav.osv.http;

/**
 * Created by Kalman on 10/8/2015.
 */
public interface RequestResponseListener extends RequestListener {

    void requestFinished(int status, String result);
}
