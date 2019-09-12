package com.telenav.osv.item.metadata;

import com.telenav.osv.utils.FormatUtils;
import com.telenav.osv.utils.Log;

/**
 * Data object which contains a track id and a frame index within that track. This object's {@link #toString()} knows how to format its data s.t. it's appropriate to write in
 * the metadata file of a sequence.
 * <p>
 * Metadata format version 1.1.6.
 * @see <a href="http://spaces.telenav.com:8080/display/TELENAVEU/Metadata+Format+Protocol">Metadata format</a>
 */
public class VideoData {

    private static final String LINE_SEPARATOR = "\n";

    private static final String TAG = "VideoData";

    private final long mTimeStamp;

    private int[] mIndex;

    private int[] mVideoIndex;

    public VideoData(int index, int videoIndex, long millis) {
        mIndex = new int[1];
        mVideoIndex = new int[1];
        mIndex[0] = index;
        mVideoIndex[0] = videoIndex;
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
        if (mVideoIndex != null) {
            builder.append(mVideoIndex[0]);
        }
        builder.append(";");
        if (mIndex != null) {
            builder.append(mIndex[0]);
        }
        builder.append(";");
        //gravity
        builder.append(";;;");
        builder.append(";");
        //todo builder.append(vertical_accuracy);
        builder.append(LINE_SEPARATOR);
        String str = builder.toString();
        if (mIndex != null && mVideoIndex != null) {
            Log.d(TAG, "toString: created for video file = " + mVideoIndex[0] + " and frame = " + mIndex[0]);
        }
        return str;
    }
}