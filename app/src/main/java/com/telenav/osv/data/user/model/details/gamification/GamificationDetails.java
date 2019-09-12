package com.telenav.osv.data.user.model.details.gamification;

import com.telenav.osv.data.user.model.details.BaseUserDetails;

/**
 * The gamification type of account represented by {@link UserDetailsTypes#GAMIFICATION} denoted by {@link #getType()} method.
 * <p>
 * The class hold information such as:
 * <ul>
 * <li>{@link #level}</li>
 * <li>{@link #rank}</li>
 * <li>{@link #points}</li>
 * </ul>
 * This information is besides the one from the {@link BaseUserDetails} parent.
 * @author horatiuf
 * @see BaseUserDetails
 */

public class GamificationDetails extends BaseUserDetails {

    /**
     * Level related information for the current user account.
     * @see GamificationLevel
     */
    private GamificationLevel level;

    /**
     * The total number of points achieved on the account.
     */
    private int points;

    /**
     * Rank related information for the current user account.
     * @see GamificationRank
     */
    private GamificationRank rank;

    /**
     * Default constructor for the current class.
     * @param photosCount {@code int} representing {@link #photosCount}.
     * @param tracksCount {@code int} representing {@link #tracksCount}.
     * @param obdDistance {@code double} representing {@link #obdDistance}.
     * @param distance {@code double} representing {@link #distance}.
     * @param points {@code int} representing {@link #points}.
     * @param level {@code GamificationLevel} representing {@link #level}.
     * @param rank {@code GamificationRank} representing {@link #rank}.
     */
    public GamificationDetails(int photosCount, int tracksCount, double obdDistance, double distance, int points, GamificationLevel level, GamificationRank rank) {
        super(photosCount, tracksCount, obdDistance, distance);
        this.level = level;
        this.points = points;
        this.rank = rank;
    }

    @Override
    public int getType() {
        return UserDetailsTypes.GAMIFICATION;
    }

    /**
     * @return {@code GamificationLevel} representing {@link #level}.
     */
    public GamificationLevel getLevel() {
        return level;
    }

    /**
     * @return {@code int} representing {@link #points}.
     */
    public int getPoints() {
        return points;
    }

    /**
     * @return {@code GamificationRank} representing {@link #rank}.
     */
    public GamificationRank getRank() {
        return rank;
    }
}
