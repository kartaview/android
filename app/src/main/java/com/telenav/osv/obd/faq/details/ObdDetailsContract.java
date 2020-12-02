package com.telenav.osv.obd.faq.details;

import android.content.Context;
import com.telenav.osv.common.model.base.BasePresenter;
import com.telenav.osv.common.model.base.BaseView;

/**
 * The contract between the view and the presenter for the obd details. Each concrete implementation will adhere either to {@code ObdDetailsView} if view element or {@code
 * ObdDetailsPresenter} if it holds business functionality.
 * @author horatiuf
 */
public interface ObdDetailsContract {

    /**
     * The obd details view which holds all available view functionality.
     * @author horatiuf
     */
    interface ObdDetailsView extends BaseView<ObdDetailsPresenter> {

    }

    /**
     * The obd details presenter which holds functionality which is common for all the details views, i.e. {@link #openRecommendation(String, Context)}.
     * @author horatiuf
     */
    interface ObdDetailsPresenter extends BasePresenter {

        /**
         * Opens the recommendation in a new browser by the users choice.
         * @param urlIdentifier The url for which the browser should open. The value must be one of {@link ObdDetailsRecommendations}.
         * @param context required to start the activity representing the default browser recommendation.
         */
        void openRecommendation(@ObdDetailsRecommendations String urlIdentifier, Context context);

        /**
         * @return {@code true} if the user settings is imperial, {@code false} otherwise.
         */
        boolean isImperial();
    }
}
