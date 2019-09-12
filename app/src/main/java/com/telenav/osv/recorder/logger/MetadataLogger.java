package com.telenav.osv.recorder.logger;

import com.telenav.osv.item.metadata.DataCollectorItemWrapper;
import com.telenav.osv.item.metadata.Obd2Data;
import com.telenav.osv.item.metadata.VideoData;

/**
 * Interface that holds all the functionality for logging the data into the metadata file.
 * Created by cameliao on 2/7/18.
 */

public interface MetadataLogger {

    /**
     * Logs the data received from {@code DataCollector}.
     * @param sensorData the data of the given sensor
     */
    void logSensorData(DataCollectorItemWrapper sensorData);

    /**
     * Logs the data received from OBD2.
     * @param obd2Data the data from obd2
     */
    void logOBD2Data(Obd2Data obd2Data);

    /**
     * Logs the data of a saved JPEG photo or encoded video
     * @param videoData the data to be logged
     */
    void logVideoData(VideoData videoData);

    /**
     * @return true if the {@code GpsData} is available in metadata file, false otherwise.
     */
    boolean isGpsDataAvailableInMetadata();

    /**
     * Stops the logging to the metadata file and closes the file.
     * This method should be called when the logging to the metadata file fro a track is done.
     */
    void finishLogging();
}
