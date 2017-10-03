package com.telenav.osv.item.network;

/**
 * Holder class for user profile data
 * Created by kalmanb on 7/5/17.
 */
public class UserData extends ApiResponse {

  public static final int TYPE_UNKNOWN = -1;

  public static final int TYPE_CONTRIBUTOR = 0;

  public static final int TYPE_QA = 1;

  public static final int TYPE_DEDICATED = 2;

  public static final int TYPE_BYOD = 3;

  public static final int TYPE_BAU = 4;

  private String userId;

  private String userName;

  private int userType;

  private double totalTracks;

  private double totalPhotos;

  private double totalDistance;

  private double obdDistance;

  private int overallRank;

  private int weeklyRank;

  private int level;

  private int levelTarget;

  private int levelProgress;

  private String levelName;

  private int totalPoints;

  private String regionCode;

  private int regionPoints;

  private int regionRank;

  private String displayName;

  public static boolean isDriver(int type) {
    return type == TYPE_BYOD ||
        type == TYPE_BAU ||
        type == TYPE_DEDICATED;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public int getUserType() {
    return userType;
  }

  public void setUserType(int userType) {
    this.userType = userType;
  }

  public double getTotalTracks() {
    return totalTracks;
  }

  public void setTotalTracks(double totalTracks) {
    this.totalTracks = totalTracks;
  }

  public double getTotalPhotos() {
    return totalPhotos;
  }

  public void setTotalPhotos(double totalPhotos) {
    this.totalPhotos = totalPhotos;
  }

  public double getTotalDistance() {
    return totalDistance;
  }

  public void setTotalDistance(double totalDistance) {
    this.totalDistance = totalDistance;
  }

  public double getTotalObdDistance() {
    return obdDistance;
  }

  public void setObdDistance(double obdDistance) {
    this.obdDistance = obdDistance;
  }

  public int getOverallRank() {
    return overallRank;
  }

  public void setOverallRank(int overallRank) {
    this.overallRank = overallRank;
  }

  public int getWeeklyRank() {
    return weeklyRank;
  }

  public void setWeeklyRank(int weeklyRank) {
    this.weeklyRank = weeklyRank;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getLevelTarget() {
    return levelTarget;
  }

  public void setLevelTarget(int levelTarget) {
    this.levelTarget = levelTarget;
  }

  public int getLevelProgress() {
    return levelProgress;
  }

  public void setLevelProgress(int levelProgress) {
    this.levelProgress = levelProgress;
  }

  public String getLevelName() {
    return levelName;
  }

  public void setLevelName(String levelName) {
    this.levelName = levelName;
  }

  public int getTotalPoints() {
    return totalPoints;
  }

  public void setTotalPoints(int totalPoints) {
    this.totalPoints = totalPoints;
  }

  public String getRegionCode() {
    return regionCode;
  }

  public void setRegionCode(String regionCode) {
    this.regionCode = regionCode;
  }

  public int getRegionPoints() {
    return regionPoints;
  }

  public void setRegionPoints(int regionPoints) {
    this.regionPoints = regionPoints;
  }

  public int getRegionRank() {
    return regionRank;
  }

  public void setRegionRank(int regionRank) {
    this.regionRank = regionRank;
  }

  @Override
  public String toString() {
    return "UserData{" + "userId='" + userId + '\'' + ", userName='" + userName + '\'' + ", userType=" + userType + ", totalTracks=" +
        totalTracks + ", totalPhotos=" + totalPhotos + ", totalDistance=" + totalDistance + ", obdDistance=" + obdDistance +
        ", overallRank=" + overallRank + ", weeklyRank=" + weeklyRank + ", level=" + level + ", levelTarget=" + levelTarget +
        ", levelProgress=" + levelProgress + ", levelName='" + levelName + '\'' + ", totalPoints=" + totalPoints + ", regionCode='" +
        regionCode + '\'' + ", regionPoints=" + regionPoints + ", regionRank=" + regionRank + ", displayName='" + displayName + '\'' + '}';
  }
}
