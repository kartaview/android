package com.telenav.osv.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.joda.time.DateTime;
import android.annotation.SuppressLint;
import com.telenav.osv.data.score.model.ScoreHistory;
import androidx.room.TypeConverter;

/**
 * Helper class in order to convert the received information from the server into a displayable mode.
 * Created by cameliao on 2/14/18.
 */
public class ConverterHelper {

    private static final String DATABASE_LOCATION_PATTERN = "%f,%f";

    /**
     * The regex used in order to split longitude from latitude in the {@link #DATABASE_LOCATION_PATTERN}.
     */
    private static final String DATABASE_LOCATION_REGEX = ",";

    /**
     * The index representing the latitude in an array format.
     */
    private static final int LATITUDE_INDEX = 0;

    /**
     * The index presenting the longitude in an array format.
     */
    private static final int LONGITUDE_INDEX = 1;

    /**
     * Converts the received score history map having the coverage as a key to a new map containing as a key the points multiplier.
     * The points multiplier is format by multiplying the value of the segment's coverage and the obd multiplier(1 if obd was disconnected, 2 otherwise).
     * @param scoreHistoryMap the map containing all the history score for a sequence arranged by their coverages.
     * @return a new map with the score history arranged by their multiplier value.
     */
    public static Map<Integer, ScoreHistory> getScoreBreakdownDetails(Map<Integer, ScoreHistory> scoreHistoryMap) {
        @SuppressLint("UseSparseArrays") Map<Integer, ScoreHistory> res = new HashMap<>();
        if (scoreHistoryMap == null || scoreHistoryMap.isEmpty()) {
            return res;
        }
        ArrayList<ScoreHistory> array = new ArrayList<>(scoreHistoryMap.values());
        Iterator<ScoreHistory> iterator = array.iterator();
        while (iterator.hasNext()) {
            ScoreHistory scoreHistory = iterator.next();
            if (scoreHistory.getCoverage() == -1) {
                iterator.remove();
            }
        }
        for (ScoreHistory hist : array) {
            Integer coverageMultiplier = Utils.getValueOnSegment(hist.getCoverage()) * hist.getObdStatus();
            if (res.containsKey(coverageMultiplier)) {
                ScoreHistory existing = res.get(coverageMultiplier);
                existing.setPhotoCount(existing.getPhotoCount() + hist.getPhotoCount());
                existing.setObdPhotoCount(existing.getObdPhotoCount() + hist.getObdPhotoCount());
            } else {
                res.put(coverageMultiplier, hist);
            }
        }
        return res;
    }

    @TypeConverter
    public static DateTime toDateTime(Long value) {
        return value == null ? null : new DateTime(value);
    }

    @TypeConverter
    public static Long toLong(DateTime value) {
        return value == null ? null : value.getMillis();
    }

    /*@TypeConverter
    public static String fromLocation(Location location) {
        if (location == null) {
            return (null);
        }

        return (String.format(Locale.US,
                DATABASE_LOCATION_PATTERN,
                location.getLatitude(),
                location.getLongitude()));
    }

    *//**
     * @param latlon the lat
     * @return {@code Location} object from a String made in a specific pattern represented by {@link #DATABASE_LOCATION_PATTERN}.
     *//*
    @TypeConverter
    public static Location toLocation(String latlon) {
        if (latlon == null) {
            return (null);
        }

        String[] pieces = latlon.split(DATABASE_LOCATION_REGEX);
        Location result = new Location(StringUtils.EMPTY_STRING);

        result.setLatitude(Double.parseDouble(pieces[LATITUDE_INDEX]));
        result.setLongitude(Double.parseDouble(pieces[LONGITUDE_INDEX]));

        return (result);
    }*/
}
