package com.telenav.osv.data.sequence.model.details.reward;

import java.util.Map;
import javax.annotation.Nullable;
import android.annotation.SuppressLint;
import com.telenav.osv.data.score.model.ScoreHistory;

/**
 * Class used to hold information for the points system in a local sequence.
 * There is required a score history therefore this field is used. The value can be null when the points are not enabled.
 * @author horatiuf
 */
public class SequenceDetailsRewardPoints extends SequenceDetailsRewardBase {

    /**
     * Used for the points reward system in order to have a history of points based on their coverage.
     */
    @SuppressLint("UseSparseArrays")
    @Nullable
    private Map<Integer, ScoreHistory> scoreHistory;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetailsRewardPoints(double value, String unit, @Nullable Map<Integer, ScoreHistory> scoreHistory) {
        super(value, unit);
        this.scoreHistory = scoreHistory;
    }

    @Nullable
    public Map<Integer, ScoreHistory> getScoreHistory() {
        return scoreHistory;
    }
}
