package com.telenav.osv.recorder.shutter.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.listener.ShutterListener;
import com.telenav.osv.utils.Log;
import androidx.annotation.IntDef;

/**
 * abstract class containing common shutter logic methods
 * Created by Kalman on 17/05/2017.
 */
public abstract class ShutterLogic {

    static final int PRIORITY_IDLE = 1;

    static final int PRIORITY_AUTO = 2;

    static final int PRIORITY_GPS = 3;

    static final int PRIORITY_OBD = 4;

    // https://en.wikipedia.org/wiki/Phrases_from_The_Hitchhiker%27s_Guide_to_the_Galaxy#Answer_to_the_Ultimate_Question_of_Life,_the_Universe,_and_Everything_(42)
    static final int PRIORITY_BENCHMARKING = 42;

    private static final String TAG = "ShutterLogic";

    ShutterListener mShutterListener;

    @SpeedCategory
    int mSpeedCategory = SpeedCategory.SPEED_STATIONARY;

    float mSpeed = 0;

    private boolean mFunctional = false;

    public void setShutterListener(ShutterListener shutterListener) {
        mShutterListener = shutterListener;
    }

    public abstract void onLocationChanged(Location location);

    public abstract void onSpeedChanged(SpeedData speedData);

    public boolean isFunctional() {
        return mFunctional;
    }

    public void setFunctional(boolean mFunctional) {
        this.mFunctional = mFunctional;
    }

    public boolean betterThan(ShutterLogic logic) {
        return mFunctional && (!logic.mFunctional || getPriority() > logic.getPriority());
    }

    public void start() {
    }

    public void stop() {
    }

    void recalculateSpeedCategory(float speed) {
        mSpeed = speed;
        //        Log.d(TAG, "recalculateSpeedCategory: speed: " + (int) mSpeed);
        int newCategory = SpeedCategory.SPEED_STATIONARY;
        if (mSpeed <= 1) {
            newCategory = SpeedCategory.SPEED_STATIONARY;
        } else if (mSpeed <= 10) {
            newCategory = SpeedCategory.SPEED_5;
        } else if (mSpeed <= 30) {
            newCategory = SpeedCategory.SPEED_8;
        } else if (mSpeed <= 50) {
            newCategory = SpeedCategory.SPEED_12;
        } else if (mSpeed <= 90) {
            newCategory = SpeedCategory.SPEED_15;
        } else if (mSpeed <= 120) {
            newCategory = SpeedCategory.SPEED_20;
        } else if (mSpeed > 120) {
            newCategory = SpeedCategory.SPEED_25;
        }
        if (newCategory != mSpeedCategory) {
            Log.d(TAG, "recalculateSpeedCategory: speed category changed " + newCategory);
            mSpeedCategory = newCategory;
        }
    }

    void destroy() {
        mShutterListener = null;
    }

    abstract int getPriority();

    /**
     * The speed category values are based on m/s.
     */

    @IntDef({SpeedCategory.SPEED_STATIONARY, SpeedCategory.SPEED_5, SpeedCategory.SPEED_8, SpeedCategory.SPEED_12,
            SpeedCategory.SPEED_15, SpeedCategory.SPEED_20, SpeedCategory.SPEED_25})
    public @interface SpeedCategory {
        int SPEED_STATIONARY = 10000;

        int SPEED_5 = 5;

        int SPEED_8 = 8;

        int SPEED_12 = 12;

        int SPEED_15 = 15;

        int SPEED_20 = 20;

        int SPEED_25 = 25;
    }
}
