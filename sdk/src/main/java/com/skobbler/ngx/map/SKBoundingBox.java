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
package com.skobbler.ngx.map;


import com.skobbler.ngx.SKCoordinate;


/**
 * Stores information about a bounding box.
 * @version // [apiVersion]
 */
public class SKBoundingBox {
    
    /**
     * Name of the class. Used for logging.
     */
    private static final String TAG = "SKBoundingBox";
    
    /**
     * Top left coordinate
     */
    private SKCoordinate topLeft;
    
    /**
     * Bottom right coordinate
     */
    private SKCoordinate bottomRight;
    
    
    /**
     * Constructor
     * @param topLeft top left corner for bounding box
     * @param bottomRight bottom right corner for bounding box
     */
    public SKBoundingBox(final SKCoordinate topLeft, final SKCoordinate bottomRight) {
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }
    
    /**
     * Returns the top left coordinate for bounding box.
     * @return the topLeft
     */
    public SKCoordinate getTopLeft() {
        return topLeft;
    }
    
    
    /**
     * Sets the top left coordinate for bounding box.
     * @param topLeft the topLeft to set
     */
    public void setTopLeft(SKCoordinate topLeft) {
        this.topLeft = topLeft;
    }
    
    
    /**
     * Returns the bottom right coordinate for bounding box.
     * @return the bottomRight
     */
    public SKCoordinate getBottomRight() {
        return bottomRight;
    }
    
    
    /**
     * Sets the bottom right coordinate for bounding box.
     * @param bottomRight the bottomRight to set
     */
    public void setBottomRight(SKCoordinate bottomRight) {
        this.bottomRight = bottomRight;
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SKBoundingBox [topLeft=");
        builder.append(topLeft);
        builder.append(", bottomRight=");
        builder.append(bottomRight);
        builder.append("]");
        return builder.toString();
    }
    
    
}