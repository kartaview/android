package com.telenav.osv.data.sequence.model.details;

import javax.annotation.Nullable;
import org.joda.time.DateTime;
import android.location.Location;

/**
 * The generic details for a sequence.
 * <p>
 * Remote related:
 * <ul>
 * <li>{@link #onlineId}</li>
 * <li>{@link #processingRemoteStatus}</li>
 * </ul>
 * Identify related:
 * <ul>
 * <li>{@link #initialLocation}</li>
 * <li>{@link #addressName}</li>
 * </ul>
 * Miscellaneous:
 * <ul>
 * <li>{@link #addressName}</li>
 * <li>{@link #appVersion}</li>
 * <li>{@link #distance}</li>
 * <li>{@link #dateTime}</li>
 * </ul>
 * @author horatiuf
 */
public class SequenceDetails {

    /**
     * The value which represents that the online id was never set.
     */
    public static final int ONLINE_ID_NOT_SET = -1;

    /**
     * Flag which show if the current sequence has been used with OBD feature.
     */
    private boolean obd;

    /**
     * The first location of the first frame.
     */
    private Location initialLocation;

    /**
     * The address name of the initial location. Can be null for local sequences where initial location have not been processed.
     */
    @Nullable
    private String addressName;

    /**
     * The total distance of the sequence for which frames were taken.
     */
    private double distance;

    /**
     * The version of the app on which the sequence was created.
     */
    private String appVersion;

    /**
     * The status of the sequence on the remote.
     */
    @Nullable
    private String processingRemoteStatus;

    /**
     * The identifier on the sequence remote. This is required for all types of sequences.
     */
    private long onlineId = ONLINE_ID_NOT_SET;

    /**
     * The {@code DateTime} representing the creation time for the sequence.
     */
    private DateTime dateTime;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetails(Location initialLocation, double distance, String appVersion, DateTime dateTime) {
        this.initialLocation = initialLocation;
        this.distance = distance;
        this.appVersion = appVersion;
        this.dateTime = dateTime;
    }

    public boolean isObd() {
        return obd;
    }

    public void setObd(boolean obd) {
        this.obd = obd;
    }

    public Location getInitialLocation() {
        return initialLocation;
    }

    @Nullable
    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(@Nullable String addressName) {
        this.addressName = addressName;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public long getOnlineId() {
        return onlineId;
    }

    public void setOnlineId(long onlineId) {
        this.onlineId = onlineId;
    }

    @Nullable
    public String getProcessingRemoteStatus() {
        return processingRemoteStatus;
    }

    public void setProcessingRemoteStatus(@Nullable String processingRemoteStatus) {
        this.processingRemoteStatus = processingRemoteStatus;
    }

    public DateTime getDateTime() {
        return dateTime;
    }
}
