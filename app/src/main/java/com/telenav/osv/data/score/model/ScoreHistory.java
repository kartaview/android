package com.telenav.osv.data.score.model;

import com.telenav.osv.data.KVBaseModel;

/**
 * The model class containing the score information for a specific coverage
 * Created by Kalman on 14/12/2016.
 */
public class ScoreHistory extends KVBaseModel {

    private static final int OBD_DISCONNECTED = 1;

    private static final int OBD_CONNECTED = 2;

    /**
     * The coverage for the current score. This should be between [0,10].
     * The coverage is a unique identifier for a score sequence.
     * A sequence can have multiple coverages.
     */
    private int coverage;

    /**
     * The number of photos that were taken without OBD.
     */
    private int photoCount;

    /**
     * The number of photos that were taken with OBD.
     */
    private int obdPhotoCount;

    /**
     * The OBD multiplier, used to double the points if the OBD was connected.
     * The value for this are {@link #OBD_DISCONNECTED} - if obd was disconnected  or {@link #OBD_CONNECTED} - id obd was connected.
     * If the OBD was connected once then the points will be doubled even if the OBD might have been disconnected at some point.
     */
    private int obdStatus;

    /**
     * The default constructor of the model for a local object.
     * The values for the {@link #photoCount} and {@link  #obdPhotoCount} that are set in the local data source are not taken into consideration for calculating the remote score.
     * The remote score is calculated supposing that if the OBD was once connected than all the photos were taken with OBD.
     * @param ID the {@code identifier} for the score history.
     * @param coverage the coverage number between [0,10]
     * @param photoCount the number of photos that were taken without OBD
     * @param obdPhotoCount the number of photos that were taken with OBD
     */
    public ScoreHistory(String ID, int coverage, int photoCount, int obdPhotoCount) {
        super(ID);
        this.coverage = coverage;
        this.photoCount = photoCount;
        this.obdPhotoCount = obdPhotoCount;
        if (obdPhotoCount > 0) {
            this.obdStatus = OBD_CONNECTED;
        } else {
            this.obdStatus = OBD_DISCONNECTED;
        }
    }

    /**
     * The default constructor of the model for a remote object. The id is null since there isn't any local id for a remote sequence.
     * @param coverage the coverage number between [0,10]
     * @param photoCount the number of photos that were taken without OBD
     * @param obdPhotoCount the number of photos that were taken with OBD
     * @param obdStatus the OBD multiplier to multiply the gained points
     */
    public ScoreHistory(int coverage, int photoCount, int obdPhotoCount, int obdStatus) {
        //the id of a ScoreHistory object is null
        //currently the unique key for a score is the coverage value
        super(null);
        this.coverage = coverage;
        this.photoCount = photoCount;
        this.obdPhotoCount = obdPhotoCount;
        this.obdStatus = obdStatus;
    }

    public int getCoverage() {
        return coverage;
    }

    public void setCoverage(int coverage) {
        this.coverage = coverage;
    }

    public int getPhotoCount() {
        return photoCount;
    }

    public void setPhotoCount(int photoCount) {
        this.photoCount = photoCount;
    }

    public int getObdPhotoCount() {
        return obdPhotoCount;
    }

    public void setObdPhotoCount(int obdPhotoCount) {
        this.obdPhotoCount = obdPhotoCount;
    }

    public int getObdStatus() {
        return obdStatus;
    }

    public void setObdStatus(int multiplier) {
        this.obdStatus = multiplier;
    }
}
