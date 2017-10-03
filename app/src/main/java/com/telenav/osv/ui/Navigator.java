package com.telenav.osv.ui;

/**
 * Navigator interface
 * Created by kalmanb on 8/29/17.
 */
public interface Navigator {

    int SCREEN_MAP = 0;

    int SCREEN_RECORDING = 1;

    int SCREEN_MY_PROFILE = 2;

    int SCREEN_SETTINGS = 3;

    int SCREEN_PREVIEW = 4;

    int SCREEN_UPLOAD_PROGRESS = 5;

    int SCREEN_WAITING = 6;

    int SCREEN_RECORDING_HINTS = 8;

    int SCREEN_NEARBY = 9;

    int SCREEN_LEADERBOARD = 10;

    int SCREEN_SUMMARY = 11;

    int SCREEN_REPORT = 12;

    int SCREEN_PREVIEW_FULLSCREEN = 13;

    void openScreen(int screen, Object extra);

    void openScreen(int screen);

    void onBackPressed();

    int getCurrentScreen();
}
