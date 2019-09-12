package com.telenav.osv.recorder.logger;

import com.telenav.osv.item.metadata.DataCollectorItemWrapper;
import com.telenav.osv.item.metadata.Obd2Data;
import com.telenav.osv.item.metadata.VideoData;

/**
 * Implementation of the {@link MetadataLogger} interface that holds all the functionality for logging the data in metadata file.
 * Created by cameliao on 2/7/18.
 */

public class MetadataLoggerManager implements MetadataLogger {

    /**
     * An instance to the {@link MetadataLogger} implementation
     */
    private static MetadataLogger instance;

    /**
     * Private constructor for the current class to hide the initialisation from external sources.
     */
    private MetadataLoggerManager() {

    }

    /**
     * @return a single instance of the {@link MetadataLogger} representing {@link #instance}.
     * If the {@link #instance} is not set, a new instance of the {@link MetadataLogger} will be created.
     */
    public static MetadataLogger getInstance() {
        if (instance == null) {
            instance = new MetadataLoggerManager();
        }
        return instance;
    }


    @Override
    public void logSensorData(DataCollectorItemWrapper sensorData) {

    }

    @Override
    public void logOBD2Data(Obd2Data obd2Data) {

    }

    @Override
    public void logVideoData(VideoData videoData) {

    }

    @Override
    public boolean isGpsDataAvailableInMetadata() {
        return false;
    }

    @Override
    public void finishLogging() {

    }
}
