package com.telenav.osv.data.video.datasource;

import java.util.List;
import com.telenav.osv.data.CompressionDataSource;
import com.telenav.osv.data.video.model.Video;
import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Data Source interface for the {@code Videos} persistence.
 * <p>The interface provides all the available operations for the data source such as:
 * <ul>
 * <li>{@link #saveVideo(Video, String)}</li>
 * <li>{@link #deleteVideo(String)}</li>
 * <li>{@link #getVideos(String)}</li>
 * <li>{@link #getVideoCountBySequenceId(String)}</li>
 * </ul>
 * @author cameliao
 */

public interface VideoLocalDataSource extends CompressionDataSource {

    /**
     * @param sequenceID the {@code identifier} for the sequence the video corresponds to.
     * @param video the {@code Video} which will be persisted.
     * @return true if the save operation was successful and a new video was inserted, false otherwise.
     * If the video already exists in the database the return value will be false.
     */
    boolean saveVideo(@NonNull Video video, @NonNull String sequenceID);

    /**
     * @param videoId the video identifier.
     * @return {@code Maybe} with one of the following callbacks:
     * <ul>
     * <li>success - {@code Video} based on the given video identifier</li>
     * <li>complete - {@code empty} response since the video was not found</li>
     * <li>error - error message received</li>
     */
    Maybe<Video> getVideo(@NonNull String videoId);

    /**
     * @param sequenceID the {@code identifier} for the sequence the video corresponds to.
     * @param video the {@code Video} which will be updated.
     * @return {@code true} if the video has update successful, {@code false} otherwise.
     */
    boolean updateVideo(@NonNull Video video, @NonNull String sequenceID);

    /**
     * @param videoId the {@code identifier} for the video in order to update the frame.
     * @param frameCount the new frame count of the video.
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    boolean updateFrameCount(@NonNull String videoId, int frameCount);

    /**
     * @param videoId the {@code identifier} for the video which will be deleted.
     * @return true if a video was deleted, false if the video was not found to be deleted.
     */
    boolean deleteVideo(@NonNull String videoId);

    /**
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of Video {@code ids} found in the persistence. </li>
     * <li>error - error message received</li>
     **/
    Single<List<String>> getVideoIdsBySequenceId(@NonNull String sequenceId);

    /**
     * @param sequenceId the sequence identifier for which the videos will be returned.
     * @return a list of {@code Video}s which are part from the given track.
     */
    Maybe<List<Video>> getVideos(@NonNull String sequenceId);

    /**
     * @param sequenceId the identifier of the sequence.
     * @return {@code number} of the videos for given sequence identifier.
     */
    int getVideoCountBySequenceId(@NonNull String sequenceId);
}
