package com.telenav.osv.item;

/**
 * the server score history format is different form the app history format FYI, one is coverage and one is multiplier
 * Created by kalmanb on 7/18/17.
 */
public class ScoreItem {

    public int value;

    public int photoCount;

    public boolean obd;

    public int detectedSigns;

    public ScoreItem(int value, int photoCount, boolean obd) {
        this.value = value;
        this.photoCount = photoCount;
        this.obd = obd;
        this.detectedSigns = 0;
    }
}
