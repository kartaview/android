package com.telenav.osv.data;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public interface ProfilePreferences extends AccountPreferences {

    double getDistance();

    void setDistance(double value);

    double getAcceptedDistance();

    void setAcceptedDistance(double value);

    double getRejectedDistance();

    void setRejectedDistance(double value);

    double getObdDistance();

    void setObdDistance(double value);

    int getTracksCount();

    void setTracksCount(int value);

    int getPhotosCount();

    void setPhotosCount(int value);
}
