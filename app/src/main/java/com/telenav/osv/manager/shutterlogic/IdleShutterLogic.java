package com.telenav.osv.manager.shutterlogic;

import android.location.Location;
import com.telenav.osv.item.SpeedData;

/**
 * idle shutter logic
 * Created by Kalman on 17/05/2017.
 */
public class IdleShutterLogic extends ShutterLogic {

  public IdleShutterLogic() {
    setFunctional(true);
  }

  @Override
  public void onLocationChanged(Location reference, Location location) {

  }

  @Override
  public void onSpeedChanged(SpeedData speedData) {

  }

  @Override
  int getPriority() {
    return PRIORITY_IDLE;
  }
}
