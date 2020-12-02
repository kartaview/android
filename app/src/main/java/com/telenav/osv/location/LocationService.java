package com.telenav.osv.location;

import android.location.Location;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.location.filter.LocationFilterType;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;

/**
 * Interface that holds the available functionality of the Location component.
 * Created by cameliao on 2/6/18.
 */

public interface LocationService {

    /**
     * @return a {@code Flowable} in order to subscribe for location updates.
     * The location can be filtered by providing a tested default filter from {@link LocationFilterType} to the {@code .filter()} operator
     * using the {@link FilterFactory}.
     */
    Flowable<Location> getLocationUpdates();

    /**
     * @return the last known {@code Location}.
     * If the location request failed the method will return null.
     */
    Maybe<Location> getLastKnownLocation();
}