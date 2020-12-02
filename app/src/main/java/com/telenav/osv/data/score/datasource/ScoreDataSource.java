package com.telenav.osv.data.score.datasource;

import java.util.Map;
import com.telenav.osv.data.score.model.ScoreHistory;
import io.reactivex.Maybe;

/**
 * Data Source interface for the {@code Score} details.
 * The interface provides all the available operations for the score local data source.
 * Created by cameliao on 2/9/18.
 */
public interface ScoreDataSource {

    /**
     * Inserts a new score into the local storage. If the entry already exists the score will be updated.
     * The score is store as being one to one with the number of photos.
     * E.g. if the number of taken photos is 10 then the score will be 10.
     * The final score is format by multiplying the score with the coverage multiplier.
     * E.g. if the number of taken photos is 10 and the multiplier is 4 then the total number of points is 40.
     * @param scoreHistory the score history to be persisted for a sequence.
     * @param sequenceID the identifier for which the score corresponds too.
     */
    boolean insertScore(ScoreHistory scoreHistory, String sequenceID);

    /**
     * @param scoreHistoryId the identifier for the {@code ScoreHistory}.
     * @param obdPhotoCount the new obd photo count for the {@code ScoreHistory} to be updated.
     * @return {@code true} if the obd photo count has been updated successful, {@code false} otherwise.
     */
    boolean updateObdPhotoCount(String scoreHistoryId, int obdPhotoCount);

    /**
     * @param scoreHistoryId the identifier for the {@code ScoreHistory}.
     * @param photoCount the new photo count for the {@code ScoreHistory} to be updated.
     * @return {@code true} if the photo count has been updated successful, {@code false} otherwise.
     */
    boolean updatePhotoCount(String scoreHistoryId, int photoCount);

    /**
     * Returns a map containing the score history for each added coverage.
     * The coverage is the unique key in the map.
     * @param sequenceId the local sequence id to retrieve all the existing coverage with the score history for the that specific sequence.
     * @return a map with the coverage as a key and the score history for each coverage.
     */
    Maybe<Map<Integer, ScoreHistory>> getScoreHistory(String sequenceId);

    /**
     * @param scoreHistoryMap a map with the coverage as a key and the score history for each coverage.
     * @return the value representing the score. This will be calculated based on coverage and obd status.
     */
    double getScore(Map<Integer, ScoreHistory> scoreHistoryMap);
}
