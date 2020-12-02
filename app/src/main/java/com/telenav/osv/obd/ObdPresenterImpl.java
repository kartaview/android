package com.telenav.osv.obd;

import java.util.ArrayList;
import java.util.List;
import com.telenav.osv.obd.ObdContract.ObdPresenter;
import com.telenav.osv.obd.ObdContract.PermissionsListener;
import com.telenav.osv.utils.PermissionCode;

/**
 * Concrete implementation for {@link ObdPresenter}.
 * @author cameliao
 * @see ObdPresenter
 */
public class ObdPresenterImpl implements ObdContract.ObdPresenter {

    /**
     * List of permissions listener which will be notified when a permission is granted.
     */
    private List<PermissionsListener> permissionsListenerList = new ArrayList<>();

    @Override
    public void addPermissionListener(ObdContract.PermissionsListener listener) {
        permissionsListenerList.add(listener);
    }

    @Override
    public void notifyRecordingPermissionListeners() {
        for (ObdContract.PermissionsListener permissionListener :
                permissionsListenerList) {
            permissionListener.onPermissionGranted(PermissionCode.PERMISSION_RECORD);
        }
    }

    @Override
    public void notifyCameraPermissionListeners() {
        for (ObdContract.PermissionsListener permissionListener :
                permissionsListenerList) {
            permissionListener.onPermissionGranted(PermissionCode.PERMISSION_CAMERA);
        }
    }
}
