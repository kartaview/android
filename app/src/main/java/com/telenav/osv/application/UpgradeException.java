package com.telenav.osv.application;

/**
 * Created by Kalman on 22/04/16.
 */
public class UpgradeException extends Throwable {
    public UpgradeException(String osv, String s) {
        super(osv + " " + s);
    }
}
