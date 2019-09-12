package com.telenav.osv.item.metadata;

import com.telenav.osv.utils.FormatUtils;

/**
 * Just a class representing speed data obtained via an OBD2 connection.
 * <p>
 * This object's {@link #toString()} prints its contents in a format appropriate for inserting as a line in the metadata file of a sequence.
 * <p>
 * Metadata format version 1.1.6.
 * @see <a href="http://spaces.telenav.com:8080/display/TELENAVEU/Metadata+Format+Protocol">Metadata format</a>
 */
public class Obd2Data {

    private static final String TAG = "Obd2Data";

    private static final String LINE_SEPARATOR = "\n";

    private final long mTimeStamp;

    private int[] mSpeed = null;

    public Obd2Data(int speed, long millis) {
        mSpeed = new int[1];
        mSpeed[0] = speed;
        mTimeStamp = millis;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(FormatUtils.getMetadataFormatTimestampFromLong(mTimeStamp));
        builder.append(";");
        //null gps data
        builder.append(";;;;;");
        //null rotation
        builder.append(";;;");
        //null accelerometer
        builder.append(";;;");
        //pressure
        builder.append(";");
        //compass
        builder.append(";");
        //video index
        builder.append(";");
        //frame index
        builder.append(";");
        //gravity
        builder.append(";;;");

        if (mSpeed != null) {
            //OBD speed
            builder.append(mSpeed[0]);
        }
        builder.append(";");
        //todo builder.append(vertical_accuracy);
        builder.append(LINE_SEPARATOR);
        return builder.toString();
    }

}
