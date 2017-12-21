package com.telenav.osv.manager.capture;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 15/02/2017.
 */
public abstract class DataSaver {

    public final static String TAG = "DataSaver";

    public static final int MIN_FREE_SPACE = 500;

    protected LocalSequence mSequence;

    protected Handler mBackgroundHandler;

    protected int mIndex = 0;

    public void saveFrame(final byte[] jpegData, final Location mLocationF, final float mAccuracyF, final int mOrientationF,
                          final long mTimestampF) {
        if (mSequence.getLocation().getLatitude() == 0 && mSequence != null) {
            mSequence.getLocation().setLatitude(mLocationF.getLatitude());
            mSequence.getLocation().setLongitude(mLocationF.getLongitude());
            SequenceDB.instance.updateSequenceLocation(mSequence.getId(), mLocationF.getLatitude(), mLocationF.getLongitude());
        }
    }

    public boolean isStorageAvailable(Context context) {
        int reserved = MIN_FREE_SPACE;
        int available = (int) Utils.getAvailableSpace(context);
        Log.d(TAG, "saveFrame: entered data handler");
        return available > reserved;
    }

    public abstract void finish();

    public Sequence getSequence() {
        return mSequence;
    }
}
