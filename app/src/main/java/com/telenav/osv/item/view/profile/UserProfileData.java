package com.telenav.osv.item.view.profile;

/**
 * Data presented on the UserProfile screen extended appbar
 * Created by kalmanb on 8/30/17.
 */
public class UserProfileData extends ProfileData {

    private int score;

    private int rank;

    private int level;

    private int xpTarget;

    private int xpProgress;

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXpTarget() {
        return xpTarget;
    }

    public void setXpTarget(int xpTarget) {
        this.xpTarget = xpTarget;
    }

    public int getXpProgress() {
        return xpProgress;
    }

    public void setXpProgress(int xpProgress) {
        this.xpProgress = xpProgress;
    }
}
