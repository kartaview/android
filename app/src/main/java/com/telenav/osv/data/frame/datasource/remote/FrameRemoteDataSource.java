package com.telenav.osv.data.frame.datasource.remote;

import java.util.List;
import com.telenav.osv.data.frame.model.Frame;
import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Flowable;

/**
 * Remote interface for the frame server API such as:
 * <ul>
 * <li>{@link #getFrames(String)}</li>
 * <li>{@link #deleteAsync(String)}</li>
 * <li>{@link #saveFrame(Frame)}</li>
 * </ul>
 * @author horatiuf
 */

public interface FrameRemoteDataSource {

    /**
     * @param sequenceId the sequence id by which the search will be performed for. This cannot be null.
     * @return {@code List<Frame>} from the API which have the sequence id as the one set by the {@code sequenceId}.
     */
    Flowable<List<Frame>> getFrames(@NonNull String sequenceId);

    Completable deleteAsync(String frame);

    Completable saveFrame(Frame frame);
}
