package com.telenav.osv.item;

/**
 * Created by Kalman on 11/18/15.
 */
public class VideoFile extends RecordingFile {

    private static final String TAG = "VideoFile";

    public int fileIndex;

    public String link;

    private int sequenceId;

    private int frameCount;

    private OSVFile file;

    public VideoFile(int sequenceId, String link, int fileIndex) {
        this.sequenceId = sequenceId;
        this.fileIndex = fileIndex;
        this.link = link;
    }

    public VideoFile(OSVFile video, int fileIndex, int count) {
        this.sequenceId = LocalSequence.getSequenceId(video.getParentFile());
        this.file = video;
        this.fileIndex = fileIndex;
        this.frameCount = count;
        link = "file://" + video.getPath();
    }
}
