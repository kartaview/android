package com.telenav.osv.data.frame.datasource.remote;

import java.util.List;
import com.telenav.osv.data.frame.model.Frame;
import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Flowable;

/**
 * @author horatiuf
 */

public class FrameRemoteDataSourceImpl implements FrameRemoteDataSource {

    /**
     * Instance for the current class.
     */
    private static FrameRemoteDataSourceImpl INSTANCE;

    /**
     * Default constructor for the current class. Private to prevent instantiation outside the class scope.
     */
    private FrameRemoteDataSourceImpl() {

    }

    /**
     * @return {@code FrameRemoteDataSourceImpl} representing {@link #INSTANCE}.
     */
    public static FrameRemoteDataSourceImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FrameRemoteDataSourceImpl();
        }
        return INSTANCE;
    }


    @Override
    public Flowable<List<Frame>> getFrames(@NonNull String sequenceId) {
        return null;
    }

    @Override
    public Completable deleteAsync(String frame) {
        return null;
    }

    @Override
    public Completable saveFrame(Frame frame) {
        return null;
    }
}
