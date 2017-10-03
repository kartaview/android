package com.telenav.osv.item.view.tracklist;

import android.text.SpannableString;

/**
 * Data presented on the tracks list header in the Profile screen
 * Created by kalmanb on 8/30/17.
 */
public class StatsData {

    private SpannableString distance;

    private SpannableString acceptedDistance;

    private SpannableString rejectedDistance;

    private SpannableString obdDistance;

    private SpannableString totalPhotos;

    private SpannableString totalTracks;

    @Override
    public String toString() {
        return "TrackCollectionStats{" +
                "acceptedDistance=" + acceptedDistance +
                ", rejectedDistance=" + rejectedDistance +
                ", obdDistance=" + obdDistance +
                ", totalPhotos=" + totalPhotos +
                ", totalTracks=" + totalTracks +
                '}';
    }

    public SpannableString getDistance() {
        return distance;
    }

    public void setDistance(SpannableString distance) {
        this.distance = distance;
    }

    public SpannableString getAcceptedDistance() {
        return acceptedDistance;
    }

    public void setAcceptedDistance(SpannableString acceptedDistance) {
        this.acceptedDistance = acceptedDistance;
    }

    public SpannableString getRejectedDistance() {
        return rejectedDistance;
    }

    public void setRejectedDistance(SpannableString rejectedDistance) {
        this.rejectedDistance = rejectedDistance;
    }

    public SpannableString getObdDistance() {
        return obdDistance;
    }

    public void setObdDistance(SpannableString obdDistance) {
        this.obdDistance = obdDistance;
    }

    public SpannableString getTotalPhotos() {
        return totalPhotos;
    }

    public void setTotalPhotos(SpannableString totalPhotos) {
        this.totalPhotos = totalPhotos;
    }

    public SpannableString getTotalTracks() {
        return totalTracks;
    }

    public void setTotalTracks(SpannableString totalTracks) {
        this.totalTracks = totalTracks;
    }
}
