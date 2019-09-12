package com.telenav.osv.network.model.video;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelVideo {

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

    @SerializedName("videoName")
    @Expose
    public String videoName;

    /**
     * Default constructor for the current class.
     */
    public ResponseModelVideo(String id, String sequenceId, String dateAdded, String sequenceIndex, String videoName) {
        super();
        this.id = id;
        this.sequenceId = sequenceId;
        this.dateAdded = dateAdded;
        this.sequenceIndex = sequenceIndex;
        this.videoName = videoName;
    }

}
