package com.telenav.osv.recorder;

/**
 * The class define a custom {@link Throwable} containing all the messages of the errors.
 * Created by cameliao on 1/31/18.
 */

public class Errors extends Throwable {

    public static final String ERROR_OBD_ALREADY_CONNECTED = "OBD already connected.";

    public static final String ERROR_NO_OBD_CONNECTION = "There is no OBD connection registered.";


    public Errors(String message) {
        super(message);
    }
}
