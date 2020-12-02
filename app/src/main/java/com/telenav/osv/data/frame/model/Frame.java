package com.telenav.osv.data.frame.model;

import android.location.Location;

import androidx.annotation.Nullable;

import com.telenav.osv.data.KVBaseModel;

import org.joda.time.DateTime;

/**
 * Model object for the frame local persistence.
 * <p>
 * This represents the frame which is captured during record and abstract the idea of photo in the app.
 *
 * @author horatiuf
 */

public class Frame extends KVBaseModel {

    /**
     * Represents the path for the file in the disk representing the frame.
     */
    private String filePath;

    /**
     * The creation date and time of the frame.
     */
    private DateTime dateTime;

    /**
     * The index representing the position of the frame in the sequence.
     */
    private int index;

    /**
     * The location of the frame.
     */
    @Nullable
    private Location location;

    /**
     * Default constructor for the current class.
     */
    public Frame(String ID,
                 String filePath,
                 DateTime dateTime,
                 int index) {
        super(ID);
        this.dateTime = dateTime;
        this.index = index;
        this.filePath = filePath;
    }

    /**
     * Special constructor for the current class which includes also the location field rather than the default fields.
     */
    public Frame(String ID,
                 String filePath,
                 DateTime dateTime,
                 int index,
                 Location location) {
        super(ID);
        this.dateTime = dateTime;
        this.index = index;
        this.filePath = filePath;
        this.location = location;
    }

    /**
     * @return the {@code String} representing {@link #filePath}.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return {@code int} representing {@link #index}.
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return {@code DateTime} representing {@link #dateTime}.
     */
    public DateTime getDateTime() {
        return dateTime;
    }

    /**
     * @return {@code Location} representing {@link #location}.
     */
    @Nullable
    public Location getLocation() {
        return location;
    }
}
