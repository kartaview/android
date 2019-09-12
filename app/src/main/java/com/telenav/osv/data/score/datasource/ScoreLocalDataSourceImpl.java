package com.telenav.osv.data.score.datasource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import android.content.Context;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.database.DataConverter;
import com.telenav.osv.data.score.database.dao.ScoreDao;
import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.reactivex.Maybe;
import io.reactivex.Observable;

/**
 * The local data source for the {@code Score} which handles the logic for storing and retrieving the score details from the storage.
 * Created by cameliao on 2/9/18.
 */

public class ScoreLocalDataSourceImpl implements ScoreDataSource {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ScoreLocalDataSourceImpl.class.getSimpleName();

    /**
     * Multiplier used for obd score.
     */
    private final static int OBD_MULTIPLIER = 2;

    /**
     * Unknown value of coverage
     */
    private static final int UNKNOWN_COVERAGE_VALUE = -1;

    /**
     * The instance of the current class
     */
    private static ScoreLocalDataSourceImpl instance;

    /**
     * The score Dao which holds all
     */
    private ScoreDao scoreDao;

    /**
     * Private constructor for the current class to hide the initialisation from external sources.
     * @param context the context of the application to instantiate the scoreDao
     */
    private ScoreLocalDataSourceImpl(Context context) {
        scoreDao = Injection.provideOSCDatabase(context).scoreDao();
    }

    /**
     * @param context the context of the application
     * @return a single instance of the {@link ScoreLocalDataSourceImpl} representing {@link #instance}, which is the implementation of the {@link ScoreDataSource}.
     * If the {@link #instance} is not set, a new instance of the {@link ScoreLocalDataSourceImpl} will be created.
     */
    public static ScoreLocalDataSourceImpl getInstance(Context context) {
        if (instance == null) {
            instance = new ScoreLocalDataSourceImpl(context);
        }
        return instance;
    }

    @Override
    public boolean insertScore(ScoreHistory scoreHistory, String sequenceId) {
        boolean insertScore = scoreDao.insert(DataConverter.toScoreEntity(scoreHistory, sequenceId)) != 0;
        Log.d(TAG, String.format("insertScore. Status: %s. Score id: %s. Sequence id: %s.", insertScore, scoreHistory.getID(), sequenceId));
        return insertScore;
    }

    @Override
    public boolean updateObdPhotoCount(String scoreHistoryId, int obdPhotoCount) {
        boolean updateObdPhotoCount = scoreDao.updateObdPhotoCount(scoreHistoryId, obdPhotoCount) != 0;
        Log.d(TAG, String.format("updateObdPhotoCount. Status: %s. Score id: %s. Obd photo count: %s.", updateObdPhotoCount, scoreHistoryId, obdPhotoCount));
        return updateObdPhotoCount;
    }

    @Override
    public boolean updatePhotoCount(String scoreHistoryId, int photoCount) {
        boolean updatePhotoCount = scoreDao.updatePhotoCount(scoreHistoryId, photoCount) != 0;
        Log.d(TAG, String.format("updatePhotoCount. Status: %s. Score id: %s. Obd photo count: %s.", updatePhotoCount, scoreHistoryId, photoCount));
        return updatePhotoCount;
    }

    @Override
    public Maybe<Map<Integer, ScoreHistory>> getScoreHistory(String sequenceId) {
        return scoreDao
                .findAllBySequenceID(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getScoreHistory. Status: success. Sequence id: %s. Message: Score history found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getScoreHistory. Status: complete. Sequence id: %s. Message: Score history not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG, String.format("getScoreHistory. Status: error. Sequence id: %s. Message: %s.", sequenceId, throwable.getLocalizedMessage())))
                .toObservable()
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toScoreHistory))
                .toMap(ScoreHistory::getCoverage)
                .toMaybe();
    }

    @Override
    public double getScore(Map<Integer, ScoreHistory> scoreHistoryMap) {
        ArrayList<ScoreHistory> array = new ArrayList<>(scoreHistoryMap.values());
        Iterator<ScoreHistory> iter = array.iterator();
        double score = 0;
        while (iter.hasNext()) {
            ScoreHistory sch = iter.next();
            if (sch.getCoverage() == UNKNOWN_COVERAGE_VALUE) {
                iter.remove();
                continue;
            }
            score = score
                    + sch.getPhotoCount() * Utils.getValueOnSegment(sch.getCoverage())
                    + sch.getObdPhotoCount() * Utils.getValueOnSegment(sch.getCoverage()) * OBD_MULTIPLIER;
        }
        return score;
    }
}
