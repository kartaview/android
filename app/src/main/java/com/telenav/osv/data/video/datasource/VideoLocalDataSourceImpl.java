package com.telenav.osv.data.video.datasource;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import android.content.Context;
import com.telenav.osv.common.Injection;
import com.telenav.osv.data.database.DataConverter;
import com.telenav.osv.data.database.OSCDatabase;
import com.telenav.osv.data.video.database.dao.VideoDao;
import com.telenav.osv.data.video.model.Video;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * The local data source for the {@code Video} which handles the logic for storing and retrieving the videos from the local storage.
 * @author cameliao
 */

public class VideoLocalDataSourceImpl implements VideoLocalDataSource {

    /**
     * A {@code String} representing the tag of the current class.
     */
    private static final String TAG = VideoLocalDataSourceImpl.class.getSimpleName();

    /**
     * The instance of the current class.
     */
    private static VideoLocalDataSourceImpl instance;

    /**
     * Instance of the {@code database} DAO for the video data.
     */
    private VideoDao videoDao;

    /**
     * Default constructor for the current class. Private to prevent instantiation from external sources.
     * @param context the {@code Context} used to instantiate the local persistence.
     */
    private VideoLocalDataSourceImpl(@NonNull Context context) {
        OSCDatabase oscDatabase = Injection.provideOSCDatabase(context);
        videoDao = oscDatabase.videoDao();
    }

    /**
     * @param context the {@code Context} used to instantiate the local persistence.
     * @return {@code VideoLocalDataSource} representing {@link #instance}.
     */
    public static VideoLocalDataSourceImpl getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new VideoLocalDataSourceImpl(context);
        }
        return instance;
    }

    @Override
    public Maybe<Video> getVideo(@NotNull String videoId) {
        return videoDao
                .findByID(videoId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getVideo. Status: success. Video id: %s. Message: Video found.", videoId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getVideo. Status: complete. Video id: %s. Message: Video not found.", videoId)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getVideo. Status: error. Video id: %s.  Message: %s.",
                                videoId,
                                throwable.getLocalizedMessage())))
                .map(DataConverter::toVideo);
    }

    @Override
    public boolean saveVideo(Video video, String sequenceID) {
        boolean videoInserted = videoDao.insert(DataConverter.toVideoEntity(video, sequenceID)) != 0;
        Log.d(TAG, String.format("saveVideo. Status: %s. Sequence id: %s.", videoInserted, sequenceID));
        return videoInserted;
    }

    @Override
    public boolean updateVideo(Video video, String sequenceID) {
        boolean updateVideo = videoDao.update(DataConverter.toVideoEntity(video, sequenceID)) != 0;
        Log.d(TAG, String.format("updateVideo. Status: %s. Sequence id: %s.", updateVideo, sequenceID));
        return updateVideo;
    }

    @Override
    public boolean updateFrameCount(String videoId, int frameCount) {
        boolean updateVideoFrameCount = videoDao.updateFrameCount(videoId, frameCount) != 0;
        Log.d(TAG, String.format("updateVideoFrameCount. Status: %s. Frame count: %s.", updateVideoFrameCount, frameCount));
        return updateVideoFrameCount;
    }

    @Override
    public boolean deleteVideo(@NonNull String videoID) {
        boolean videoDelete = videoDao.deleteById(videoID) != 0;
        Log.d(TAG, String.format("deleteVideo. Status: %s. Video id: %s.", videoDelete, videoID));
        return videoDelete;
    }

    @Override
    public Single<List<String>> getVideoIdsBySequenceId(@NonNull String sequenceId) {
        return videoDao
                .findAllIdsBySequenceId(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, "getVideoIdsBySequenceId. Status: success. Message: Videos ids found."))
                .doOnError(throwable -> Log.d(TAG, String.format("getVideoIdsBySequenceId. Status: error. Message: %s.", throwable.getLocalizedMessage())));
    }

    @Override
    public Maybe<List<Video>> getVideos(String sequenceId) {
        return videoDao
                .findAllBySequenceID(sequenceId)
                .doOnSuccess(items -> Log.d(TAG, String.format("getVideos. Status: success. Sequence id: %s. Message: Videos found.", sequenceId)))
                .doOnComplete(() -> Log.d(TAG, String.format("getVideos. Status: complete. Sequence id: %s. Message: Videos not found.", sequenceId)))
                .doOnError(throwable -> Log.d(TAG, String.format("getVideos. Status: error. Sequence id: %s. Message: %s.", sequenceId, throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toVideo)
                        .toList()
                        .toMaybe());
    }

    @Override
    public int getVideoCountBySequenceId(@NonNull String sequenceId) {
        int videoCount = videoDao.findNumberOfRows(sequenceId);
        Log.d(TAG, String.format(
                "getVideoCountBySequenceId. Sequence id: %s. Count: %s. Message: Fetching the video count from persistence for the sequence given.",
                sequenceId,
                videoCount));
        return videoCount;
    }
}
