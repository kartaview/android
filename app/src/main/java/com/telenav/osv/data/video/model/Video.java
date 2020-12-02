package com.telenav.osv.data.video.model;

import com.telenav.osv.data.KVBaseModel;

/**
 * The model class for data related to a track video.
 *
 * @author cameliao
 */
public class Video extends KVBaseModel {

    /**
     * The index of a video, which will represent the name of the video file.
     */
    private int index;

    /**
     * The path to the current video file.
     */
    private String path;

    /**
     * The count representing the number of locations compressed in the video.
     */
    private int locationsCount;

    /**
     * Default constructor for the current model class.
     * @param locationsCount the id of the sequence that contains the current video {@link #index}.
     * @param index the index of the current video, which will represent the name of the video file.
     * @param path the path of the current video file.
     */
    public Video(String ID, int locationsCount, int index, String path) {
        super(ID);
        this.locationsCount = locationsCount;
        this.index = index;
        this.path = path;
    }

    /**
     * @return {@code int} representing {@link #index}.
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return {@code String} representing {@link #path}.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return {@code int} representing {@link #locationsCount}.
     */
    public int getLocationsCount() {
        return locationsCount;
    }

    /**
     * Sets the frame count for the current video model.
     * @param locationsCount the new frame count to be set.
     */
    public void setLocationsCount(int locationsCount) {
        this.locationsCount = locationsCount;
    }
}
