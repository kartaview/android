package com.telenav.osv.data.frame.datasource.local;

import java.util.List;
import com.telenav.osv.data.CompressionDataSource;
import com.telenav.osv.data.frame.model.Frame;
import androidx.annotation.NonNull;
import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Local interface for the frame persistence such as:
 * <ul>
 * <li>{@link #getFrames(String)}</li>
 * <li>{@link #getFramesWithLocations(String)}</li>
 * <li>{@link #deleteFrame(String)}</li>
 * <li>{@link #saveFrame(Frame, String)}</li>
 * <li>{@link #getFrameCountBySequenceId(String)}</li>
 * </ul>
 * @author horatiuf
 */

public interface FrameLocalDataSource extends CompressionDataSource {

    /**
     * @param sequenceId the sequence id for which the query for local frame will be filtered by.
     * @return {@code Maybe} stream of a collection of {@code Frame} objects which have foreign key the given sequence identifier.
     */
    Maybe<List<Frame>> getFrames(@NonNull String sequenceId);

    /**
     * The main difference between this an the {@code getFrames} method is that this includes the location where the frame was taken.
     * @param sequenceId the sequence id for which the query for local frame will be filtered by.
     * @return {@code Maybe} stream of a collection of {@code Frame} objects which have foreign key the given sequence identifier.
     */
    Maybe<List<Frame>> getFramesWithLocations(@NonNull String sequenceId);

    /**
     * @param frameId the frame id for which the query will be filtered by.
     * @return {@code Maybe} stream of a collection of {@code Frame} objects which have foreign key the given sequence identifier.
     */
    Maybe<Frame> getFrame(@NonNull String frameId);

    /**
     * The main difference between this an the {@code getFrame} method is that this includes the location where the frame was taken.
     * @param frameId the frame id for which the query will be filtered by.
     * @return {@code Maybe} stream of a collection of {@code Frame} objects which have foreign key the given sequence identifier.
     */
    Maybe<Frame> getFrameWithLocation(@NonNull String frameId);

    /**
     * @return {@code Single} with one of the following callbacks:
     * <ul>
     * <li>success - collection of Frame {@code ids} found in the persistence. </li>
     * <li>error - error message received</li>
     **/
    Single<List<String>> getFrameIdsBySequenceId(@NonNull String sequenceId);

    /**
     * Removes the frame from the persistence.
     * @param frameId {@code identifier} for the frame. It cannot be null.
     */
    boolean deleteFrame(@NonNull String frameId);

    /**
     * Persists the given frame into the persistence.
     * @param frame the {@code Frame} which will be saved. It cannot be null.
     * @param sequenceID the {@code identifier} for the sequence to which the frame corresponds to.
     * @return {@code true} if the save operation was performed successful, {@code false} otherwise.
     */
    boolean saveFrame(@NonNull Frame frame, @NonNull String sequenceID);

    /**
     * @param sequenceId the identifier of the sequence.
     * @return {@code number} of the frames for given sequence identifier.
     */
    int getFrameCountBySequenceId(@NonNull String sequenceId);
}
