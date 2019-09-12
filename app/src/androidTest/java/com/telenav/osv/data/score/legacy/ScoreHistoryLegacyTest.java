package com.telenav.osv.data.score.legacy;

/**
 * @author horatiuf
 */
public class ScoreHistoryLegacyTest {

    public int coverage;

    public int photoCount;

    public int obdPhotoCount;

    public int detectedSigns;

    public ScoreHistoryLegacyTest(int coverage, int photoCount, int obdPhotoCount) {
        this.coverage = coverage;
        this.photoCount = photoCount;
        this.obdPhotoCount = obdPhotoCount;
        this.detectedSigns = 0;
    }
}
