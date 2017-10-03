package com.telenav.osv.item;

/**
 * Created by Kalman on 22/11/2016.
 */

public class LeaderboardData {

    private final String name;

    private final int rank;

    private final int points;

    private final String countryCode;

    public LeaderboardData(String name, String countryCode, int rank, int points) {
        this.name = name;
        this.countryCode = countryCode;
        this.rank = rank;
        this.points = points;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getName() {
        return name;
    }

    public int getRank() {
        return rank;
    }

    public int getPoints() {
        return points;
    }
}
