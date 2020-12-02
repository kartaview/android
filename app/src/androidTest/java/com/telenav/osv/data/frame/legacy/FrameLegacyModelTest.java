package com.telenav.osv.data.frame.legacy;

/**
 * @author horatiuf
 */
public class FrameLegacyModelTest {
    private int sequenceId;

    private int videoIndex;

    private int seqIndex;

    private String filePath;

    private double lat;

    private double lon;

    private float accuracy;

    public FrameLegacyModelTest(int sequenceId, int videoIndex, int seqIndex, String filePath, double lat, double lon, float accuracy) {
        this.sequenceId = sequenceId;
        this.videoIndex = videoIndex;
        this.seqIndex = seqIndex;
        this.filePath = filePath;
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public int getVideoIndex() {
        return videoIndex;
    }

    public int getSeqIndex() {
        return seqIndex;
    }

    public String getFilePath() {
        return filePath;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public float getAccuracy() {
        return accuracy;
    }
}
