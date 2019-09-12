package com.telenav.osv.recorder.tagging.converter.model;

import com.google.gson.JsonObject;

public abstract class TaggingGeometry extends TaggingBase<JsonObject> {

    /**
     * Default constructor for the current class.
     */
    public TaggingGeometry() {
        super(new JsonObject());
    }
}
