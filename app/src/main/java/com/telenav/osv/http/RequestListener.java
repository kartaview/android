package com.telenav.osv.http;

/**
 * Created by Kalman on 10/8/2015.
 */
public interface RequestListener {
    int STATUS_FAILED = -1;

    int STATUS_SUCCESS_IMAGE = 0;

    int STATUS_SUCCESS_SEQUENCE = 1;

    int STATUS_SUCCESS_LIST_SEQUENCE = 2;

    int STATUS_SUCCESS_LIST_IMAGES = 3;

    int STATUS_SUCCESS_DELETE_SEQUENCE = 4;

    int STATUS_SUCCESS_DELETE_IMAGE = 5;

    int STATUS_SUCCESS_LOGIN = 6;

    int STATUS_SUCCESS_SEQUENCE_FINISHED = 7;

    int STATUS_SUCCESS_NEARBY = 8;

    int STATUS_SUCCESS_PROFILE_DETAILS = 9;

    int STATUS_SUCCESS_LEADERBOARD = 10;

    void requestFinished(int status);
}
