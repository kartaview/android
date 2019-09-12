package com.telenav.osv.item;

import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 11/18/15.
 */
public class ImageFile extends RecordingFile {

    private static final String TAG = "ImageFile";

    private static final int INDEX_SEQUENCE_ID = 1;

    public SKCoordinate coords;

    public int index;

    public String link;

    public boolean isChecked;

    public OSVFile file;

    public String thumb = "";

    private int sequenceId;

    private int id;

    private boolean isPano = false;

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

    public ImageFile(OSVFile photo, int index, SKCoordinate skCoordinate, boolean panorama) {
        this.sequenceId = getSequenceId(photo.getParentFile().getName());
        this.file = photo;
        this.index = index;
        link = "file:///" + photo.getPath();
        thumb = link;
        coords = skCoordinate;
        this.isPano = panorama;
    }

    /**
     * @param name the folder name from which the sequence id will be extracted.
     * @return {@code int} representing the sequence id extraced from the folder of the image.
     */
    private int getSequenceId(String name) {
        int result = -1;
        try {
            result = Integer.valueOf(name.split("_")[INDEX_SEQUENCE_ID]);
        } catch (Exception e) {
            Log.w(TAG, "getSequenceId: " + e.getLocalizedMessage());
        }
        return result;
    }
}
