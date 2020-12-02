package com.telenav.osv.network.model.metadata;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelMetadataSequence {

    @SerializedName("id")
    @Expose
    public long id;

    @SerializedName("userId")
    @Expose
    public String userId;

    @SerializedName("dateAdded")
    @Expose
    public String dateAdded;

    @SerializedName("currentLat")
    @Expose
    public String currentLat;

    @SerializedName("currentLng")
    @Expose
    public String currentLng;

    @SerializedName("countryCode")
    @Expose
    public String countryCode;

    @SerializedName("stateCode")
    @Expose
    public Object stateCode;

    @SerializedName("status")
    @Expose
    public String status;

    @SerializedName("imagesStatus")
    @Expose
    public String imagesStatus;

    @SerializedName("metaDataFilename")
    @Expose
    public String metaDataFilename;

    @SerializedName("detectedSignsFilename")
    @Expose
    public Object detectedSignsFilename;

    @SerializedName("clientTotal")
    @Expose
    public String clientTotal;

    @SerializedName("clientTotalDetails")
    @Expose
    public Object clientTotalDetails;

    @SerializedName("obdInfo")
    @Expose
    public Object obdInfo;

    @SerializedName("platformName")
    @Expose
    public String platformName;

    @SerializedName("platformVersion")
    @Expose
    public String platformVersion;

    @SerializedName("appVersion")
    @Expose
    public String appVersion;

    @SerializedName("track")
    @Expose
    public Object track;

    @SerializedName("matchTrack")
    @Expose
    public Object matchTrack;

    @SerializedName("reviewed")
    @Expose
    public Object reviewed;

    @SerializedName("changes")
    @Expose
    public Object changes;

    @SerializedName("recognitions")
    @Expose
    public Object recognitions;

    @SerializedName("address")
    @Expose
    public String address;

    @SerializedName("sequenceType")
    @Expose
    public Object sequenceType;

    @SerializedName("uploadSource")
    @Expose
    public String uploadSource;

    @SerializedName("distance")
    @Expose
    public Object distance;

    @SerializedName("processingStatus")
    @Expose
    public String processingStatus;

    @SerializedName("countActivePhotos")
    @Expose
    public String countActivePhotos;

    /**
     * No args constructor for use in serialization
     */
    public ResponseModelMetadataSequence() {
    }

    /**
     *
     */
    public ResponseModelMetadataSequence(long id, String userId, String dateAdded, String currentLat, String currentLng, String countryCode, Object stateCode, String status,
                                         String imagesStatus, String metaDataFilename, Object detectedSignsFilename, String clientTotal, Object clientTotalDetails,
                                         Object obdInfo, String platformName, String platformVersion, String appVersion, Object track, Object matchTrack, Object reviewed,
                                         Object changes, Object recognitions, String address, Object sequenceType, String uploadSource, Object distance, String processingStatus,
                                         String countActivePhotos) {
        super();
        this.id = id;
        this.userId = userId;
        this.dateAdded = dateAdded;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
        this.countryCode = countryCode;
        this.stateCode = stateCode;
        this.status = status;
        this.imagesStatus = imagesStatus;
        this.metaDataFilename = metaDataFilename;
        this.detectedSignsFilename = detectedSignsFilename;
        this.clientTotal = clientTotal;
        this.clientTotalDetails = clientTotalDetails;
        this.obdInfo = obdInfo;
        this.platformName = platformName;
        this.platformVersion = platformVersion;
        this.appVersion = appVersion;
        this.track = track;
        this.matchTrack = matchTrack;
        this.reviewed = reviewed;
        this.changes = changes;
        this.recognitions = recognitions;
        this.address = address;
        this.sequenceType = sequenceType;
        this.uploadSource = uploadSource;
        this.distance = distance;
        this.processingStatus = processingStatus;
        this.countActivePhotos = countActivePhotos;
    }

}
