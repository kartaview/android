package com.telenav.osv.data.frame.datasource.local;

import java.util.List;
import android.content.Context;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.database.DataConverter;
import com.telenav.osv.data.database.OSCDatabase;
import com.telenav.osv.data.frame.database.dao.FrameDao;
import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Concrete implementation for {@code FrameLocalDataSource}.
 * <p>
 * The implementation uses a database as a device persistence which is used through {@code SequenceDB} singleton which uses a SQLite DB.
 * </p>
 * @author horatiuf
 * @see FrameLocalDataSource
 */

public class FrameLocalDataSourceImpl implements FrameLocalDataSource {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = FrameLocalDataSource.class.getSimpleName();

    /**
     * Constant for unknown value.
     */
    private static final int UNKNOWN_VALUE = -1;

    /**
     * Instance for the current class.
     */
    private static FrameLocalDataSourceImpl INSTANCE;

    /**
     * Reference to the database dao for {@code frame}.
     * @see FrameDao
     */
    private FrameDao frameDao;

    /**
     * Default constructor for the current class. Private to prevent instantiation outside the class scope.
     * @param context the {@code Context} used to instantiate the local persistence.
     */
    private FrameLocalDataSourceImpl(@NonNull Context context) {
        OSCDatabase oscDatabase = Injection.provideOSCDatabase(context);
        frameDao = oscDatabase.frameDao();
    }

    /**
     * @param context the {@code Context} used to instantiate the local persistence.
     * @return {@code FrameLocalDataSourceImpl} representing {@link #INSTANCE}.
     */
    public static FrameLocalDataSourceImpl getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new FrameLocalDataSourceImpl(context);
        }
        return INSTANCE;
    }

    @Override
    public Maybe<List<Frame>> getFrames(String sequenceId) {
        return frameDao
                .findAllBySequenceID(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getFrames. Status: success. Sequence id: %s. Message: Frames found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getFrames. Status: complete. Sequence id: %s. Message: Frames not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG, String.format("getFrames. Status: error. Sequence id: %s. Message: %s.", sequenceId, throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toFrame)
                        .toList()
                        .toMaybe());

    }

    @Override
    public Maybe<List<Frame>> getFramesWithLocations(String sequenceId) {
        return frameDao
                .findAllWithLocationBySequenceID(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getFramesWithLocations. Status: success. Sequence id: %s. Message: Frames found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getFramesWithLocations. Status: complete. Sequence id: %s. Message: Frames not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getFramesWithLocations. Status: error. Sequence id: %s. Message: %s.", sequenceId, throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toFrame)
                        .toList()
                        .toMaybe());
    }

    @Override
    public Maybe<Frame> getFrame(@NonNull String frameId) {
        return frameDao
                .findByID(frameId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getFrame. Status: success. Frame id: %s. Message: Frames found.", frameId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getFrame. Status: complete. Frame id: %s. Message: Frames not found.", frameId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getFrame. Status: error. Frame id: %s. Message: %s.", frameId, throwable.getLocalizedMessage())))
                .map(DataConverter::toFrame);
    }

    @Override
    public Maybe<Frame> getFrameWithLocation(@NonNull String frameId) {
        return frameDao
                .findWithLocationByID(frameId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getFrameWithLocation. Status: success. Frame id: %s. Message: Frames found.", frameId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getFrameWithLocation. Status: complete. Frame id: %s. Message: Frames not found.", frameId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getFrameWithLocation. Status: error. Frame id: %s. Message: %s.", frameId, throwable.getLocalizedMessage())))
                .map(DataConverter::toFrame);
    }

    @Override
    public Single<List<String>> getFrameIdsBySequenceId(@NonNull String sequenceId) {
        return frameDao
                .findAllIdsBySequenceId(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, "getFrameIdsBySequenceId. Status: success. Message: frames ids found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getFrameIdsBySequenceId. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    @Override
    public boolean deleteFrame(@NonNull String frameId) {
        boolean deleteResult = frameDao.deleteById(frameId) != 0;
        Log.d(TAG, String.format("deleteFrame. Status: %s. Id: %s.", deleteResult, frameId));
        return deleteResult;
    }

    @Override
    public boolean saveFrame(@NonNull Frame frame, @NonNull String sequenceID) {
        if (frame == null) {
            Log.d(TAG, String.format("saveFrame. The frame is null. Sequence id: %s", sequenceID));
            return false;
        }

        FrameEntity frameEntity = DataConverter.toFrameEntity(frame, sequenceID);
        frameDao.insert(frameEntity);
        //persist the frame
        String frameEntityID = frameEntity.getFrameId();
        if (frameEntityID == null) {
            Log.d(TAG, String.format("saveFrame. The frame was not persisted successfully. Sequence id: %s", sequenceID));
            return false;
        }
        return true;
    }

    @Override
    public int getFrameCountBySequenceId(@NonNull String sequenceId) {
        int frameCount = frameDao.findNumberOfRows(sequenceId);
        Log.d(TAG, String.format(
                "getFrameCountBySequenceId. Sequence id: %s. Count: %s. Message: Fetching the frame count from the persistence for the sequence given.",
                sequenceId,
                frameCount));
        return frameCount;
    }
}
