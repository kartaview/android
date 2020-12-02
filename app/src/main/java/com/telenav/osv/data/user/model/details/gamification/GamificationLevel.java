package com.telenav.osv.data.user.model.details.gamification;

/**
 * Class representing information related to level achieved by the user using gamification feature.
 * @author horatiuf
 */

public class GamificationLevel {

    /**
     * Current level number.
     */
    private int level;

    /**
     * The next level which is targeted.
     */
    private int target;

    /**
     * The progress of the current level.
     */
    private int progress;

    /**
     * The name of the level.
     */
    private String name;

    /**
     * Default constructor for the current class.
     * @param level {@code int} representing {@link #level}.
     * @param target {@code int} representing {@link #target}.
     * @param progress {@code int} representing {@link #progress}.
     * @param name {@code String} representing {@link #name}.
     */
    public GamificationLevel(int level, int target, int progress, String name) {
        this.level = level;
        this.target = target;
        this.progress = progress;
        this.name = name;
    }

    /**
     * @return {@code int} representing {@link #level}.
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return {@code int} representing {@link #target}.
     */
    public int getTarget() {
        return target;
    }

    /**
     * @return {@code int} representing {@link #progress}.
     */
    public int getProgress() {
        return progress;
    }

    /**
     * @return {@code String} representing {@link #name}.
     */
    public String getName() {
        return name;
    }
}
