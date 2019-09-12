package com.telenav.osv.recorder.sensor;

import io.reactivex.Completable;

/**
 * Interface that holds all the {@code DataCollector} functionality
 * Created by cameliao on 2/7/18.
 */

public interface Sensor {

    /**
     * Starts collecting the data from the sensors
     */
    Completable startCollecting();

    /**
     * Stops collecting the data from the sensors
     */
    Completable stopCollecting();
}
