package com.telenav.osv.obd.connected;

import android.os.Bundle;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;
import com.telenav.osv.obd.manager.ObdManager;


/**
 * The contract between the view and the presenter for the obd connected. Each concrete implementation will adhere either to {@code ObdDetailsView} if it is a view element or
 * {@code ObdDetailsPresenter} if it holds only business functionality.
 * @author horatiuf
 */
public interface ObdConnectedContract {

    /**
     * The obd connected view which holds all available ui related functionality required by the presenter.
     * @author horatiuf
     */
    interface ObdConnectedView extends BaseView<ObdConnectedPresenter> {

        /**
         * Updates the duration display.
         * @param hours the hours which have passed since the recording have started.
         * @param minutes the minutes which have passed since the recording have started.
         */
        void updateDuration(int hours, int minutes);

        /**
         * Displays the speed value for the user.
         * @param speed the speed value.
         * @param metric the metric symbol.
         */
        void updateSpeed(String speed, String metric);

        /**
         * Signals the view to navigate to obd connect due to either obd timeout/disconnect.
         */
        void goToObdConnect();

        /**
         * Displays the total number of images and the sequence distance for the current recording.
         */
        void updateImageDetails(int imageCount, int distance);

        /***
         * Displays the score for the current recording..
         * @param score the score value.
         */
        void updateScore(long score);
    }

    /**
     * The obd connected presenter which holds all available business functionality required by the view.
     * @author horatiuf
     */
    interface ObdConnectedPresenter extends BasePresenter, ObdManager.ObdConnectionListener {

        /**
         * @return {@code false} if the account is either:
         * <ul>
         * <li>{@code WIFI} obd is connected since there is no way to determine the score as such</li>
         * </ul>Otherwise it will return {@code true}.
         */
        boolean isScoreEnabled();

        /**
         * Dismiss all resource of the presenter. It should be called or either on view {@code onStop}, or {@code onDestroyView} lifecycle callbacks.
         */
        void dispose();

        /**
         * Stops the obd collecting from {@code DataCollector}.
         */
        void stopCollecting();

        /**
         * @return {@code true} if the imperial unit metric is enabled, {@code false} otherwise.
         */
        boolean isImperial();

        /**
         * Initialise the values for details such as distance, score and points.
         * @param arguments the {@code Bundle} representing the arguments passed from the fragment.
         */
        void initDetails(Bundle arguments);
    }
}
