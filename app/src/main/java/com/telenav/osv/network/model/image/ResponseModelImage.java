package com.telenav.osv.network.model.image;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelImage {

    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("sequenceId")
    @Expose
    public String sequenceId;

    @SerializedName("dateAdded")
    @Expose
    public String dateAdded;

    @SerializedName("sequenceIndex")
    @Expose
    public String sequenceIndex;

    @SerializedName("photoName")
    @Expose
    public String photoName;

    @SerializedName("lat")
    @Expose
    public String lat;

    @SerializedName("lng")
    @Expose
    public String lng;

    @SerializedName("gpsAccuracy")
    @Expose
    public String gpsAccuracy;

    @SerializedName("headers")
    @Expose
    public Object headers;

    @SerializedName("autoImgProcessingResult")
    @Expose
    public Object autoImgProcessingResult;

    @SerializedName("status")
    @Expose
    public String status;

    @SerializedName("multipleInsert")
    @Expose
    public List<Object> multipleInsert = null;

    @SerializedName("path")
    @Expose
    public String path;

    @SerializedName("projection")
    @Expose
    public String projection;

    @SerializedName("videoIndex")
    @Expose
    public Object videoIndex;

    @SerializedName("visibility")
    @Expose
    public String visibility;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelImage(String id, String sequenceId, String dateAdded, String sequenceIndex, String photoName, String lat, String lng, String gpsAccuracy, Object headers,
                              Object autoImgProcessingResult, String status, List<Object> multipleInsert, String path, String projection, Object videoIndex, String visibility) {
        super();
        this.id = id;
        this.sequenceId = sequenceId;
        this.dateAdded = dateAdded;
        this.sequenceIndex = sequenceIndex;
        this.photoName = photoName;
        this.lat = lat;
        this.lng = lng;
        this.gpsAccuracy = gpsAccuracy;
        this.headers = headers;
        this.autoImgProcessingResult = autoImgProcessingResult;
        this.status = status;
        this.multipleInsert = multipleInsert;
        this.path = path;
        this.projection = projection;
        this.videoIndex = videoIndex;
        this.visibility = visibility;
    }

}
