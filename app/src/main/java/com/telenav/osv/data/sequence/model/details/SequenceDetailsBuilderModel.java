package com.telenav.osv.data.sequence.model.details;

import javax.annotation.Nullable;
import android.location.Location;

/**
 * The generic details for a sequence.
 * <p>
 * Remote related:
 * <ul>
 * <li>{@link #remoteId}</li>
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
 * </ul>
 * @author horatiuf
 */
public class SequenceDetailsBuilderModel {

    /**
     * Flag which show if the current sequence has been used with OBD feature.
     */
    private boolean obd;

    /**
     * The first location of the first frame.
     */
    private Location initialLocation;

    /**
     * ToDo: remove once view models are implemented
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
    private long remoteId;

    /**
     * The timestamp representing creation time for the sequence.
     */
    private long creationTime;

    /**
     * Default constructor for the current class.
     */
    private SequenceDetailsBuilderModel() {
        // empty constructor to prevent instantiation without using builder.
    }

    public boolean isObd() {
        return obd;
    }

    public Location getInitialLocation() {
        return initialLocation;
    }

    @Nullable
    public String getAddressName() {
        return addressName;
    }

    public double getDistance() {
        return distance;
    }

    public String getAppVersion() {
        return appVersion;
    }

    @Nullable
    public String getProcessingRemoteStatus() {
        return processingRemoteStatus;
    }

    public long getRemoteId() {
        return remoteId;
    }

    /**
     * Builder pattern class.
     */
    public static class SequenceDetailsBuilder {

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
        private String processingRemoteStatus;

        /**
         * The identifier on the sequence remote. This is required for all types of sequences.
         */
        private long remoteId;

        /**
         * The timestamp representing creation time for the sequence.
         */
        private long creationTime;

        /**
         * Default constructor for the current builder.
         */
        public SequenceDetailsBuilder(Location initialLocation, double distance, String appVersion, long remoteId, long creationTime) {
            this.initialLocation = initialLocation;
            this.distance = distance;
            this.appVersion = appVersion;
            this.remoteId = remoteId;
            this.creationTime = creationTime;
        }

        public SequenceDetailsBuilder(SequenceDetailsBuilderModel sequenceDetails) {
            this.appVersion = sequenceDetails.appVersion;
            this.initialLocation = sequenceDetails.initialLocation;
            this.creationTime = sequenceDetails.creationTime;
            this.processingRemoteStatus = sequenceDetails.processingRemoteStatus;
            this.distance = sequenceDetails.distance;
            this.addressName = sequenceDetails.addressName;
            this.obd = sequenceDetails.obd;
            this.remoteId = sequenceDetails.remoteId;
        }

        public SequenceDetailsBuilder setObd(boolean obd) {
            this.obd = obd;
            return this;
        }

        public SequenceDetailsBuilder setAddressName(String addressName) {
            this.addressName = addressName;
            return this;
        }

        public SequenceDetailsBuilder setProcessingRemoteStatus(String processingRemoteStatus) {
            this.processingRemoteStatus = processingRemoteStatus;
            return this;
        }

        public SequenceDetailsBuilderModel build() {
            SequenceDetailsBuilderModel sequenceDetails = new SequenceDetailsBuilderModel();
            sequenceDetails.appVersion = this.appVersion;
            sequenceDetails.initialLocation = this.initialLocation;
            sequenceDetails.creationTime = this.creationTime;
            sequenceDetails.processingRemoteStatus = this.processingRemoteStatus;
            sequenceDetails.distance = this.distance;
            sequenceDetails.addressName = this.addressName;
            sequenceDetails.obd = this.obd;
            sequenceDetails.remoteId = this.remoteId;
            return sequenceDetails;
        }
    }
}
