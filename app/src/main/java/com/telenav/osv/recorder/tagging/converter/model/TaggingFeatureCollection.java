package com.telenav.osv.recorder.tagging.converter.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TaggingFeatureCollection extends TaggingBase<JsonObject> {

    private List<TaggingFeature> features;

    /**
     * Default constructor for the current class.
     */
    public TaggingFeatureCollection() {
        super(new JsonObject());
        features = new ArrayList<>();
    }

    @Override
    protected void build() {
        geoJsonObject.addProperty(PROPERTY_NAME_TYPE, PROPERTY_VALUE_TYPE_FEATURE_COLLECTION);
        JsonArray jsonArray = new JsonArray();
        for (TaggingFeature feature : features) {
            feature.build();
            jsonArray.add(feature.geoJsonObject);
        }
        geoJsonObject.add(PROPERTY_NAME_FEATURES, jsonArray);
    }

    public void addFeature(TaggingFeature feature) {
        features.add(feature);
    }
}
