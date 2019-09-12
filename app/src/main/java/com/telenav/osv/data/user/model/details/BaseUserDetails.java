package com.telenav.osv.data.user.model.details;

import androidx.annotation.IntDef;

/**
 * User details class which represents the base details class.
 * <p>For concrete class identification abstract method {@link #getType()} can be used.</p>
 * @author horatiuf
 * @see UserDetailsTypes
 */

public abstract class BaseUserDetails {

    /**
     * The total number of photos which the account uploaded.
     */
    protected int photosCount;

    /**
     * The total number of tracks that the user account uploaded.
     */
    protected int tracksCount;

    /**
     * The total distance with odb connected.
     */
    protected double obdDistance;

    /**
     * The total distance which the current user has tracked.
     */
    protected double distance;

    /**
     * Default constructor for the current class.
     * @param photosCount {@code int} representing {@link #photosCount}.
     * @param tracksCount {@code int} representing {@link #tracksCount}.
     * @param obdDistance {@code double} representing {@link #obdDistance}.
     * @param distance {@code double} representing {@link #distance}.
     */
    public BaseUserDetails(int photosCount, int tracksCount, double obdDistance, double distance) {
        this.photosCount = photosCount;
        this.tracksCount = tracksCount;
        this.obdDistance = obdDistance;
        this.distance = distance;
    }

    /**
     * @return {@code int} representing {@link #photosCount}.
     */
    public int getPhotosCount() {
        return photosCount;
    }

    /**
     * @return {@code int} representing {@link #tracksCount}.
     */
    public int getTracksCount() {
        return tracksCount;
    }

    /**
     * @return {@code double} representing {@link #obdDistance}.
     */
    public double getObdDistance() {
        return obdDistance;
    }

    /**
     * @return {@code double} representing {@link #distance}.
     */
    public double getDistance() {
        return distance;
    }

    /**
     * The type of the details. It must be a value from {@link UserDetailsTypes}.
     */
    @UserDetailsTypes
    public abstract int getType();

    /**
     * Interface representing the type for the current details. The values can be:
     * <ul>
     * <li>{@link #UNKNOWN}</li>
     * <li>{@link #GAMIFICATION}</li>
     * <li>{@link #DRIVER}</li>
     * </ul>
     */
    @IntDef
    public @interface UserDetailsTypes {

        /**
         * Account which has unknown details type.
         */
        int UNKNOWN = -1;

        /**
         * Account with gamification details.
         */
        int GAMIFICATION = 0;

        /**
         * Account with driver details.
         */
        int DRIVER = 1;
    }
}
