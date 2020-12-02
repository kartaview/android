package com.telenav.osv.item;

/**
 * Model class for user's leader board data.
 * Created by Kalman on 22/11/2016.
 */
public class LeaderboardData {

    /**
     * The username of the user
     */
    private final String username;

    /**
     * The name of the user
     */
    private final String name;

    /**
     * The position of the user in the leader board
     */
    private final int rank;

    /**
     * The points gained by the user
     */
    private final int points;

    /**
     * The code of the country in which the user is located
     */
    private final String countryCode;

    /**
     * Default constructor for the current class.
     * @param username the unique identifier of the user
     * @param name the full name o the user
     * @param countryCode the code of the country in which the user is located
     * @param rank the position of the user in the leader board
     * @param points the points gained by the user in the country
     */
    public LeaderboardData(String username, String name, String countryCode, int rank, int points) {
        this.username = username;
        this.name = name;
        this.countryCode = countryCode;
        this.rank = rank;
        this.points = points;
    }

    /**
     * @return {@code String} representing the {@link #countryCode}
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @return {@code String} representing {@link #name}
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@code String} representing {@link #username}
     */
    public String getUsername() {return username;}

    /**
     * @return {@code int} representing {@link #rank}
     */
    public int getRank() {
        return rank;
    }

    /**
     * @return {@code int} representing {@link #points}
     */
    public int getPoints() {
        return points;
    }
}
