package com.telenav.osv.location.filter;

/**
 * Interface containing all the location filter types.
 */
public @interface LocationFilterType {

    /**
     * Filters the location with the latitude or longitude equals with zero.
     */
    int FILTER_ZERO_VALUES = 0;
}