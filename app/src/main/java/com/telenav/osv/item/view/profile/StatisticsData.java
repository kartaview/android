package com.telenav.osv.item.view.profile;

/**
 * data holder for statistics displayed on profile screen
 * Created by kalmanb on 9/29/17.
 */
public class StatisticsData {

  private double distance;

  private double acceptedDistance;

  private double rejectedDistance;

  private double obdDistance;

  private int totalPhotos;

  private int totalTracks;

  public double getDistance() {
    return distance;
  }

  public void setDistance(double distance) {
    this.distance = distance;
  }

  public double getAcceptedDistance() {
    return acceptedDistance;
  }

  public void setAcceptedDistance(double acceptedDistance) {
    this.acceptedDistance = acceptedDistance;
  }

  public double getRejectedDistance() {
    return rejectedDistance;
  }

  public void setRejectedDistance(double rejectedDistance) {
    this.rejectedDistance = rejectedDistance;
  }

  public double getObdDistance() {
    return obdDistance;
  }

  public void setObdDistance(double obdDistance) {
    this.obdDistance = obdDistance;
  }

  public int getTotalPhotos() {
    return totalPhotos;
  }

  public void setTotalPhotos(int totalPhotos) {
    this.totalPhotos = totalPhotos;
  }

  public int getTotalTracks() {
    return totalTracks;
  }

  public void setTotalTracks(int totalTracks) {
    this.totalTracks = totalTracks;
  }

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
}
