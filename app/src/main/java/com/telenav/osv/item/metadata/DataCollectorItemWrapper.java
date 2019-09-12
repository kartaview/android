package com.telenav.osv.item.metadata;

import com.telenav.datacollectormodule.datatype.datatypes.AccuracyObject;
import com.telenav.datacollectormodule.datatype.datatypes.AltitudeObject;
import com.telenav.datacollectormodule.datatype.datatypes.BaseObject;
import com.telenav.datacollectormodule.datatype.datatypes.GPSData;
import com.telenav.datacollectormodule.datatype.datatypes.PositionObject;
import com.telenav.datacollectormodule.datatype.datatypes.PressureObject;
import com.telenav.datacollectormodule.datatype.datatypes.SpeedObject;
import com.telenav.datacollectormodule.datatype.datatypes.ThreeAxesObject;
import com.telenav.datacollectormodule.datatype.util.LibraryUtil;
import com.telenav.osv.utils.FormatUtils;

/**
 * Wrapper for a DataCollector {@link BaseObject}. This is basically just a {@link BaseObject} that knows how to format its data s.t.
 * it's appropriate to write in the metadata file of a sequence.
 * <p>
 * Metadata format version 1.1.6.
 * @see <a href="http://spaces.telenav.com:8080/display/TELENAVEU/Metadata+Format+Protocol">Metadata format</a>
 */
public class DataCollectorItemWrapper {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = DataCollectorItemWrapper.class.getSimpleName();

    private static final String DIGITS_FORMAT = "%03d";

    private BaseObject baseObject;

    public DataCollectorItemWrapper(BaseObject baseObject) {
        this.baseObject = baseObject;
    }

    @Override
    public String toString() {
        StringBuilder result;
        switch (baseObject.getSensorType()) {
            case LibraryUtil.LINEAR_ACCELERATION:
                ThreeAxesObject linearAccelerationObject = (ThreeAxesObject) baseObject;
                result = new StringBuilder();
                result.append(FormatUtils.getMetadataFormatTimestampFromLong(baseObject.getTimestamp()))
                        .append(";;;;;;;;;")
                        .append(linearAccelerationObject.getxValue())
                        .append(";")
                        .append(linearAccelerationObject.getyValue())
                        .append(";")
                        .append(linearAccelerationObject.getzValue())
                        .append(";;;;;;;;;\n");
                return result.toString();

            case LibraryUtil.PRESSURE:
                PressureObject pressureObject = (PressureObject) baseObject;
                result = new StringBuilder();
                result.append(FormatUtils.getMetadataFormatTimestampFromLong(pressureObject.getTimestamp()))
                        .append(";;;;;;;;;;;;")
                        .append(pressureObject.getPressure())
                        .append(";;;;;;;;\n");

                return result.toString();

            case LibraryUtil.GRAVITY:
                ThreeAxesObject gravityObject = ((ThreeAxesObject) baseObject);
                result = new StringBuilder();
                result.append(FormatUtils.getMetadataFormatTimestampFromLong(gravityObject.getTimestamp()))
                        .append(";;;;;;;;;;;;;;;;")
                        .append(gravityObject.getxValue())
                        .append(";")
                        .append(gravityObject.getyValue())
                        .append(";")
                        .append(gravityObject.getzValue())
                        .append(";;\n");

                return result.toString();

            case LibraryUtil.ROTATION_VECTOR_RAW:
                ThreeAxesObject rotVectRawObject = ((ThreeAxesObject) baseObject);
                result = new StringBuilder();
                result.append(FormatUtils.getMetadataFormatTimestampFromLong(rotVectRawObject.getTimestamp()))
                        .append(";;;;;;")
                        .append(rotVectRawObject.getzValue())
                        .append(";")
                        .append(rotVectRawObject.getxValue())
                        .append(";")
                        .append(rotVectRawObject.getyValue())
                        .append(";;;;;;;;;;;;\n");
                return result.toString();

            case LibraryUtil.PHONE_GPS:
                PositionObject positionObject = (PositionObject) baseObject;
                result = new StringBuilder();
                result.append(FormatUtils.getMetadataFormatTimestampFromLong(positionObject.getTimestamp()))
                        .append(";")
                        .append(positionObject.getLon())
                        .append(";")
                        .append(positionObject.getLat())
                        .append(";;;;;;;;;;;;;;;;;;\n");

                return result.toString();

            case LibraryUtil.SPEED:
                SpeedObject speedObject = (SpeedObject) baseObject;
                result = new StringBuilder();
                result.append(FormatUtils.getMetadataFormatTimestampFromLong(speedObject.getTimestamp()))
                        .append(";;;;;;;;;;;;;;;;;;;")
                        .append(speedObject.getSpeed())
                        .append(";\n");

                return result.toString();

            case LibraryUtil.GPS_DATA:
                GPSData gpsData = (GPSData) baseObject;
                result = new StringBuilder();

                PositionObject position = gpsData.getPositionObject();
                AltitudeObject elevation = gpsData.getAltitudeObject();
                AccuracyObject accuracy = gpsData.getAccuracyObject();
                SpeedObject speed = gpsData.getSpeedObject();

                result.append(FormatUtils.getMetadataFormatTimestampFromLong(gpsData.getTimestamp()))
                        .append(";")
                        .append(position.getLon())
                        .append(";")
                        .append(position.getLat())
                        .append(";");

                if (elevation != null) {
                    result.append(elevation.getAltitude());
                }

                result.append(";");

                if (accuracy != null) {
                    result.append(accuracy.getAccuracy());
                }

                result.append(";");

                if (speed != null) {
                    result.append(speed.getSpeed());
                }

                result.append(";;;;;;;;;;;;;;;\n");

                return result.toString();

            case LibraryUtil.HEADING:
                ThreeAxesObject compassObject = (ThreeAxesObject) baseObject;
                result = new StringBuilder();

                result.append(FormatUtils.getMetadataFormatTimestampFromLong(compassObject.getTimestamp()))
                        .append(";;;;;;;;;;;;;")
                        .append(compassObject.getzValue())
                        .append(";;;;;;;\n");
                return result.toString();

            default:
                return "";
        }
    }

    /**
     * Returns the type of data contained by this item wrapper instance.
     * @return a string from {@link LibraryUtil} representing the type of the sensor wrapped by this instance.
     */
    public String getType() {
        return baseObject.getSensorType();
    }
}
