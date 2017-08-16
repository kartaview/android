package com.telenav.osv.item;

import android.support.annotation.NonNull;
import com.skobbler.ngx.SKCoordinate;

/**
 * segment object holding matcher info and coverage
 * Created by Kalman on 28/04/2017.
 */

public class Segment {

    private static final String TAG = "Segment";

    private final SKCoordinate reference;

    private double deltaBearing;

    private float score;

    private SKCoordinate start;

    private SKCoordinate end;

    private double distance;

    private Polyline polyline;

    public Segment(double distance, SKCoordinate referencePoint, @NonNull Polyline polyline, SKCoordinate start, SKCoordinate end, double deltaBearing, float score) {
        this.distance = distance;
        this.polyline = polyline;
        this.reference = referencePoint;
        this.start = start;
        this.end = end;
        this.deltaBearing = deltaBearing;
        this.score = score;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public double getDistance() {
        return distance;
    }

    public SKCoordinate getStart() {
        return start;
    }

    public SKCoordinate getEnd() {
        return end;
    }

    public SKCoordinate getReference() {
        return reference;
    }

    public double getDeltaBearing() {
        return deltaBearing;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return TAG + " from " + polyline.getIdentifier() + ", distance " + (int) (distance * 110000) + ", deltaBearing " + deltaBearing + ", score " + score;
    }
}