package com.telenav.osv.recorder.tagging.converter.model;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;

public class TaggingGeometryLineString extends TaggingGeometry {

    private List<TaggingCoordinate> coordinates;

    public TaggingGeometryLineString() {
        super();
        this.coordinates = new ArrayList<>();
    }

    @Override
    protected void build() {
        geoJsonObject.addProperty(PROPERTY_NAME_TYPE, PROPERTY_VALUE_TYPE_LINE_STRING);
        JsonArray jsonArray = new JsonArray();
        for (TaggingCoordinate taggingCoordinate : coordinates) {
            taggingCoordinate.build();
            jsonArray.add(taggingCoordinate.geoJsonObject);
        }
        geoJsonObject.add(PROPERTY_NAME_COORDINATES, jsonArray);
    }

    public void addCoordinate(TaggingCoordinate taggingCoordinate) {
        coordinates.add(taggingCoordinate);
    }
}
