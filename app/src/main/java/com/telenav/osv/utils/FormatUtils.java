package com.telenav.osv.utils;

import android.annotation.SuppressLint;
import android.content.Context;

import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Represents an utility class for converting an item to a displayable format.
 * Created by cameliao on 1/19/18.
 */

public class FormatUtils {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = FormatUtils.class.getSimpleName();

    public static final String OBD_CONNECTED_HOUR_MIN_FORMAT = "%02d:%02d min";

    public static final String SEPARATOR_SLASH = "/";

    public static final String SEPARATOR_SPACE = " ";

    public static final String FORMAT_ONE_DECIMAL = "#,###,###,###.#";

    public static final String FORMAT_TWO_DECIMAL = "#,###,###,###.##";

    public static final String FORMAT_UNIT_DISTANCE_IMPERIAL_MILES_LABEL = "mile";

    public static final String FORMAT_UNIT_DISTANCE_METRIC_KM = "km";

    public static final String FORMAT_SPEED_KM = "km/h";

    public static final String FORMAT_SPEED_MPH = "mph";

    public static final String METADATA_TIMESTAMP_FORMAT = "%.3f";

    public static final double SECONDS_DIVISION_DOUBLE = 1000.0;

    public static final int TOTAL_BITES_IN_MEGABYTE = 1024;

    public static final String FORMAT_KB_PER_SECONDS = "KB/s";

    public static final String FORMAT_MEGABYTES_PER_SECONDS = "MB/s";

    public static final int TOTAL_SECONDS_IN_MINUTE = 60;

    public static final int TOTAL_MINUTES_IN_HOUR = 60;

    public static final String FORMAT_UNKOWN_ETA = "-";

    public static final String FORMAT_ETA_FOR_TIME_IN_LONG = "%02d:%02d:%02d";

    public static final String FORMAT_SHORT_ONE_DECIMAL = "#.#";

    private static final double KILOMETER_TO_MILE = 0.621371;

    private static final float METER_TO_FEET = 3.28084f;

    private static final double MPH_MULTIPLIER = 2.23694;

    private static final double KMPH_MULTIPLIER = 3.6;

    private static final String SEPARATOR_NONE = "";

    private static final String FORMAT_SIZE_MB = "MB";

    private static final String FORMAT_SIZE_GB = "GB";

    private static final String DEFAULT_SIZE = "0";

    private static final String FORMAT_SIMPLE_DECIMAL = "#,###,###,###";

    private static final String FORMAT_FOUR_DECIMAL = "#,###,###,###.####";

    private static final String FORMAT_UNIT_DISTANCE_IMPERIAL_FEET = "ft";

    private static final String FORMAT_UNIT_DISTANCE_IMPERIAL_MILES = "mi";

    private static final String FORMAT_UNIT_DISTANCE_METRIC_METER = "m";

    private static final DecimalFormat doubleDigitDecimalFormatter = new DecimalFormat("#.#");

    private static final int SECONDS_IN_HOUR = 3600;

    public static final String TASK_AMOUNT_FORMAT = "%.2f";

    /**
     * On some time zones the Chronometer separator is a dot.
     */
    private static String SEPARATOR_DOT = ".";

    /**
     * Default separator for Chronometer.
     */
    private static String SEPARATOR_COLON = ":";

    /**
     * Formats the id of a sequence to a {@code String} which can be displayed in the view.
     * @param sequenceId the sequence id
     * @return a {@code String} representing the displayable format for the given sequence id
     */
    public static String formatSequenceId(long sequenceId) {
        return "#" + String.valueOf(sequenceId);
    }

    /**
     * Formats a given distance value (given in meters)
     * @param distInMeters dist
     * @return strings, value/unit
     */
    public static String[] formatDistanceFromMeters(Context context, int distInMeters, String separator) {
        boolean isUS =
                !((KVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        if (isUS) {
            distInMeters = (int) (distInMeters * METER_TO_FEET);
        }
        DecimalFormat df2 = new DecimalFormat(FORMAT_SHORT_ONE_DECIMAL);
        if (distInMeters < 500) {
            return new String[]{String.valueOf(distInMeters), getDistanceUnitLabel(context, separator, true)};
        } else {
            return new String[]{df2.format((double) distInMeters / (isUS ? 5280 : 1000)), getDistanceUnitLabel(context, separator, false)};
        }
    }

    /**
     * Formats a given distance value (given in meters)
     * @param distInMeters dist
     * @return strings, value/unit
     */
    public static String[] formatDistanceFromMeters(boolean isImperial, int distInMeters) {
        if (isImperial) {
            distInMeters = (int) (distInMeters * METER_TO_FEET);
        }
        DecimalFormat df2 = new DecimalFormat(FORMAT_SHORT_ONE_DECIMAL);
        if (distInMeters < 500) {
            return new String[]{String.valueOf(distInMeters), getDistanceUnitLabel(isImperial, SEPARATOR_SPACE, true)};
        } else {
            return new String[]{df2.format((double) distInMeters / (isImperial ? 5280 : 1000)), getDistanceUnitLabel(isImperial, SEPARATOR_SPACE, false)};
        }
    }

    /**
     * Formats the distance in m/s into metric unit/h.
     *
     * @param isImperial             {@code true} if the format should be imperial, {@code false} otherwise.
     * @param distInKilometerPerHour the distance in km/h.
     * @return {@code String[]} representing the first index the distance formatted and the second index the metric used.
     */
    public static String[] fromDistanceFromMetersToUnit(boolean isImperial, int distInKilometerPerHour) {
        return formatDistanceToMetric(isImperial, distInKilometerPerHour);
    }

    /**
     * Formats the distance in m/s into metric unit/h.
     * @param context the context required to access app shared preferences.
     * @param distInMetersPerSeconds the distance in m/s.
     * @return {@code String[]} representing the first index the distance formatted and the second index the metric used.
     */
    public static String[] fromDistanceFromMetersToUnit(Context context, double distInMetersPerSeconds) {
        boolean isImperial =
                !((KVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        return formatDistanceToMetric(isImperial, distInMetersPerSeconds);
    }

    /**
     * Formats a given distance value (given in km)
     * @param dist dist the distance to modify in a displayable format
     * @param formatPattern the decimal format pattern
     * @return value/unit
     */
    public static String[] formatDistanceFromKiloMeters(Context context, double dist, String formatPattern) {
        boolean isUS =
                !((KVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        if (isUS) {
            dist = dist * KILOMETER_TO_MILE;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat df2 = new DecimalFormat(formatPattern, symbols);
        return new String[]{df2.format(dist), getDistanceUnitLabel(context, SEPARATOR_SPACE, false)};
    }

    public static String formatNumber(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat formatter = new DecimalFormat(FORMAT_SIMPLE_DECIMAL, symbols);
        return formatter.format(value);
    }

    public static String formatSize(double value) {
        String[] sizeText = formatSizeDetailed(value);
        return sizeText[0] + sizeText[1];
    }

    public static String[] formatSizeDetailed(double value) {
        String[] sizeText = new String[]{DEFAULT_SIZE, FORMAT_SIZE_MB};
        if (value > 0) {
            double size = value / (double) 1024 / (double) 1024;
            String type = FORMAT_SIZE_MB;
            DecimalFormat df2 = new DecimalFormat(FORMAT_SHORT_ONE_DECIMAL);
            if (size > 1024) {
                size = (size / (double) 1024);
                type = FORMAT_SIZE_GB;
                sizeText = new String[]{String.valueOf(df2.format(size)), type};
            } else {
                sizeText = new String[]{String.valueOf(df2.format(size)), type};
            }
        }
        return sizeText;
    }

    public static String formatBandwidth(long bandwidth) {
        String bandwithUnit = FORMAT_KB_PER_SECONDS;
        double returnBandwith = ((double) (bandwidth / 8));
        if (returnBandwith > TOTAL_BITES_IN_MEGABYTE) {
            bandwithUnit = FORMAT_MEGABYTES_PER_SECONDS;
            returnBandwith = returnBandwith / TOTAL_BITES_IN_MEGABYTE / TOTAL_BITES_IN_MEGABYTE;
        }
        return String.format("%s %s", doubleDigitDecimalFormatter.format(returnBandwith), bandwithUnit);
    }

    @SuppressLint("DefaultLocale")
    public static String formatETA(long eta) {
        if (eta == 0) {
            return FORMAT_UNKOWN_ETA;
        }

        int hours = (int) (eta / SECONDS_IN_HOUR);
        //reminder of seconds after calculating the hours
        long remainderHoursInSeconds = eta % SECONDS_IN_HOUR;
        int minutes = (int) (remainderHoursInSeconds / TOTAL_SECONDS_IN_MINUTE);
        int seconds = (int) (remainderHoursInSeconds % TOTAL_SECONDS_IN_MINUTE);

        return String.format(FORMAT_ETA_FOR_TIME_IN_LONG, hours, minutes, seconds);
    }

    /**
     * Formats the speed using the selected unit distance.
     * @param context the context to access the application preferences
     * @param speed the speed to convert to a displayable format
     * @return an array of {@code Strings}
     * <p>The first element of the array represents the actual speed value and the second element represents the unit distance </p>
     */
    public static String[] formatSpeedFromKmph(Context context, int speed) {
        boolean isUS =
                !((KVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        return formatSpeedFromKmph(isUS, speed);
    }

    /**
     * Formats the speed using the selected unit distance.
     * @param speed the speed to convert to a displayable format
     * @return an array of {@code Strings}
     * <p>The first element of the array represents the actual speed value and the second element represents the unit distance </p>
     */
    public static String[] formatSpeedFromKmph(boolean imperial, int speed) {
        if (imperial) {
            speed = (int) (speed * KILOMETER_TO_MILE);
            return new String[]{String.valueOf(speed), FORMAT_SPEED_MPH};
        } else {
            return new String[]{String.valueOf(speed), FORMAT_SPEED_KM};
        }
    }

    /**
     * Formats a {@code double} value to a number with two decimals.
     * @param value a {@code double} value to be format
     * @return a {@code String} value representing the displayable format with two decimals of the given value .
     */
    public static String formatMoneyWithTwoDecimals(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        DecimalFormat df2 = new DecimalFormat(FORMAT_TWO_DECIMAL, symbols);
        return df2.format(value);
    }

    public static String formatMoneyConstrained(double value) {
        if (value < 100) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(',');
            DecimalFormat df2 = new DecimalFormat(FORMAT_TWO_DECIMAL, symbols);
            return df2.format(value);
        } else {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator(',');
            DecimalFormat df2 = new DecimalFormat(FORMAT_SIMPLE_DECIMAL, symbols);
            return df2.format(value);
        }
    }

    /**
     * Formats the received {@code double} value to a displayable format using the selected unit distance preference.
     * @param context the application context to read the unit distance from application preference
     * @param value the {@code double} value to format
     * @param withTwoDecimals true - to format the value with two decimals, false -  to format the value with four decimals
     * @return a {@code String} containing the displayable format of the {@code double} value accordingly with the unit distance an the value from {@code withTwoDecimals}
     */
    public static String formatMoneyForPreferredDistanceUnit(Context context, double value, boolean withTwoDecimals) {
        boolean isMiles =
                !((KVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC);
        if (isMiles) {
            value = value / KILOMETER_TO_MILE;
        }
        if (withTwoDecimals) {
            return formatMoneyWithTwoDecimals(value);
        } else {
            return formatMoneyWithFourDecimals(value);
        }
    }

    /**
     * Converts the timestamp from milliseconds to seconds, as it is required for the metadata file
     * @param mTimeStamp the timestamp in milliseconds.
     * @return the formatted metadata compliant timestamp.
     */
    public static String getMetadataFormatTimestampFromLong(long mTimeStamp) {
        double number = (double) mTimeStamp / SECONDS_DIVISION_DOUBLE;
        BigDecimal doubleVal = new BigDecimal(number);
        BigDecimal returnValue = doubleVal.setScale(3, BigDecimal.ROUND_HALF_EVEN);
        return returnValue.toString();
    }

    public static float transformSquareMetersPerSecondIntoGravity(float squareMeters) {
        return squareMeters / 9.80665F;
    }

    /**
     * Converts the timestamp from milliseconds to seconds, as it is required for the metadata file
     * @param mTimeStamp the timestamp in milliseconds.
     * @return double representing the formatted metadata compliant timestamp.
     */
    public static double getMetadataFormatTimestampFromLongAsDouble(long mTimeStamp) {
        return Double.parseDouble(getMetadataFormatTimestampFromLong(mTimeStamp));
    }

    /**
     * @param context the context of the application to access the application preferences
     * @param separator a {@code String} representing the separator for the distance unit label
     * @param isShortDistance true - if the distance is in feet or meters; false - if the distance is in miles or km
     * @return a {@code String} containing the displayable format of the unit distance
     */
    public static String getDistanceUnitLabel(Context context, String separator, boolean isShortDistance) {
        if (!((KVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC)) {
            return separator + (isShortDistance ? FORMAT_UNIT_DISTANCE_IMPERIAL_FEET : FORMAT_UNIT_DISTANCE_IMPERIAL_MILES);
        } else {
            return separator + (isShortDistance ? FORMAT_UNIT_DISTANCE_METRIC_METER : FORMAT_UNIT_DISTANCE_METRIC_KM);
        }
    }

    /**
     * @param isImperial {@code true} if the distance is formated in imperial, {@code false} otherwise.
     * @param separator a {@code String} representing the separator for the distance unit label
     * @param isShortDistance true - if the distance is in feet or meters; false - if the distance is in miles or km
     * @return a {@code String} containing the displayable format of the unit distance
     */
    public static String getDistanceUnitLabel(boolean isImperial, String separator, boolean isShortDistance) {
        if (isImperial) {
            return separator + (isShortDistance ? FORMAT_UNIT_DISTANCE_IMPERIAL_FEET : FORMAT_UNIT_DISTANCE_IMPERIAL_MILES.toUpperCase());
        } else {
            return separator + (isShortDistance ? FORMAT_UNIT_DISTANCE_METRIC_METER : FORMAT_UNIT_DISTANCE_METRIC_KM.toUpperCase());
        }
    }

    /**
     * @param context the context of the application to access the app preferences
     * @param isShortDistance true - if the distance is in feet or meters; false - if the distance is in miles or km
     * @return a {@code String} containing the displayable format of the unit distance without a separator
     */
    public static String getDistanceUnitLabel(Context context, boolean isShortDistance) {
        return getDistanceUnitLabel(context, SEPARATOR_NONE, isShortDistance);
    }

    /**
     * Converts an array of {@link String} elements to a single {@code String} element.
     * @param array the array to be converted
     * @return a {@code String} object containing all the data from the array.
     */
    public static String convertStringArrayToString(String[] array) {
        StringBuilder builder = new StringBuilder();
        for (String string : array) {
            builder.append(string);
        }
        return builder.toString();
    }

    /**
     * Format teh Chronometer text to a proper display format.
     * @param text the text from chronometer.
     * @return teh formatted text.
     */
    public static String formatChronometerText(String text) {
        return text.replace(SEPARATOR_DOT, SEPARATOR_COLON);
    }

    /**
     * Formats a {@code double} number using a given format pattern.
     * @param number the number which should be formatted.
     * @param formatPattern the format pattern.
     * @return a {@code String} representing the number in the given format.
     */
    public static String formatDecimalNumber(double number, String formatPattern) {
        return new DecimalFormat(formatPattern).format(number);
    }

    /**
     * Formats the distance in m/s into metric unit/h.
     * @param isImperial {@code true} if the format should be imperial, {@code false} otherwise.
     * @param distInMetersPerSeconds the distance in m/s.
     * @return {@code String[]} representing the first index the distance formatted and the second index the metric used.
     */
    private static String[] formatDistanceToMetric(boolean isImperial, double distInMetersPerSeconds) {
        String distanceInUnitMetric;
        String metric;
        if (isImperial) {
            distanceInUnitMetric = String.valueOf((int) (distInMetersPerSeconds * MPH_MULTIPLIER));
            metric = FORMAT_SPEED_MPH;
        } else {
            distanceInUnitMetric = String.valueOf((int) (distInMetersPerSeconds * KMPH_MULTIPLIER));
            metric = FORMAT_SPEED_KM;
        }

        return new String[]{distanceInUnitMetric, metric};
    }

    /**
     * Formats the distance in m/s into metric unit/h.
     *
     * @param isImperial             {@code true} if the format should be imperial, {@code false} otherwise.
     * @param distInMetersPerSeconds the distance in m/s.
     * @return {@code String[]} representing the first index the distance formatted and the second index the metric used.
     */
    private static String[] formatDistanceToMetric(boolean isImperial, int distInKilometersPerHour) {
        String distanceInUnitMetric;
        String metric;
        if (isImperial) {
            distanceInUnitMetric = String.valueOf((int) (distInKilometersPerHour * KILOMETER_TO_MILE));
            metric = FORMAT_SPEED_MPH;
        } else {
            distanceInUnitMetric = String.valueOf(distInKilometersPerHour);
            metric = FORMAT_SPEED_KM;
        }

        return new String[]{distanceInUnitMetric, metric};
    }

    /**
     * Formats a {@code double} value to a number with four decimals.
     *
     * @param value a {@code double} value to be format
     * @return a {@code String} value representing the displayable format with four decimals of the given value.
     */
    private static String formatMoneyWithFourDecimals(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        DecimalFormat df2 = new DecimalFormat(FORMAT_FOUR_DECIMAL, symbols);
        return df2.format(value);
    }
}
