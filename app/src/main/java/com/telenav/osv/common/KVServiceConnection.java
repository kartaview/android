package com.telenav.osv.common;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.telenav.osv.common.service.KVBaseBinder;
import com.telenav.osv.common.service.KVBaseService;
import com.telenav.osv.utils.Log;

/**
 * Connection class for upload service. This will by default override:
 * <ul>
 * <li>{@link ServiceConnection#onServiceConnected(ComponentName, IBinder)} - persists the bounded service and sets the bound flag to true.</li>
 * <li>{@link ServiceConnection#onServiceDisconnected(ComponentName)} - removed the persisted bounded service and sets the bound flag to false</li>
 * </ul>
 */
public abstract class KVServiceConnection<T extends KVBaseService, G extends KVBaseBinder> implements ServiceConnection {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public final String TAG = KVServiceConnection.class.getSimpleName();

    /**
     * Instance to the service.
     */
    protected T service;

    /**
     * The {@code boolean} status for bounded, can be modified outside of the scope of the {@link ServiceConnection} callbacks since they may not be always called.
     */
    private boolean bounded;

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        Log.d(TAG, String.format("onServiceConnected. Component name: %s", componentName.getClassName()));
        @SuppressWarnings("unchecked")
        G binder = (G) service;
        @SuppressWarnings("unchecked")
        T newService = (T) binder.getService();
        this.service = newService;
        bounded = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d(TAG, String.format("onServiceDisconnected. Component name: %s", componentName.getClassName()));
        service = null;
        bounded = false;
    }

    @Override
    public void onBindingDied(ComponentName name) {
        bounded = false;
    }

    /**
     * @return {@code UploadHandlerService} representing the upload handler service bounded.
     */
    public T getService() {
        return service;
    }

    /**
     * @return {@code true} when the server is bounded, {@code false} otherwise.
     */
    public boolean isBounded() {
        return bounded;
    }

    /**
     * Required to be exposed since {@link #onServiceDisconnected(ComponentName)} might not always be called.
     * @param bounded the value for the bounded server.
     */
    public void setBounded(boolean bounded) {
        this.bounded = bounded;
    }
}
