package com.telenav.osv.data.user.model.details.gamification;

/**
 * Class that holds rank related information such as:
 * <ul>
 * <li>{@link #weekly}</li>
 * <li>{@link #overall}</li>
 * </ul>
 * @author horatiuf
 */

public class GamificationRank {

    /**
     * Weekly rank for the current user account.
     */
    private int weekly;

    /**
     * Overall rank for the current user account.
     */
    private int overall;

    /**
     * Default constructor for the current class.
     * @param weekly {@code int} representing {@link #weekly}.
     * @param overall {@code int} representing {@link #overall}.
     */
    public GamificationRank(int weekly, int overall) {
        this.weekly = weekly;
        this.overall = overall;
    }

    /**
     * @return {@code int} representing {@link #weekly}.
     */
    public int getWeekly() {
        return weekly;
    }

    /**
     * @return {@code int} representing {@link #overall}.
     */
    public int getOverall() {
        return overall;
    }
}
