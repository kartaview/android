package com.telenav.osv.manager.shutterlogic;

import android.location.Location;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.gps.SpeedCategoryEvent;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.listener.ShutterListener;
import com.telenav.osv.utils.Log;

/**
 * abstract class containing common shutter logic methods
 * Created by Kalman on 17/05/2017.
 */
public abstract class ShutterLogic {

    static final int PRIORITY_IDLE = 1;

    static final int PRIORITY_AUTO = 2;

    static final int PRIORITY_GPS = 3;

    static final int PRIORITY_OBD = 4;

    private static final String TAG = "ShutterLogic";

    ShutterListener mShutterListener;

    float mSpeed = 0;

    SpeedCategoryEvent.SpeedCategory mSpeedCategory = SpeedCategoryEvent.SpeedCategory.SPEED_STATIONARY;

    private boolean mFunctional = false;

    public void setShutterListener(ShutterListener shutterListener) {
        mShutterListener = shutterListener;
    }

    public abstract void onLocationChanged(Location reference, Location location);

    public abstract void onSpeedChanged(SpeedData speedData);

    void recalculateSpeedCategory(float speed) {
        mSpeed = speed;
//        Log.d(TAG, "recalculateSpeedCategory: speed: " + (int) mSpeed);
        SpeedCategoryEvent.SpeedCategory newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_STATIONARY;
        if (mSpeed <= 1) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_STATIONARY;
        } else if (mSpeed <= 10) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_5;
        } else if (mSpeed <= 30) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_10;
        } else if (mSpeed <= 50) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_15;
        } else if (mSpeed <= 90) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_20;
        } else if (mSpeed <= 120) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_25;
        } else if (mSpeed > 120) {
            newCategory = SpeedCategoryEvent.SpeedCategory.SPEED_35;
        }
        if (newCategory != mSpeedCategory) {
            Log.d(TAG, "recalculateSpeedCategory: speed category changed " + newCategory);
            mSpeedCategory = newCategory;
            EventBus.postSticky(new SpeedCategoryEvent(mSpeed, mSpeedCategory));
        }
    }

    public boolean isFunctional() {
        return mFunctional;
    }

    public void setFunctional(boolean mFunctional) {
        this.mFunctional = mFunctional;
    }

    void destroy() {
        mShutterListener = null;
    }

    public boolean betterThan(ShutterLogic logic) {
        return mFunctional && (!logic.mFunctional || getPriority() > logic.getPriority());
    }

    abstract int getPriority();

    public void start() {}

    public void stop() {}

}
