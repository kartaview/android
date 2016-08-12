package com.telenav.osv.listener;

/**
 * Created by Kalman on 11/18/15.
 */
public interface FocusListener {
    /**
     * This method is called when the focus operation starts.
     * @param smallAdjust If this parameter is set to true, the focus is being done because of
     * continuous focus mode and thus only make a small adjustment
     */
    void onFocusStart(boolean smallAdjust);

    /**
     * This method is called when the focus operation ends
     * @param smallAdjust If this parameter is set to true, the focus is being done because of
     * continuous focus mode and made only a small adjustment
     * @param success If the focus operation was successful. Note that if smallAdjust is true,
     * this parameter will always be false.
     */
    void onFocusReturns(boolean smallAdjust, boolean success);
}
