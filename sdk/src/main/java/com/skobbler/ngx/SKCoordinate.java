/*
 * Copyright (c) 2011 SKOBBLER SRL. Cuza Voda 1, Cluj-Napoca, Cluj, 400107,
 * Romania All rights reserved. This software is the confidential and
 * proprietary information of SKOBBLER SRL ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into with
 * SKOBBLER SRL. Created on Feb 25, 2014 by Dana Modified on $Date$ by $Author$
 */
package com.skobbler.ngx;


/**
 * Stores information about a coordinate.
 * @version // [apiVersion]
 */
public class SKCoordinate {
    
    private double longitude;
    
    private double latitude;
    
    /**
     * Creates a SKCoordinate object having latitude and longitude as 0.
     */
    public SKCoordinate() {
        this.latitude = 0.0;
        this.longitude = 0.0;
    }
    
    /**
     * Creates a SKCoordinate object.
     * @param latitude
     * @param longitude
     */
    public SKCoordinate(double latitude, double longitude) {
        super();
        this.longitude = longitude;
        this.latitude = latitude;
    }
    
    
    /**
     * Returns a copy of the given coordinate.
     * @param coordinate coordinate for which a copy needs to be returned.
     * @return a copy of the given coordinate.
     */
    public static SKCoordinate copyOf(SKCoordinate coordinate) {
        if (coordinate != null) {
            final SKCoordinate copy = new SKCoordinate(coordinate.getLatitude(), coordinate.getLongitude());
            return copy;
        }
        return null;
    }
    
    /**
     * Returns the longitude of the SKCoordinate object.
     * @return the longitude of the SKCoordinate object.
     */
    public double getLongitude() {
        return longitude;
    }
    
    /**
     * Set the longitude for the SKCoordinate object.
     * @param longitude
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    /**
     * Returns the latitude for the SKCoordinate object.
     * @return the latitude for the SKCoordinate object.
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * Sets the latitude for the SKCoordinate object.
     * @param latitude new latitude
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public String toString() {
        return "[" + latitude + "," + longitude + "]";
    }
    
}
