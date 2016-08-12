package com.telenav.osv.external.model;

import android.graphics.Bitmap;

/**
 * Photo object storage class
 */
public class Photo {

    private Double mOrientationAngle = 0d;

    private Double mElevationAngle = 0d;

    private Double mHorizontalAngle = 0d;

    private Bitmap mPhoto;

    private Photo() {
    }

    /**
     * Constructor
     * @param photo Photo object
     */
    public Photo(Bitmap photo) {
        this(photo, null, null, null);
    }

    /**
     * Constructor
     * @param photo Photo object
     * @param orientationAngle Orientation angle
     * @param elevationAngle Elevation angle
     * @param horizontalAngle Horizontal angle
     */
    public Photo(Bitmap photo, Double orientationAngle, Double elevationAngle, Double horizontalAngle) {
        this();

        mOrientationAngle = orientationAngle;
        mElevationAngle = elevationAngle;
        mHorizontalAngle = horizontalAngle;

        mPhoto = photo;
    }

    /**
     * Acquires the orientation angle
     * @return Orientation angle
     */
    public Double getOrientationAngle() {
        return mOrientationAngle;
    }

    /**
     * Acquires the elevation angle
     * @return Elevation angle
     */
    public Double getElevetionAngle() {
        return mElevationAngle;
    }

    /**
     * Acquires the horizontal angle
     * @return Horizontal angle
     */
    public Double getHorizontalAngle() {
        return mHorizontalAngle;
    }

    /**
     * Acquires the photo object
     * @return Photo object
     */
    public Bitmap getPhoto() {
        return mPhoto;
    }

    /**
     * Updates the photo object
     * @param drawable Photo object
     */
    public void updatePhoto(Bitmap drawable) {
        mPhoto = drawable;
    }

}