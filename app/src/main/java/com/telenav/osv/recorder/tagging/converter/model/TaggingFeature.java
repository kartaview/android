package com.telenav.osv.recorder.tagging.converter.model;

import com.google.gson.JsonObject;

public class TaggingFeature extends TaggingBase<JsonObject> {

    private TaggingProperties taggingProperties;

    private TaggingGeometry taggingGeometry;

    public TaggingFeature(TaggingProperties taggingProperties, TaggingGeometry taggingGeometry) {
        super(new JsonObject());
        this.taggingProperties = taggingProperties;
        this.taggingGeometry = taggingGeometry;
    }

    @Override
    protected void build() {
        taggingProperties.build();
        taggingGeometry.build();
        geoJsonObject.addProperty(PROPERTY_NAME_TYPE, PROPERTY_VALUE_TYPE_FEATURE);
        geoJsonObject.add(PROPERTY_NAME_GEOMETRY, taggingGeometry.geoJsonObject);
        geoJsonObject.add(PROPERTY_NAME_PROPERTIES, taggingProperties.geoJsonObject);
    }
}
