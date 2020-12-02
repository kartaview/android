package com.telenav.osv.common.listener;

/**
 * Generic listener for callback responses.
 * @author horatiuf
 */
public interface GenericListener {
    /**
     * The success callback on the listener.
     */
    void onSuccess();

    /**
     * The error callback of the listener;
     */
    void onError();
}
