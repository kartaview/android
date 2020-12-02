package com.telenav.osv.common.filter;

import android.location.Location;
import com.telenav.osv.location.filter.LocationFilterType;
import io.reactivex.functions.Predicate;

/**
 * Factory class for filters. The class should be used to set a default filter for the stream of data.
 * <p>Location:
 * <ul>
 * <li>{@link #getLocationFilter(int)}</li>
 * </ul>
 */
public class FilterFactory {

    /**
     * Static method which creates a location filter from a given {@code filter type}.
     * @param filterType default filter types for the location stream.
     * @return a {@code Predicate} defining the location filter.
     */
    public static Predicate<Location> getLocationFilter(@LocationFilterType int filterType) {
        switch (filterType) {
            case LocationFilterType.FILTER_ZERO_VALUES:
                return location -> location.getLatitude() != 0 && location.getLongitude() != 0;
            default:
                return location -> true;
        }
    }
}
