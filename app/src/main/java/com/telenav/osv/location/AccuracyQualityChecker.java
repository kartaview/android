package com.telenav.osv.location;

import com.telenav.osv.utils.Log;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * Accuracy Quality checker for the location component.
 * Use {@link #getAccuracyType()} to subscribe for accuracy updates.
 */
class AccuracyQualityChecker {

    private static final String TAG = AccuracyQualityChecker.class.getSimpleName();

    /**
     * Default value for location accuracy type until a valid location is received.
     */
    private static final int ACCURACY_TYPE_NOT_SET = -1;

    /**
     * The last known type for location accuracy.
     */
    @AccuracyType
    private int lastKnownAccuracyType = ACCURACY_TYPE_NOT_SET;

    /**
     * {@code Subject} used to push the accuracy updates.
     */
    private PublishSubject<Integer> subjectAccuracyType;

    /**
     * Method used for subscribing to listen for accuracy events.
     * @return an {@code Observable} which will emit the accuracy type when changes.
     */
    Observable<Integer> getAccuracyType() {
        if (subjectAccuracyType == null) {
            subjectAccuracyType = PublishSubject.create();
        }
        return subjectAccuracyType.toSerialized().hide();
    }

    /**
     * Method used to update the location accuracy for each location received from sensors.
     * @param accuracy the value of location accuracy.
     */
    void onAccuracyChanged(float accuracy) {
        int accuracyType = getAccuracyType(accuracy);
        if (accuracyType != lastKnownAccuracyType) {
            Log.d(TAG, "onAccuracyChanged: changed to " + accuracyType);
            lastKnownAccuracyType = accuracyType;
            if (subjectAccuracyType != null) {
                subjectAccuracyType.onNext(lastKnownAccuracyType);
            }
        }
    }

    /**
     * Resets teh location accuracy type to default.
     */
    void onAccuracyReset() {
        lastKnownAccuracyType = ACCURACY_TYPE_NOT_SET;
    }

    /**
     * Checks the location accuracy value in order to select a proper accuracy type from {@link AccuracyType}.
     * @param accuracy the value for the current location accuracy.
     * @return a value form {@link AccuracyType} representing the accuracy type for the given value.
     */
    @AccuracyType
    private int getAccuracyType(float accuracy) {
        if (accuracy <= AccuracyType.ACCURACY_GOOD) {
            return AccuracyType.ACCURACY_GOOD;
        } else if (accuracy <= AccuracyType.ACCURACY_MEDIUM) {
            return AccuracyType.ACCURACY_MEDIUM;
        } else {
            return AccuracyType.ACCURACY_BAD;
        }
    }
}