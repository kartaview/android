package com.telenav.osv.item;

import com.skobbler.ngx.SKCoordinate;

/**
 *
 * Created by Kalman on 11/18/15.
 */
public class VideoFile extends RecordingFile {

    private static final String TAG = "VideoFile";

    public int sequenceId;

    public int startIndex;

    public String link;

    public OSVFile file;

    public VideoFile(int sequenceId, String link, int startIndex) {
        this.sequenceId = sequenceId;
        this.startIndex = startIndex;
        this.link = link;
    }

    public VideoFile(OSVFile video, int startIndex) {
        this.sequenceId = Sequence.getSequenceId(video.getParentFile());
        this.file = video;
        this.startIndex = startIndex;
        link = "file://" + video.getPath();
    }
}
