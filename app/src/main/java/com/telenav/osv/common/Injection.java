package com.telenav.osv.common;

import android.content.Context;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.initialisation.DataConsistency;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.common.listener.ListenerDefault;
import com.telenav.osv.data.database.OSCDatabase;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSourceImpl;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.location.datasource.LocationLocalDataSourceImpl;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.data.score.datasource.ScoreLocalDataSourceImpl;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSourceImpl;
import com.telenav.osv.data.user.datasource.UserRepository;
import com.telenav.osv.data.user.datasource.local.UserLocalDataSource;
import com.telenav.osv.data.user.datasource.remote.UserRemoteDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSourceImpl;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.LocationServiceManager;
import com.telenav.osv.network.FactoryServerEndpointUrl;
import com.telenav.osv.network.OscApi;
import com.telenav.osv.network.util.RetrofitFactory;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.camera.CameraManager;
import com.telenav.osv.recorder.encoder.VideoEncoder;
import com.telenav.osv.recorder.encoder.VideoEncoderManager;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.persistence.frame.FramePersistenceManager;
import com.telenav.osv.recorder.persistence.video.VideoPersistenceManager;
import com.telenav.osv.recorder.score.PositionMatcher;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.score.ScoreManager;
import com.telenav.osv.recorder.shutter.Shutter;
import com.telenav.osv.recorder.shutter.ShutterManager;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.upload.UploadManagerImpl;
import com.telenav.osv.utils.Size;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enables injection of implementations for classes at compile time.
 * <p>
 * This is useful since the data sources are singleton are required to be injected for testing, since it allows us to use a fake instance of the class to isolate the
 * dependencies and run a test hermetically.
 * @author horatiuf
 */
public class Injection {

    /**
     * Provides {@code UserRepository} concrete implementation for {@code UserDataSource}.
     * @param context the context used in the local data source.
     * @return {@code UserRepository}.
     */
    public static UserRepository provideUserRepository(@NonNull Context context) {
        checkNotNull(context);
        return UserRepository.getInstance(UserLocalDataSource.getInstance(context),
                UserRemoteDataSource.getInstance());
    }

    /**
     * Provides {@code ScoreLocalDataSourceImpl} concrete implementation for {@code ScoreDataSource}.
     * @param context the context used in the local data source.
     * @return {@code ScoreDataSource}.
     */
    public static ScoreDataSource provideScoreLocalDataSource(@NonNull Context context) {
        checkNotNull(context);
        return ScoreLocalDataSourceImpl.getInstance(context);
    }

    /**
     * Provides custom abstract implementation of a {@code RoomDatabase} which hold access to the persistence in the application.
     * @param context the context used for initialisation for the {@code RoomDatabase}.
     * @return {@code OSCDatabase}.
     */
    public static OSCDatabase provideOSCDatabase(@NonNull Context context) {
        checkNotNull(context);
        return OSCDatabase.getInstance(context);
    }

    /**
     * @param sequenceLocalDataSource the data source for the sequences. Used in order to update specific fields for data consistency.
     * @param locationLocalDataSource the location source in order to calculate the distance if required.
     * @param videoLocalDataSource instance representing the video data source. This will be used to check the path for each video from persistence to exist physically on device.
     * @param frameLocalDataSource instance representing the frame data source. This will be used to check the path for each frame from persistence to exist physically on device.
     * @param frameLocalDataSource instance representing the frame data source. This will be used to check the path for each frame from persistence to exist physically on device.
     * @param context the context required for data consistency external folder check.
     * @return {@code DataConsistency} class which handles the consistency of the sequences for file/data.
     */
    public static DataConsistency provideDataConsistency(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                                         @NonNull LocationLocalDataSource locationLocalDataSource,
                                                         @NonNull VideoLocalDataSource videoLocalDataSource,
                                                         @NonNull FrameLocalDataSource frameLocalDataSource,
                                                         @NonNull Context context) {
        checkNotNull(sequenceLocalDataSource);
        checkNotNull(locationLocalDataSource);
        checkNotNull(videoLocalDataSource);
        checkNotNull(frameLocalDataSource);
        checkNotNull(context);
        return DataConsistency.getInstance(
                sequenceLocalDataSource,
                locationLocalDataSource,
                videoLocalDataSource,
                frameLocalDataSource,
                context
        );
    }

    /**
     * Provides {@code VideoLocalDataSourceImpl} concrete implementation for {@code VideoLocalDataSource}.
     * @param context the context used for the local data source.
     * @return {@code VideoLocalDataSourceImpl}.
     */
    public static VideoLocalDataSource provideVideoDataSource(@NonNull Context context) {
        checkNotNull(context);
        return VideoLocalDataSourceImpl.getInstance(context);
    }

    /**
     * Provides {@code FrameLocalDataSourceImpl} concrete implementation for {@code FrameLocalDataSource}.
     * @param context the context used for the local data source.
     * @return {@code FrameLocalDataSourceImpl}.
     */
    public static FrameLocalDataSource provideFrameLocalDataSource(@NonNull Context context) {
        checkNotNull(context);
        return FrameLocalDataSourceImpl.getInstance(context);
    }

    /**
     * Provides {@code LocationLocalDataSourceImpl} concrete implementation for {@code FrameLocalDataSource}.
     * @param context the context used for the local data source.
     * @return {@code FrameLocalDataSourceImpl}.
     */
    public static LocationLocalDataSource provideLocationLocalDataSource(@NonNull Context context) {
        checkNotNull(context);
        return LocationLocalDataSourceImpl.getInstance(context);
    }

    /**
     * @param retrofit the configured builder used in the app. In order to customize one, use
     * {@link RetrofitFactory#provideRetrofitBuilder(String, CallAdapter.Factory, OkHttpClient, Converter.Factory...)}.
     * @return {@code OscApi} retrofit implementation based on provided builder.
     */
    public static OscApi provideOscApi(Retrofit retrofit) {
        return retrofit.create(OscApi.class);
    }

    /**
     * @param url the url which will be used for all network requests.
     * @param internetAvailabilityEventBus the event bus used for internet changes.
     * @param applicationPreferences the application preferences required by the interceptor in order to be created.
     * @param context the context required by the interceptor in order to be created.
     * @param noInternetListener the listener which can be set to receive no internet callback.
     * @return {@code OscApi} retrofit implementation by using de default builder provided by
     * {@link RetrofitFactory#provideUploadRetrofitBuilder(String, SimpleEventBus, ApplicationPreferences, Context, ListenerDefault)} with the given params.
     */
    public static OscApi provideUploadOscApi(@NonNull String url,
                                             @NonNull SimpleEventBus internetAvailabilityEventBus,
                                             @NonNull ApplicationPreferences applicationPreferences,
                                             @NonNull Context context,
                                             @Nullable ListenerDefault noInternetListener) {
        checkNotNull(url);
        checkNotNull(internetAvailabilityEventBus);
        checkNotNull(applicationPreferences);
        checkNotNull(context);
        return RetrofitFactory.provideUploadRetrofitBuilder(url, internetAvailabilityEventBus, applicationPreferences, context, noInternetListener).create(OscApi.class);
    }

    /**
     * Provides {@code FrameLocalDataSourceImpl} concrete implementation for {@code FrameLocalDataSource}.
     * @param context the context used for the local data source.
     * @return {@code FrameLocalDataSourceImpl}.
     */
    public static SequenceLocalDataSource provideSequenceLocalDataSource(@NonNull Context context,
                                                                         @NonNull FrameLocalDataSource frameLocalDataSource,
                                                                         @NonNull ScoreDataSource scoreLocalDataSource,
                                                                         @NonNull LocationLocalDataSource locationLocalDataSource,
                                                                         @NonNull VideoLocalDataSource videoLocalDataSource) {
        checkNotNull(context);
        checkNotNull(frameLocalDataSource);
        checkNotNull(scoreLocalDataSource);
        checkNotNull(videoLocalDataSource);
        return SequenceLocalDataSourceImpl.getInstance(context,
                frameLocalDataSource,
                scoreLocalDataSource,
                locationLocalDataSource,
                videoLocalDataSource);
    }

    /**
     * Provides {@code ObdManager} class which handles all functionality of OBD functionality.
     * @param context the {@code Context} required by the {@code ObdManager}.
     * @param applicationPreferences the {@code ApplicationPreferences} required by the {@code ObdManager}.
     * @return {@link ObdManager}
     */
    public static ObdManager provideObdManager(@NonNull Context context, @NonNull ApplicationPreferences applicationPreferences) {
        checkNotNull(context);
        checkNotNull(applicationPreferences);
        return ObdManager.getInstance(context, applicationPreferences);
    }

    /**
     * Provides {@code LocationService} implementation which handles all the Location functionality.
     * @param context the {@code Context} required by the location component.
     * @return the concrete implementation of {@link LocationService} interface.
     */
    public static LocationService provideLocationService(@NonNull Context context) {
        checkNotNull(context);
        return LocationServiceManager.getInstance(context);
    }

    /**
     * Provides {@code Score} implementation which handles the logic for score functionality.
     * @param scoreDataSource the score data source used to update the sequence score.
     * @param positionMatcher matcher to identify the location position for score coverage.
     * @param locationService the service for receiving location updates.
     * @param obdManager the obd manager for receiving updates in order to increase the points value.
     * @return the concrete implementation of {@link Score} interface.
     */
    public static Score provideScoreManager(@NonNull ScoreDataSource scoreDataSource, @NonNull PositionMatcher positionMatcher, @NonNull LocationService locationService,
                                            @NonNull ObdManager obdManager) {
        checkNotNull(scoreDataSource);
        checkNotNull(positionMatcher);
        checkNotNull(locationService);
        checkNotNull(obdManager);
        return new ScoreManager(scoreDataSource, positionMatcher, locationService, obdManager);
    }

    /**
     * Provides {@code Shutter} implementation which handles the logic for photo capturing functionality.
     * @param locationService location service used to retrieve location updates.
     * @param obdManager used for obd connectivity updates.
     * @return the concrete implementation of {@link Shutter} interface.
     */
    public static Shutter provideShutterManager(@NonNull LocationService locationService, @NonNull ObdManager obdManager, @NonNull boolean isBenchmarkLogicEnabled) {
        checkNotNull(locationService);
        checkNotNull(obdManager);
        return new ShutterManager(locationService, obdManager, isBenchmarkLogicEnabled);
    }

    /**
     * Provides {@code RecordingPersistence} implementation which is responsible to store JPEG images.
     * @param sequenceLocalDataSource data source for sequence used to update the details when a picture is taken.
     * @param locationLocalDataSource data source for location used to store the frame location.
     * @param frameLocalDataSource data source for storing frame information.
     * @return the concrete implementation of {@link RecordingPersistence}.
     */
    public static RecordingPersistence provideRecordingPersistence(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                                                   @NonNull LocationLocalDataSource locationLocalDataSource,
                                                                   @NonNull FrameLocalDataSource frameLocalDataSource) {
        checkNotNull(sequenceLocalDataSource);
        checkNotNull(locationLocalDataSource);
        checkNotNull(frameLocalDataSource);
        return new FramePersistenceManager(sequenceLocalDataSource, locationLocalDataSource, frameLocalDataSource);
    }

    /**
     * Provides {@code RecordingPersistence} implementation which is responsible to store video information.
     * @param sequenceLocalDataSource data source for sequence used to update the details when a frame is received.
     * @param locationLocalDataSource data source for location used to store the frame location.
     * @param videoLocalDataSource data source for storing video information.
     * @param videoEncoder the encoder used to convert camera frames to video file.
     * @return the concrete implementation of {@link RecordingPersistence}.
     */
    public static RecordingPersistence provideRecordingPersistence(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                                                   @NonNull LocationLocalDataSource locationLocalDataSource,
                                                                   @NonNull VideoLocalDataSource videoLocalDataSource,
                                                                   @NonNull VideoEncoder videoEncoder) {
        checkNotNull(sequenceLocalDataSource);
        checkNotNull(locationLocalDataSource);
        checkNotNull(videoLocalDataSource);
        checkNotNull(videoEncoder);
        return new VideoPersistenceManager(sequenceLocalDataSource, locationLocalDataSource, videoLocalDataSource, videoEncoder);
    }

    /**
     * Provides {@code VideoEncoder} implementation which is responsible to convert camera frames to video.
     * @return the concrete implementation of {@link VideoEncoder}.
     */
    public static VideoEncoder provideVideoEncoder() {
        return new VideoEncoderManager();
    }

    /**
     * Provides {@code PositionMatcher} which is responsible to match user's location on
     * @param context the context required for position matcher.
     * @return
     */
    public static PositionMatcher providePositionMatcher(@NonNull Context context) {
        checkNotNull(context);
        return new PositionMatcher(context);
    }

    /**
     * Provides {@code Camera} implementation which handles the logic for camera functionality.
     * @param context the {@code Context} required to instantiate the {@code Camera}.
     * @param pictureSize the picture size for the camera.
     * @param screenSize the screen size for camera preview.
     * @param isJpegMode a flag which defines if the camera should be in the picture mode.
     * @return the concrete implementation of {@link Camera} interface.
     */
    public static Camera provideCamera(@NonNull Context context, @NonNull Size pictureSize, @NonNull Size screenSize, boolean isJpegMode) {
        checkNotNull(context);
        checkNotNull(pictureSize);
        checkNotNull(screenSize);
        return new CameraManager(context, pictureSize, screenSize, isJpegMode);
    }

    /**
     * Provides {@code UploadManager} interface which handles all functionality of upload.
     * @return {@code UploadManager} instance for the implementation which adheres to the interface requested.
     */
    public static UploadManager provideUploadManager() {
        return UploadManagerImpl.getInstance();
    }

    /**
     * Provides {@code FactoryServerEndpointUrl} which handles all functionality of url get and set.
     * @param applicationPreferences the preferences of the device required by the url factory.
     * @return {@code FactoryServerEndpointUrl} instance.
     */
    public static FactoryServerEndpointUrl provideNetworkFactoryUrl(@NonNull ApplicationPreferences applicationPreferences) {
        checkNotNull(applicationPreferences);
        return new FactoryServerEndpointUrl(applicationPreferences);
    }
}
