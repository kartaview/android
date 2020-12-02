package com.telenav.osv.network.model.tagging;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResponseModelTaggingData {

    @SerializedName("id")
    @Expose
    public String id;

    @SerializedName("dataType")
    @Expose
    public String dataType;

    @SerializedName("dateAdded")
    @Expose
    public String dateAdded;

    @SerializedName("filemd5")
    @Expose
    public String filemd5;

    @SerializedName("filename")
    @Expose
    public String filename;

    @SerializedName("filepath")
    @Expose
    public String filepath;

    @SerializedName("filesize")
    @Expose
    public String filesize;

    @SerializedName("processingError")
    @Expose
    public String processingError;

    @SerializedName("processingResult")
    @Expose
    public Object processingResult;

    @SerializedName("processingStatus")
    @Expose
    public String processingStatus;

    @SerializedName("sequence")
    @Expose
    public ResponseModelTaggingSequence responseModelTaggingSequence;

    @SerializedName("sequenceIndex")
    @Expose
    public String sequenceIndex;

    @SerializedName("status")
    @Expose
    public String status;

    @SerializedName("storage")
    @Expose
    public String storage;

    /**
     * No args constructor for use in serialization
     */
    public ResponseModelTaggingData() {
    }

    /**
     * @param filepath
     * @param sequenceIndex
     * @param status
     * @param filemd5
     * @param dateAdded
     * @param id
     * @param processingError
     * @param dataType
     * @param filesize
     * @param processingStatus
     * @param responseModelTaggingSequence
     * @param processingResult
     * @param filename
     * @param storage
     */
    public ResponseModelTaggingData(String id, String dataType, String dateAdded, String filemd5, String filename, String filepath, String filesize, String processingError,
                                    Object processingResult, String processingStatus, ResponseModelTaggingSequence responseModelTaggingSequence, String sequenceIndex,
                                    String status, String storage) {
        super();
        this.id = id;
        this.dataType = dataType;
        this.dateAdded = dateAdded;
        this.filemd5 = filemd5;
        this.filename = filename;
        this.filepath = filepath;
        this.filesize = filesize;
        this.processingError = processingError;
        this.processingResult = processingResult;
        this.processingStatus = processingStatus;
        this.responseModelTaggingSequence = responseModelTaggingSequence;
        this.sequenceIndex = sequenceIndex;
        this.status = status;
        this.storage = storage;
    }

}
