package com.telenav.osv.obd;

import com.telenav.osv.utils.PermissionCode;

/**
 * The contract between the OBD view and the OBD presenter.
 */
public interface ObdContract {

    /**
     * The OBD presenter which holds all the available business functionality required by the view.
     */
    interface ObdPresenter {

        /**
         * Adds a permission listener to be notified when a permission is granted.
         * @param listener the listener to be added.
         */
        void addPermissionListener(PermissionsListener listener);

        /**
         * Notifies the listeners when a the recording permissions are granted.
         */
        void notifyRecordingPermissionListeners();

        /**
         * Notifies the listeners when the camera permissions are granted.
         */
        void notifyCameraPermissionListeners();
    }

    /**
     * Interface representing permission listener.
     * <ul>
     * <li>{@link #onPermissionGranted(int)} ()}</li>
     * </ul>
     */
    interface PermissionsListener {

        /**
         * Called when the a permissions is granted.
         * @param permissionCode the code of the granted permission
         */
        void onPermissionGranted(@PermissionCode int permissionCode);
    }

}
