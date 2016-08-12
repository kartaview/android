package com.telenav.osv.item;

import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.utils.Log;

/**
 *
 * Created by Kalman on 11/18/15.
 */
public class ImageFile extends RecordingFile {

    private static final String TAG = "ImageFile";

    public int sequenceId;

    public SKCoordinate coords;

    public int id;

    public int index;

    public String link;

    public boolean isChecked;

    public OSVFile file;

    public String thumb = "";

    public boolean isPano = false;

    public ImageFile(int sequenceId, String link, String thumbLink, int id, int index, SKCoordinate skCoordinate, boolean pano) {
        this.sequenceId = sequenceId;
        this.id = id;
        this.index = index;
        this.link = link;
        this.coords = skCoordinate;
        this.thumb = thumbLink;
        this.isPano = pano;
        if (thumb.equals("")) {
            thumb = link;
        }
    }

    public ImageFile(OSVFile video, int index, SKCoordinate skCoordinate, boolean panorama) {
        this.sequenceId = Sequence.getSequenceId(video.getParentFile());
        this.file = video;
        this.index = index;
        link = "file:///" + video.getPath();
        thumb = link;
        coords = skCoordinate;
        this.isPano = panorama;
    }
}
