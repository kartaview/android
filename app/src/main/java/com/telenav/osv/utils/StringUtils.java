package com.telenav.osv.utils;

/**
 * Utils class for {@code String} primitives.
 * @author horatiuf
 */

public class StringUtils {

    /**
     * Empty string constant
     */
    public static final String EMPTY_STRING = "";

    /**
     * Format in order to right pad a string with specific characters.
     */
    public static final String RIGHT_PADDING_STRING_FORMATTER = "%1$-";

    /**
     * The representation of 0 as a character.
     */
    public static final char ZERO_CHARACTER = '0';

    /**
     * The representation of space as a character.
     */
    public static final char SPACE_CHARACTER = ' ';

    /**
     * @param stringValue the value which will be validated.
     * @return {@code true} if the {@code String} is empty, {@code false} otherwise.
     */
    public static boolean isEmpty(String stringValue) {
        return stringValue == null || stringValue.isEmpty();
    }

    /**
     * Adds the {@code zero} character to the right of a number. This is based on given length of a number.
     * @param str the string which will be right padded with zero characters.
     * @param number the length of the formatted number.
     * @return {@code String} representing the number formatted with given params.
     */
    public static String rightPadZeros(String str, long number) {
        return String.format(RIGHT_PADDING_STRING_FORMATTER + number + "s", str).replace(SPACE_CHARACTER, ZERO_CHARACTER);
    }
}
