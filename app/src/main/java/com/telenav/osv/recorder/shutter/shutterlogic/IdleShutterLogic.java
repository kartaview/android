package com.telenav.osv.recorder.shutter.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;
import com.telenav.osv.utils.Log;

/**
 * idle shutter logic
 * Created by Kalman on 17/05/2017.
 */
public class IdleShutterLogic extends ShutterLogic {

    private static final String TAG = IdleShutterLogic.class.getSimpleName();

    public IdleShutterLogic() {
        setFunctional(true);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "#onLocationChanged");
    }

    @Override
    public void onSpeedChanged(SpeedData speedData) {

    }

    @Override
    int getPriority() {
        return PRIORITY_IDLE;
    }
}
