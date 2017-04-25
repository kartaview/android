package com.telenav.osv.item;

/**
 * Created by Kalman on 22/11/2016.
 */

public class UserData {
    private final String name;

    private final int rank;

    private final int points;

    public UserData(String name, int rank, int points) {
        this.name = name;
        this.rank = rank;
        this.points = points;
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
