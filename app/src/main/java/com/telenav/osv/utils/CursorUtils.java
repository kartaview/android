package com.telenav.osv.utils;

import android.database.Cursor;
import androidx.annotation.Nullable;

/**
 * Helper class for cursor related operations for all primitive types.
 * @author horatiuf
 */

public class CursorUtils {

    /**
     * The constant value for DB true.
     */
    public static final int DB_TRUE = 1;

    /**
     * private constructor just to make sure this class cannot be instantiated
     */
    private CursorUtils() {}

    /**
     * Returns {@code String} value for a cursor column. The column identifier must be for an {@code String } type..
     * <p>The cursor is <i>closed</i> after returning the value.</p>
     * @param cursor {@link Cursor} object containing data
     * @param columnName the column for which value needs to be returned.
     * @return {@code String} value of the column.
     */
    @Nullable
    public static String getStringValueByColumn(Cursor cursor, String columnName) {
        if (cursor == null) {
            return null;
        }
        String stringValue = null;
        if (!isCursorEmpty(cursor)) {
            cursor.moveToFirst();
            stringValue = cursor.getString(cursor.getColumnIndex(columnName));
        }
        cursor.close();
        return stringValue;
    }

    /**
     * Returns {@code Integer} value for a cursor column. The column identifier must be for an {@code int} type..
     * <p>The cursor is <i>closed</i> after returning the value.</p>
     * @param cursor {@link Cursor} object containing data/
     * @param columnName the column for which value needs to be returned.
     * @return {@code Integer} value of the column.
     */
    public static int getIntValueByColumn(Cursor cursor, String columnName) {
        if (cursor == null) {
            return 0;
        }
        int intValue = 0;
        if (!isCursorEmpty(cursor)) {
            cursor.moveToFirst();
            intValue = cursor.getInt(cursor.getColumnIndex(columnName));
        }
        cursor.close();
        return intValue;
    }

    /**
     * Wrapper method for boolean value from an int column since sql lite doesn't support the boolean primitive. The column identifier must be for an {@code int} type. uses
     * internally {@link Cursor#getInt(int)} and checking with the value that represents {@code true} for boolean if matches.
     * <p>The cursor is <i>closed</i> after returning the value.</p>
     * @param cursor {@link Cursor} object containing data/
     * @param columnName the column for which value needs to be returned.
     * @return {@code true} if the value of the int column is equal {@link #DB_TRUE}, otherwise {@code false}.
     */
    public static boolean getBooleanValueByIntColumn(Cursor cursor, String columnName) {
        if (cursor == null) {
            return false;
        }
        boolean booleanValue = false;
        if (!isCursorEmpty(cursor)) {
            cursor.moveToFirst();
            booleanValue = cursor.getInt(cursor.getColumnIndex(columnName)) == DB_TRUE;
        }
        cursor.close();
        return booleanValue;
    }

    /**
     * Returns {@code Long} value for a cursor column. The column identifier must be for an {@code login} type.
     * <p>The cursor is <i>closed</i> after returning the value.</p>
     * @param cursor {@link Cursor} object containing data.
     * @param columnName the column for which value needs to be returned.
     * @return {@code Long} value of the column.
     */
    public static long getLongValueByColumn(Cursor cursor, String columnName) {
        if (cursor == null) {
            return 0;
        }
        long longValue = 0;
        if (!isCursorEmpty(cursor)) {
            cursor.moveToFirst();
            longValue = cursor.getLong(cursor.getColumnIndex(columnName));
        }
        cursor.close();
        return longValue;
    }

    /**
     * Helper method to checks if a cursor is empty.
     * @param cursor the {@link Cursor} to be checked.
     * @return {@code true} if the cursor is empty, {@code false} otherwise.
     */
    public static boolean isCursorEmpty(Cursor cursor) {
        return cursor == null || cursor.getCount() == 0;
    }

    /**
     * Helper method to close a cursor.
     * @param cursor the {@code Cursor} to be closed if valid.
     */
    public static void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
