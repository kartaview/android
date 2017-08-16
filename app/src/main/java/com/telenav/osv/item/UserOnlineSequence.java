package com.telenav.osv.item;

import java.util.Date;

/**
 * Created by kalmanb on 7/11/17.
 */
public class UserOnlineSequence extends OnlineSequence {
    private static final String TAG = "UserOnlineSequence";

    public UserOnlineSequence(int sequenceId, Date date, int originalImageCount, String address, String thumbLink, boolean obd, String platform, String platformVersion, String
            appVersion, int distance, int score) {
        super(sequenceId, date, originalImageCount, address, thumbLink, obd, platform, platformVersion, appVersion, distance, score);
    }
}
