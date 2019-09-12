package com.telenav.osv.upload.status;

import android.content.ComponentName;
import android.os.IBinder;
import com.telenav.osv.common.OSVServiceConnection;
import com.telenav.osv.upload.ServiceUploadImpl;
import com.telenav.osv.utils.Log;

public class UploadServiceConnection extends OSVServiceConnection<ServiceUpload, ServiceUploadImpl.UploadServiceBinder> {

    private final String TAG = UploadServiceConnection.class.getSimpleName();

    private UploadStatus listener;

    public UploadServiceConnection(UploadStatus listener) {
        this.listener = listener;
    }

    public UploadServiceConnection() {

    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        super.onServiceConnected(componentName, service);
        Log.d(TAG, "service connected.");
        if (listener != null) {
            addListener(listener);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        super.onServiceDisconnected(componentName);
        Log.d(TAG, "service disconnected.");
        removeListener();
    }

    public void addListener(UploadStatus uploadStatus) {
        this.listener = uploadStatus;
        if (service != null) {
            service.setListener(uploadStatus);
        }
    }

    public void removeListener() {
        this.listener = null;
        if (service != null) {
            service.setListener(null);
        }
    }
}
