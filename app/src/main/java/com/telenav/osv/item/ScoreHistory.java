package com.telenav.osv.item;

/**
 * Created by Kalman on 14/12/2016.
 */
public class ScoreHistory {

    public int coverage;

    public int photoCount;

    public int obdPhotoCount;

    public int detectedSigns;

    public ScoreHistory(int coverage, int photoCount, int obdPhotoCount) {
        this.coverage = coverage;
        this.photoCount = photoCount;
        this.obdPhotoCount = obdPhotoCount;
        this.detectedSigns = 0;
    }
}
