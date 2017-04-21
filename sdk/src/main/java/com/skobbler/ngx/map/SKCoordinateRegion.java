/*
 * Copyright (c) 2011 SKOBBLER SRL. Cuza Voda 1, Cluj-Napoca, Cluj, 400107,
 * Romania All rights reserved. This software is the confidential and
 * proprietary information of SKOBBLER SRL ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into with
 * SKOBBLER SRL. Created on May 21, 2014 by Dana Modified on $Date$ by $Author$
 */
package com.skobbler.ngx.map;


import com.skobbler.ngx.SKCoordinate;


/**
 * Class that stores information about a map region ( map center and zoom
 * level).
 * @version // [apiVersion]
 */
public class SKCoordinateRegion {
    
    /**
     * Map center
     */
    private SKCoordinate center = new SKCoordinate();
    
    /**
     * Zoom level
     */
    private float zoomLevel;
    
    /**
     * Constructor
     */
    public SKCoordinateRegion() {
        
    }
    
    /**
     * Creates a region based on given center and zoom level.
     * @param center
     * @param zoomLevel
     */
    public SKCoordinateRegion(SKCoordinate center, float zoomLevel) {
        super();
        this.center = center;
        this.zoomLevel = zoomLevel;
    }
    
    /**
     * Returns the center for the region.
     * @return a {@link SKCoordinateRegion} that is the center point for the
     * region.
     */
    public SKCoordinate getCenter() {
        return center;
    }
    
    /**
     * Sets the center for the region.
     * @param center a {@link SKCoordinateRegion} that will represent the center
     * point for the region.
     */
    public void setCenter(SKCoordinate center) {
        this.center = center;
    }
    
    /**
     * Returns the zoom level for the region.
     * @return the zoom level for the region.
     */
    public float getZoomLevel() {
        return zoomLevel;
    }
    
    /**
     * Sets the zoom level for the region.
     * @param zoomLevel new zoom level for the region.
     */
    public void setZoomLevel(float zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SKCoordinateRegion [center=");
        builder.append(center);
        builder.append(", zoomLevel=");
        builder.append(zoomLevel);
        builder.append("]");
        return builder.toString();
    }
    
}
