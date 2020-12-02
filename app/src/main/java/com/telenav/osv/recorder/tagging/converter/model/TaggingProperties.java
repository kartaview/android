package com.telenav.osv.recorder.tagging.converter.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TaggingProperties extends TaggingBase<JsonObject> {

    private DateTimeFormatter zuluTimeDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private String description;

    private long dateTime;

    @EventType
    private String eventType;

    @RoadType
    private String roadType;

    /**
     * Constructor for event types in tagging.
     */
    public TaggingProperties(long dateTime, @NonNull @EventType String eventType, @Nullable String description) {
        super(new JsonObject());
        this.description = description;
        this.dateTime = dateTime;
        this.eventType = eventType;
    }

    /**
     * Constructor for one way/two way in tagging.
     */
    public TaggingProperties(@RoadType String roadType) {
        super(new JsonObject());
        this.roadType = roadType;
    }

    @Override
    protected void build() {
        if (eventType != null) {
            geoJsonObject.addProperty(PROPERTY_NAME, eventType);
            geoJsonObject.addProperty(PROPERTY_NAME_TIME, new DateTime(dateTime).toString(zuluTimeDateTimeFormat));
            geoJsonObject.addProperty(PROPERTY_NAME_KV_TAGGING, eventType);
            if (description != null) {
                geoJsonObject.addProperty(PROPERTY_NAME_DESCRIPTION, description);
            }
        } else {
            geoJsonObject.addProperty(PROPERTY_NAME_KV_TAGGING, roadType);
            if (roadType.equals(PROPERTY_VALUE_KV_TAGGING_ONE_WAY)) {
                geoJsonObject.addProperty(PROPERTY_NAME_ONEWAY, PROPERTY_VALUE_ONE_WAY_YES);
                return;
            }
            geoJsonObject.addProperty(PROPERTY_NAME_ONEWAY, PROPERTY_VALUE_ONE_WAY_NO);
        }
    }
}
