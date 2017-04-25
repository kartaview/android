/*
 * Copyright (c) 2011 SKOBBLER SRL.
 * Cuza Voda 1, Cluj-Napoca, Cluj, 400107, Romania
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SKOBBLER SRL 
 * ("Confidential Information"). You shall not disclose such Confidential 
 * Information and shall use it only in accordance with the terms of the license 
 * agreement you entered into with SKOBBLER SRL.
 */
package com.skobbler.ngx.positioner;


import android.location.Location;
import com.skobbler.ngx.SKCoordinate;


/**
 * A data class representing a geographic position. A position can consist of a
 * latitude, longitude, and other information such as heading, accuracy and
 * speed.
 * @version // [apiVersion]
 */
public class SKPosition {
    
    /**
     * The GPS coordinate
     */
    private SKCoordinate coordinate;
    
    /**
     * The heading
     */
    private double heading;
    
    /**
     * The horizontal acurracy
     */
    private double horizontalAccuracy;
    
    /**
     * Vertical accuracy
     */
    private double verticalAccuracy;
    
    /**
     * The speed
     */
    private double speed;
    
    /**
     * Altitude
     */
    private double altitude;
    
    /**
     * Creates a point using given latitude and longitude.
     * @param latitude
     * @param longitutde
     */
    public SKPosition(double latitude, double longitude) {
        super();
        coordinate = new SKCoordinate(latitude, longitude);
        
    }
    
    /**
     * Creates a point using given GPS coordinates.
     * @param coordinate
     */
    public SKPosition(SKCoordinate coordinate) {
        this.coordinate = SKCoordinate.copyOf(coordinate);
    }
    
    /**
     * Creates a point using given longitude, latitude and altitude.
     * @param latitude
     * @param longitude
     * @param altitude
     */
    public SKPosition(double latitude, double longitude, double altitude) {
        coordinate = new SKCoordinate(latitude, longitude);
        this.altitude = altitude;
    }
    
    /**
     * Construct from an Android Location object
     * @param location
     */
    public SKPosition(Location location) {
        this(location.getLatitude(), location.getLongitude(), location.getBearing(), location.getAccuracy(), location
                .getSpeed(), 0, location.getAltitude());
    }
    
    /**
     * Constructor using all fields.
     * @param latitude
     * @param longitude
     * @param heading
     * @param horizontalAccuracy
     * @param speed
     * @param verticalAccuracy
     * @param altitude
     */
    public SKPosition(double latitude, double longitude, double heading, double horizontalAccuracy, double speed,
            double verticalAccuracy, double altitude) {
        super();
        coordinate = new SKCoordinate(latitude, longitude);
        this.heading = heading;
        this.horizontalAccuracy = horizontalAccuracy;
        this.speed = speed;
        this.verticalAccuracy = verticalAccuracy;
        this.altitude = altitude;
    }
    
    
    /**
     * Constructor
     * @param latitude
     * @param longitude
     * @param heading
     * @param horizontalAccuracy
     * @param speed
     */
    public SKPosition(double latitude, double longitude, double heading, double horizontalAccuracy, double speed) {
        super();
        coordinate = new SKCoordinate(latitude, longitude);
        this.heading = heading;
        this.horizontalAccuracy = horizontalAccuracy;
        this.speed = speed;
        
    }
    
    /**
     * Constructor
     */
    public SKPosition() {}
    
    /**
     * Returns the coordinates for the point, in degrees.
     * @return the coordinate for the position
     */
    public SKCoordinate getCoordinate() {
        return coordinate;
    }
    
    /**
     * Sets a new coordinate for the point, in degrees.
     * @param coordinate new latitude to be set for the position
     */
    public void setCoordinate(SKCoordinate coordinate) {
        if (this.coordinate == null) {
            this.coordinate = SKCoordinate.copyOf(coordinate);
        } else {
            this.coordinate.setLatitude(coordinate.getLatitude());
            this.coordinate.setLongitude(coordinate.getLongitude());
        }
    }
    
    /**
     * Returns the heading, in degrees.
     * @return the position heading
     */
    public double getHeading() {
        return heading;
    }
    
    /**
     * Sets the heading for the position, in degrees.
     * @param heading new heading to be set
     */
    public void setHeading(double heading) {
        this.heading = heading;
    }
    
    /**
     * Returns the horizontal accuracy reported by the GPS device, in meters.
     * @return the position horizontal accuracy
     */
    public double getHorizontalAccuracy() {
        return horizontalAccuracy;
    }
    
    /**
     * Sets the horizontal accuracy reported by the GPS device, in meters.
     * @param horizontalAccuracy new horizontal accuracy to be set
     */
    public void setHorizontalAccuracy(double horizontalAccuracy) {
        this.horizontalAccuracy = horizontalAccuracy;
    }
    
    /**
     * Returns the vertical accuracy reported by the GPS device, in meters.
     * @return the position vertical accuracy
     */
    public double getVerticalAccuracy() {
        return verticalAccuracy;
    }
    
    /**
     * Sets the vertical accuracy reported by the GPS device, in meters.
     * @param verticalAccuracy
     */
    public void setVerticalAccuracy(double verticalAccuracy) {
        this.verticalAccuracy = verticalAccuracy;
    }
    
    
    public double getAltitude() {
        return altitude;
    }
    
    
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
    
    /**
     * Returns the speed reported by the GPS device, in meters/second over
     * ground.
     * @return the speed reported for the position
     */
    public double getSpeed() {
        return speed;
    }
    
    /**
     * Sets the speed reported by the GPS device, in meters/second over ground.
     * @param speed new speed to be set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SKPosition [latitude=").append(coordinate.getLatitude()).append(", longitude=")
                .append(coordinate.getLongitude()).append(", heading=").append(heading).append(", horizontalAccuracy=")
                .append(horizontalAccuracy).append(", verticalAccuracy=").append(verticalAccuracy).append(", speed=")
                .append(speed).append(", altitude=").append(altitude).append("]");
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof SKPosition) {
            SKPosition pos = (SKPosition) o;
            return (pos.getCoordinate().getLatitude() == this.getCoordinate().getLatitude() && pos.getCoordinate()
                    .getLongitude() == this.getCoordinate().getLongitude());
        }
        return false;
    }
    
    public int hashCode() {
        return (int) ((Math.round(coordinate.getLatitude() * 10000000)
                + Math.round(coordinate.getLongitude() * 10000000) + Math.round(heading * 10000000)
                + Math.round(horizontalAccuracy * 10000000) + Math.round(verticalAccuracy * 10000000)));
    }
    
    
}
