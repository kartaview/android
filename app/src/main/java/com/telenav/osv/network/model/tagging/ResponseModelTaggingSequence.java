package com.telenav.osv.network.model.tagging;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelTaggingSequence {

    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("userId")
    @Expose
    public String userId;

    @SerializedName("dateAdded")
    @Expose
    public String dateAdded;

    @SerializedName("dateProcessed")
    @Expose
    public String dateProcessed;

    @SerializedName("imageProcessingStatus")
    @Expose
    public String imageProcessingStatus;

    @SerializedName("isVideo")
    @Expose
    public String isVideo;

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

    @SerializedName("address")
    @Expose
    public String address;

    @SerializedName("sequenceType")
    @Expose
    public Object sequenceType;

    @SerializedName("countActivePhotos")
    @Expose
    public String countActivePhotos;

    @SerializedName("distance")
    @Expose
    public String distance;

    @SerializedName("metaDataFilename")
    @Expose
    public String metaDataFilename;

    @SerializedName("clientTotal")
    @Expose
    public String clientTotal;

    @SerializedName("obdInfo")
    @Expose
    public String obdInfo;

    @SerializedName("deviceName")
    @Expose
    public String deviceName;

    @SerializedName("platformName")
    @Expose
    public String platformName;

    @SerializedName("platformVersion")
    @Expose
    public String platformVersion;

    @SerializedName("appVersion")
    @Expose
    public String appVersion;

    @SerializedName("matched")
    @Expose
    public Object matched;

    @SerializedName("uploadSource")
    @Expose
    public String uploadSource;

    @SerializedName("storage")
    @Expose
    public String storage;

    @SerializedName("countMetadataPhotos")
    @Expose
    public String countMetadataPhotos;

    @SerializedName("uploadStatus")
    @Expose
    public String uploadStatus;

    @SerializedName("processingStatus")
    @Expose
    public String processingStatus;

    @SerializedName("metadataStatus")
    @Expose
    public String metadataStatus;

    @SerializedName("hasRawData")
    @Expose
    public String hasRawData;

    @SerializedName("countMetadataVideos")
    @Expose
    public String countMetadataVideos;

    @SerializedName("qualityStatus")
    @Expose
    public String qualityStatus;

    @SerializedName("quality")
    @Expose
    public Object quality;

    @SerializedName("cameraParameters")
    @Expose
    public Object cameraParameters;

    @SerializedName("blurVersion")
    @Expose
    public String blurVersion;

    @SerializedName("blurBuild")
    @Expose
    public String blurBuild;

    @SerializedName("status")
    @Expose
    public String status;

    /**
     * No args constructor for use in serialization
     */
    public ResponseModelTaggingSequence() {
    }

    /**
     * @param blurBuild
     * @param matched
     * @param currentLng
     * @param uploadSource
     * @param blurVersion
     * @param sequenceType
     * @param dateAdded
     * @param isVideo
     * @param dateProcessed
     * @param countMetadataPhotos
     * @param id
     * @param platformVersion
     * @param distance
     * @param metaDataFilename
     * @param deviceName
     * @param platformName
     * @param processingStatus
     * @param userId
     * @param countMetadataVideos
     * @param metadataStatus
     * @param imageProcessingStatus
     * @param countActivePhotos
     * @param status
     * @param uploadStatus
     * @param countryCode
     * @param stateCode
     * @param obdInfo
     * @param cameraParameters
     * @param qualityStatus
     * @param currentLat
     * @param address
     * @param appVersion
     * @param hasRawData
     * @param quality
     * @param clientTotal
     * @param storage
     */
    public ResponseModelTaggingSequence(String id, String userId, String dateAdded, String dateProcessed, String imageProcessingStatus, String isVideo, String currentLat,
                                        String currentLng, String countryCode, Object stateCode, String address, Object sequenceType, String countActivePhotos, String distance,
                                        String metaDataFilename, String clientTotal, String obdInfo, String deviceName, String platformName, String platformVersion,
                                        String appVersion, Object matched, String uploadSource, String storage, String countMetadataPhotos, String uploadStatus,
                                        String processingStatus, String metadataStatus, String hasRawData, String countMetadataVideos, String qualityStatus, Object quality,
                                        Object cameraParameters, String blurVersion, String blurBuild, String status) {
        super();
        this.id = id;
        this.userId = userId;
        this.dateAdded = dateAdded;
        this.dateProcessed = dateProcessed;
        this.imageProcessingStatus = imageProcessingStatus;
        this.isVideo = isVideo;
        this.currentLat = currentLat;
        this.currentLng = currentLng;
        this.countryCode = countryCode;
        this.stateCode = stateCode;
        this.address = address;
        this.sequenceType = sequenceType;
        this.countActivePhotos = countActivePhotos;
        this.distance = distance;
        this.metaDataFilename = metaDataFilename;
        this.clientTotal = clientTotal;
        this.obdInfo = obdInfo;
        this.deviceName = deviceName;
        this.platformName = platformName;
        this.platformVersion = platformVersion;
        this.appVersion = appVersion;
        this.matched = matched;
        this.uploadSource = uploadSource;
        this.storage = storage;
        this.countMetadataPhotos = countMetadataPhotos;
        this.uploadStatus = uploadStatus;
        this.processingStatus = processingStatus;
        this.metadataStatus = metadataStatus;
        this.hasRawData = hasRawData;
        this.countMetadataVideos = countMetadataVideos;
        this.qualityStatus = qualityStatus;
        this.quality = quality;
        this.cameraParameters = cameraParameters;
        this.blurVersion = blurVersion;
        this.blurBuild = blurBuild;
        this.status = status;
    }

}
