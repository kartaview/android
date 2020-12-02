package com.telenav.osv.recorder.tagging.converter.model;

import com.google.gson.JsonArray;

public class TaggingCoordinate extends TaggingBase<JsonArray> {

    private double lat;

    private double lon;

    public TaggingCoordinate(double lat, double lon) {
        super(new JsonArray());
        this.lat = lat;
        this.lon = lon;
    }


    @Override
    protected void build() {
        geoJsonObject.add(lon);
        geoJsonObject.add(lat);
    }
}
