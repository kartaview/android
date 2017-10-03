package com.telenav.osv.data;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public interface UIPreferences {

    boolean shouldShowTapToShoot();

    void setShouldShowTapToShoot(boolean value);

    boolean shouldShowBackgroundHint();

    void setShouldShowBackgroundHint(boolean value);

    boolean shouldShowRecordingSummary();

    void setShouldShowRecordingSummary(boolean value);

    boolean shouldShowClearRecentsWarning();

    void setShouldShowClearRecentsWarning(boolean value);

    boolean shouldShowWalkthrough();

    void setShouldShowWalkthrough(boolean value);

    boolean shouldShowSafeModeMessage();

    void setShouldShowSafeModeMessage(boolean value);

    boolean shouldShowDeleteConfirmation();

    void setShouldShowDeleteConfirmation(boolean value);

    boolean shouldShowTapOnMap();

    void setShouldShowTapOnMap(boolean value);

    boolean shouldShowDriverDialog();

    void setShouldShowDriverDialog(boolean value);
}
