package com.telenav.osv.upload;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.telenav.osv.upload.settings.SettingsUploadBase;
import com.telenav.osv.upload.settings.SettingsUploadManual;
import com.telenav.osv.upload.status.ServiceUpload;
import com.telenav.osv.upload.status.UploadServiceConnection;
import com.telenav.osv.upload.status.UploadStatus;
import com.telenav.osv.utils.Log;

/**
 * @author horatiuf
 */

public class UploadManagerImpl implements UploadManager {

    public static final String SEQUENCES_IDS = "SEQUENCES_IDS";

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = UploadManagerImpl.class.getSimpleName();

    private static UploadManagerImpl INSTANCE = null;

    /**
     * Connection class required for manual upload to establish a direct link in order to control the service.
     */
    private UploadServiceConnection connection;

    /**
     * Settings instance in order to setup any upload related perquisites.
     */
    private SettingsUploadBase settingsUpload;

    /**
     * Listener which will be notified of any upload updates.
     */
    private UploadStatus listener;

    /**
     * Reference to the upload intent which will be used to also start and bind the upload service.
     */
    private Intent uploadIntent;

    /**
     * Default constructor for the current class.
     */
    private UploadManagerImpl() {
    }

    public static UploadManagerImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UploadManagerImpl();
        }
        return INSTANCE;
    }

    @Override
    public void start() {
        if (settingsUpload instanceof SettingsUploadManual) {
            connection = new UploadServiceConnection();
            SettingsUploadManual settingsUploadManual = (SettingsUploadManual) settingsUpload;
            Context context = settingsUploadManual.getContext();
            uploadIntent = new Intent(context, ServiceUploadImpl.class);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                context.startService(uploadIntent);
            } else {
                context.startForegroundService(uploadIntent);
            }
        }
    }

    @Override
    public void setup(SettingsUploadBase settingsUpload) {
        this.settingsUpload = settingsUpload;
    }

    @Override
    public void onViewCreated() {
        if (!isBoundUploadHandler()) {
            boolean bindService = settingsUpload.getContext().bindService(uploadIntent, connection, 0);
            Log.d(TAG, String.format("onViewCreated. Status: %s. Message: Attempting to bind the upload service.", bindService));
        }
    }

    @Override
    public void onViewDestroy() {
        if (isBoundUploadHandler()) {
            settingsUpload.getContext().unbindService(connection);
            connection.setBounded(false);
            Log.d(TAG, "onViewDestroy. Status: true. Message: Attempting to unbind the upload service.");
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void stop() {
        if (connection == null) {
            return;
        }
        ServiceUpload service = connection.getService();
        if (service == null) {
            return;
        }
        if (connection.isBounded()) {
            settingsUpload.getContext().unbindService(connection);
        }
        if (listener != null) {
            listener.onUploadStoped();
        }
        connection.removeListener();
        connection = null;
        settingsUpload = null;
        service.stopForeground(true);
        service.stopSelf();
        Log.d(TAG, "UploadManagerImpl. Status: stop. Message: Stopping the service.");
    }

    @Override
    public boolean isInProgress() {
        return connection != null;
    }

    @Override
    public void addUploadStatusListener(UploadStatus uploadStatus) {
        if (connection != null) {
            listener = uploadStatus;
            connection.addListener(listener);
        }
    }

    @Override
    public void removeUploadStatusListener() {
        listener = null;
        if (connection != null) {
            connection.removeListener();
        }
    }

    /**
     * @return {@code true} if the upload service is set, {@code false} otherwise.
     */
    private boolean isBoundUploadHandler() {
        return connection != null
                && connection.isBounded()
                && connection.getService() != null;
    }
}
