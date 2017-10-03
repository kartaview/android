package com.telenav.osv.item;

import com.skobbler.ngx.SKCoordinate;

/**
 * Created by Kalman on 11/18/15.
 */
public class ImageFile extends RecordingFile {

    private static final String TAG = "ImageFile";

    public SKCoordinate coords;

    public int index;

    public String link;

    public OSVFile file;

    public String thumb = "";

    public ImageFile(String link, String thumbLink, int index, SKCoordinate skCoordinate) {
        this.index = index;
        this.link = link;
        this.coords = skCoordinate;
        this.thumb = thumbLink;
        if ("".equals(thumb)) {
            thumb = link;
        }
    }

    public ImageFile(OSVFile photo, int index, SKCoordinate skCoordinate) {
        this.file = photo;
        this.index = index;
        link = "file:///" + photo.getPath();
        thumb = link;
        coords = skCoordinate;
    }
}
