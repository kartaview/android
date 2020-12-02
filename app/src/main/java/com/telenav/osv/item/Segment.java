package com.telenav.osv.item;

import androidx.annotation.NonNull;

import com.telenav.osv.common.model.KVLatLng;

/**
 * segment object holding matcher info and coverage
 * Created by Kalman on 28/04/2017.
 */

public class Segment {

    private static final String TAG = "Segment";

    private final KVLatLng reference;

    private double deltaBearing;

    private float score;

    private KVLatLng start;

    private KVLatLng end;

    private double distance;

    private Polyline polyline;

    public Segment(double distance, KVLatLng referencePoint, @NonNull Polyline polyline, KVLatLng start, KVLatLng end,
                   double deltaBearing, float score) {
        this.distance = distance;
        this.polyline = polyline;
        this.reference = referencePoint;
        this.start = start;
        this.end = end;
        this.deltaBearing = deltaBearing;
        this.score = score;
    }

    @Override
    public String toString() {
        return TAG + " from " + polyline.getIdentifier() + ", distance " + (int) (distance * 110000) + ", deltaBearing " + deltaBearing +
                ", score " + score;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public double getDistance() {
        return distance;
    }

    public KVLatLng getStart() {
        return start;
    }

    public KVLatLng getEnd() {
        return end;
    }

    public KVLatLng getReference() {
        return reference;
    }

    public double getDeltaBearing() {
        return deltaBearing;
    }

    public float getScore() {
        return score;
    }
}