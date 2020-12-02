package com.telenav.osv.data;

/**
 * Base class for all model data in the app. It contains a default constructor and the next fields:
 * <ul>
 * <li>{@link #ID}</li>
 * </ul>
 *
 * @author horatiuf
 */

public abstract class KVBaseModel {

    /**
     * The id of the model which extends the base class.
     */
    private String ID;

    /**
     * Default constructor for the base model class.
     *
     * @param ID {@code String} representing {@link #ID}.
     */
    public KVBaseModel(String ID) {
        this.ID = ID;
    }

    /**
     * @return {@code String} representing {@link #ID}.
     */
    public String getID() {
        return ID;
    }
}
