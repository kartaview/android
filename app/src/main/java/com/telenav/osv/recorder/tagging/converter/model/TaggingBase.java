package com.telenav.osv.recorder.tagging.converter.model;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import com.google.gson.JsonElement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base class for tagging models which will contain {@link #toGeoJson()}.
 */
public abstract class TaggingBase<T extends JsonElement> {

    public static final String PROPERTY_VALUE_TYPE_LINE_STRING = "LineString";

    public static final String PROPERTY_VALUE_TYPE_POINT = "Point";

    public static final String PROPERTY_VALUE_TYPE_FEATURE = "Feature";

    public static final String PROPERTY_VALUE_TYPE_FEATURE_COLLECTION = "FeatureCollection";

    public static final String PROPERTY_VALUE_KV_TAGGING_NOTE = "NOTE";

    public static final String PROPERTY_VALUE_KV_TAGGING_NARROW_ROAD = "NARROW ROAD";

    public static final String PROPERTY_VALUE_KV_TAGGING_CLOSED_ROAD = "CLOSED ROAD";

    public static final String PROPERTY_VALUE_ONE_WAY_YES = "YES";

    public static final String PROPERTY_VALUE_KV_TAGGING_ONE_WAY = "ONE WAY";

    public static final String PROPERTY_VALUE_KV_TAGGING_TWO_WAY = "TWO WAY";

    public static final String PROPERTY_VALUE_ONE_WAY_NO = "NO";

    public static final String PROPERTY_NAME_TYPE = "type";

    public static final String PROPERTY_NAME_COORDINATES = "coordinates";

    public static final String PROPERTY_NAME_GEOMETRY = "geometry";

    public static final String PROPERTY_NAME = "name";

    public static final String PROPERTY_NAME_KV_TAGGING = "osc_tagging";

    public static final String PROPERTY_NAME_ONEWAY = "oneway";

    public static final String PROPERTY_NAME_DESCRIPTION = "description";

    public static final String PROPERTY_NAME_TIME = "time";

    public static final String PROPERTY_NAME_FEATURES = "features";

    public static final String PROPERTY_NAME_PROPERTIES = "properties";

    /**
     * Reference to the JsonObject which will be serialized.
     */
    protected T geoJsonObject;

    /**
     * Default constructor for the current class.
     */
    public TaggingBase(T geoJsonObject) {
        this.geoJsonObject = geoJsonObject;
    }

    @NonNull
    @Override
    public String toString() {
        return geoJsonObject.toString();
    }

    public T getGeoJsonObject() {
        return geoJsonObject;
    }

    /**
     * @return {@code String} representing the serialised data in Json format.
     */
    public String toGeoJson() {
        build();
        return geoJsonObject.toString();
    }

    /**
     * Build the {@link #geoJsonObject} with the values of each concrete implementation.
     */
    protected abstract void build();

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(value = {PROPERTY_VALUE_KV_TAGGING_CLOSED_ROAD, PROPERTY_VALUE_KV_TAGGING_NARROW_ROAD, PROPERTY_VALUE_KV_TAGGING_NOTE})
    public @interface EventType {

    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(value = {PROPERTY_VALUE_KV_TAGGING_ONE_WAY, PROPERTY_VALUE_KV_TAGGING_TWO_WAY})
    public @interface RoadType {

    }
}
