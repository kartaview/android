package com.telenav.osv.item;

import com.telenav.osv.common.model.KVLatLng;
import com.telenav.osv.utils.Log;

/**
 * Created by Kalman on 11/18/15.
 */
public class ImageFile extends RecordingFile {

    private static final String TAG = "ImageFile";

    private static final int INDEX_SEQUENCE_ID = 1;

    public KVLatLng coords;

    public int index;

    public String link;

    public boolean isChecked;

    public KVFile file;

    public String thumb = "";

    private int sequenceId;

    private int id;

    private boolean isPano = false;

    public ImageFile(int sequenceId, String link, String thumbLink, int id, int index, KVLatLng location, boolean pano) {
        this.sequenceId = sequenceId;
        this.id = id;
        this.index = index;
        this.link = link;
        this.coords = location;
        this.thumb = thumbLink;
        this.isPano = pano;
        if (thumb.equals("")) {
            thumb = link;
        }
    }

    public ImageFile(KVFile photo, int index, KVLatLng location, boolean panorama) {
        this.sequenceId = getSequenceId(photo.getParentFile().getName());
        this.file = photo;
        this.index = index;
        link = "file:///" + photo.getPath();
        thumb = link;
        coords = location;
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
