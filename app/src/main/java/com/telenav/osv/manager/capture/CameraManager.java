package com.telenav.osv.manager.capture;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.view.OrientationEventListener;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.ui.OrientationChangedEvent;
import com.telenav.osv.listener.ImageReadyCallback;
import com.telenav.osv.listener.ShutterCallback;
import com.telenav.osv.utils.FixedQueue;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Size;
import com.telenav.osv.utils.Utils;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Abstract camera manager
 * Created by Kalman on 17/02/2017.
 */
public abstract class CameraManager {

  public final static String TAG = "CameraManager";

  final OrientationEventListener mOrientationListener;

  protected Context mContext;

  int mOrientation = -1;

  CameraManager(Context context) {
    mContext = context;
    mOrientationListener = new OrientationListener(context);
  }

  public static CameraManager get(Context context) {
    CameraManager manager;
    //        if (isNewApiCompatible() && ((OSVApplication) context.getApplicationContext()).getAppPrefs().getBooleanPreference
    // (PreferenceTypes.K_USE_CAMERA_API_NEW)) {
    //            manager = new Camera2Manager(context);
    //        } else {
    manager = new CameraManagerOld(context);
    //        }
    EventBus.register(manager);
    return manager;
  }

  private static boolean isNewApiCompatible() {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
  }

  public abstract void takeSnapshot(final ShutterCallback shutterCallback, final ImageReadyCallback jpeg, final long timestamp,
                                    final int sequenceId, final String folderPath, final Location location);

  public abstract void open();

  public abstract void release();

  public abstract void setPreviewSurface(SurfaceTexture surface);

  public void focusOnTouch(int x, int y) {
    float top, left, right, bottom;
    boolean isLeftMinus = false, isTopMinus = false, isRightOver = false, isBottomOver = false;

    x = x * 2 - 1000;
    y = y * 2 - 1000;

    int r = 100;

    left = x - r;
    right = x + r;
    top = y - r;
    bottom = y + r;

    int hOffset = (int) ((left < -1000) ? ((left + 1000) * (-1)) : ((right > 1000) ? ((right - 1000) * (-1)) : (0)));
    int vOffset = (int) ((top < -1000) ? ((top + 1000) * (-1)) : ((bottom > 1000) ? ((bottom - 1000) * (-1)) : (0)));

    Rect cameraFocusRect = new Rect((int) (left + hOffset), (int) (top + vOffset), (int) (right + hOffset), (int) (bottom + vOffset));
    Log.d(TAG, "focusOnTouch: " + cameraFocusRect);
    //        Log.d(TAG, "focusOnTouch: surfaceWidth=" + surfaceWidth + ", surfaceHeight=" + surfaceHeight + ", radius=" + radius + ",
    // x=" + x + ", y=" + y);
    focus(cameraFocusRect);
  }

  protected abstract void focus(Rect cameraFocusRect);

  public abstract void forceCloseCamera();

  public abstract List<Size> getSupportedPictureSizes();

  protected abstract void setOrientation(int value);

  public abstract void unlockFocus();

  private class OrientationListener extends OrientationEventListener {

    private FixedQueue queue;

    OrientationListener(Context context) {
      super(context);
      EventBus.register(this);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onConfigChanged(OrientationChangedEvent event) {
      if (queue != null) {
        int orientation = queue.get(queue.size() - 1);
        Log.d(TAG, "onConfigChanged: setting orientation " + orientation);
        setOrientation(orientation);
      }
    }

    @Override
    public void onOrientationChanged(int orientation) {
      // We keep the last known orientation. So if the user first orient
      // the camera then point the camera to floor or sky, we still have
      // the correct orientation.
      if (orientation == ORIENTATION_UNKNOWN) {
        return;
      }
      final int value = Utils.roundOrientation(orientation, mOrientation);

      if (queue == null) {
        queue = new FixedQueue(new Integer[] {value, value, value, value, value, value});
        setOrientation(value);
      }
      queue.offer(value);
      if (value == mOrientation) {
        return;
      }
      for (int i = 0; i < queue.size(); i++) {
        if (value != queue.get(i)) {
          Log.d(TAG, "onOrientationChanged: not valid");
          return;
        }
      }
      Log.d(TAG, "onOrientationChanged: valid orientation: " + value);
      setOrientation(value);
    }
  }
}
