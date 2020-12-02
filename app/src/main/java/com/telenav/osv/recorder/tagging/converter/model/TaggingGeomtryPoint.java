package com.telenav.osv.recorder.tagging.converter.model;

public class TaggingGeomtryPoint extends TaggingGeometry {

    private TaggingCoordinate coordinate;

    /**
     * Default constructor for the current class.
     */
    public TaggingGeomtryPoint(TaggingCoordinate coordinates) {
        super();
        this.coordinate = coordinates;
    }

    @Override
    protected void build() {
        coordinate.build();
        geoJsonObject.addProperty(PROPERTY_NAME_TYPE, PROPERTY_VALUE_TYPE_POINT);
        geoJsonObject.add(PROPERTY_NAME_COORDINATES, coordinate.geoJsonObject);
    }
}
