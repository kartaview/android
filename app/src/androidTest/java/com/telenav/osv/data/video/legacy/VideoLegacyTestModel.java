package com.telenav.osv.data.video.legacy;

/**
 * @author horatiuf
 */
public class VideoLegacyTestModel {

    private static final String TAG = "VideoFile";

    public int fileIndex;

    public String link;

    private int sequenceId;

    private int frameCount;

    private String filePath;

    public VideoLegacyTestModel(int fileIndex, String link, int sequenceId, int frameCount, String filePath) {
        this.fileIndex = fileIndex;
        this.link = link;
        this.sequenceId = sequenceId;
        this.frameCount = frameCount;
        this.filePath = filePath;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public String getLink() {
        return link;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public String getFile() {
        return filePath;
    }
}
