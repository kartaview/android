package com.telenav.osv.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.initialisation.DataConsistency;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.common.listener.ListenerDefault;
import com.telenav.osv.data.database.KVDatabase;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSourceImpl;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.location.datasource.LocationLocalDataSourceImpl;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.data.score.datasource.ScoreLocalDataSourceImpl;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSourceImpl;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.datasource.UserRepository;
import com.telenav.osv.data.user.datasource.local.UserLocalDataSource;
import com.telenav.osv.data.user.datasource.remote.UserRemoteDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.data.video.datasource.VideoLocalDataSourceImpl;
import com.telenav.osv.jarvis.login.network.JarvisLoginApi;
import com.telenav.osv.jarvis.login.usecase.JarvisLoginUseCase;
import com.telenav.osv.jarvis.login.usecase.JarvisLoginUseCaseImpl;
import com.telenav.osv.jarvis.login.utils.LoginUtils;
import com.telenav.osv.location.AccuracyQualityChecker;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.LocationServiceManager;
import com.telenav.osv.manager.network.GeometryRetriever;
import com.telenav.osv.map.render.mapbox.grid.loader.GridsLoader;
import com.telenav.osv.map.viewmodel.MapViewModelFactory;
import com.telenav.osv.network.GenericJarvisApiErrorHandler;
import com.telenav.osv.network.GenericJarvisApiErrorHandlerImpl;
import com.telenav.osv.network.KVApi;
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl;
import com.telenav.osv.network.request.interceptor.JarvisRequestAuthorizationInterceptor;
import com.telenav.osv.network.util.RetrofitFactory;
import com.telenav.osv.obd.manager.ObdManager;
import com.telenav.osv.recorder.camera.Camera;
import com.telenav.osv.recorder.camera.CameraManager;
import com.telenav.osv.recorder.encoder.VideoEncoder;
import com.telenav.osv.recorder.encoder.VideoEncoderManager;
import com.telenav.osv.recorder.gpsTrail.GpsTrailHelper;
import com.telenav.osv.recorder.metadata.MetadataSensorManager;
import com.telenav.osv.recorder.metadata.callback.MetadataGpsCallback;
import com.telenav.osv.recorder.metadata.callback.MetadataObdCallback;
import com.telenav.osv.recorder.metadata.callback.MetadataPhotoVideoCallback;
import com.telenav.osv.recorder.persistence.RecordingPersistence;
import com.telenav.osv.recorder.persistence.frame.FramePersistenceManager;
import com.telenav.osv.recorder.persistence.video.VideoPersistenceManager;
import com.telenav.osv.recorder.score.PositionMatcher;
import com.telenav.osv.recorder.score.Score;
import com.telenav.osv.recorder.score.ScoreManager;
import com.telenav.osv.recorder.shutter.Shutter;
import com.telenav.osv.recorder.shutter.ShutterManager;
import com.telenav.osv.report.network.ReportIssueApi;
import com.telenav.osv.report.usecase.ReportIssueUseCase;
import com.telenav.osv.report.usecase.ReportIssueUseCaseImpl;
import com.telenav.osv.report.viewmodel.ReportIssueViewModelFactory;
import com.telenav.osv.tasks.network.TasksApi;
import com.telenav.osv.tasks.usecases.FetchAssignedTasksUseCase;
import com.telenav.osv.tasks.usecases.FetchAssignedTasksUseCaseImpl;
import com.telenav.osv.tasks.usecases.TaskDetailsUseCase;
import com.telenav.osv.tasks.usecases.TaskDetailsUseCaseImpl;
import com.telenav.osv.tasks.utils.CurrencyUtil;
import com.telenav.osv.tasks.utils.CurrencyUtilImpl;
import com.telenav.osv.tasks.viewmodels.TaskDetailsViewModelFactory;
import com.telenav.osv.tasks.viewmodels.TasksViewModelFactory;
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.RecordingViewModel;
import com.telenav.osv.upload.UploadManager;
import com.telenav.osv.upload.UploadManagerImpl;
import com.telenav.osv.utils.Size;

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
        return UserRepository.getInstance(UserLocalDataSource.getInstance(provideApplicationPreferences(context)),
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
     *
     * @param context the context used for initialisation for the {@code RoomDatabase}.
     * @return {@code KVDatabase}.
     */
    public static KVDatabase provideKVDatabase(@NonNull Context context) {
        checkNotNull(context);
        return KVDatabase.getInstance(context);
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
     *                 {@link RetrofitFactory#provideRetrofitBuilder(String, CallAdapter.Factory, OkHttpClient, Converter.Factory...)}.
     * @return {@code KVApi} retrofit implementation based on provided builder.
     */
    public static KVApi provideKvApi(Retrofit retrofit) {
        return retrofit.create(KVApi.class);
    }

    /**
     * @param url                          the url which will be used for all network requests.
     * @param internetAvailabilityEventBus the event bus used for internet changes.
     * @param applicationPreferences       the application preferences required by the interceptor in order to be created.
     * @param context                      the context required by the interceptor in order to be created.
     * @param noInternetListener           the listener which can be set to receive no internet callback.
     * @return {@code KVApi} retrofit implementation by using de default builder provided by
     * {@link RetrofitFactory#provideUploadRetrofitBuilder(String, SimpleEventBus, ApplicationPreferences, Context, ListenerDefault)} with the given params.
     */
    public static KVApi provideUploadKvApi(@NonNull String url,
                                           @NonNull SimpleEventBus internetAvailabilityEventBus,
                                           @NonNull ApplicationPreferences applicationPreferences,
                                           @NonNull Context context,
                                           @Nullable ListenerDefault noInternetListener) {
        checkNotNull(url);
        checkNotNull(internetAvailabilityEventBus);
        checkNotNull(applicationPreferences);
        checkNotNull(context);
        return RetrofitFactory.provideUploadRetrofitBuilder(url, internetAvailabilityEventBus, applicationPreferences, context, noInternetListener).create(KVApi.class);
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
    public static Shutter provideShutterManager(@NonNull LocationService locationService,
                                                @NonNull ObdManager obdManager,
                                                @NonNull boolean isBenchmarkLogicEnabled,
                                                @NonNull MetadataObdCallback metadataObdCallback,
                                                @NonNull MetadataGpsCallback metadataGpsCallback,
                                                @NonNull AccuracyQualityChecker accuracyQualityChecker) {
        checkNotNull(locationService);
        checkNotNull(obdManager);
        checkNotNull(metadataObdCallback);
        checkNotNull(metadataGpsCallback);
        checkNotNull(accuracyQualityChecker);
        return new ShutterManager(locationService, obdManager, isBenchmarkLogicEnabled, metadataObdCallback, metadataGpsCallback, accuracyQualityChecker);
    }

    /**
     * Provides {@code RecordingPersistence} implementation which is responsible to store JPEG images.
     * @param sequenceLocalDataSource data source for sequence used to update the details when a picture is taken.
     * @param locationLocalDataSource data source for location used to store the frame location.
     * @param frameLocalDataSource data source for storing frame information.
     * @param metadataPhotoVideoCallback the callback trigger for when a photo/video entry is taken.
     * @return the concrete implementation of {@link RecordingPersistence}.
     */
    public static RecordingPersistence provideRecordingPersistence(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                                                   @NonNull LocationLocalDataSource locationLocalDataSource,
                                                                   @NonNull FrameLocalDataSource frameLocalDataSource,
                                                                   @NonNull MetadataPhotoVideoCallback metadataPhotoVideoCallback) {
        checkNotNull(sequenceLocalDataSource);
        checkNotNull(locationLocalDataSource);
        checkNotNull(frameLocalDataSource);
        checkNotNull(metadataPhotoVideoCallback);
        return new FramePersistenceManager(sequenceLocalDataSource, locationLocalDataSource, frameLocalDataSource, metadataPhotoVideoCallback);
    }

    /**
     * Provides {@code RecordingPersistence} implementation which is responsible to store video information.
     * @param sequenceLocalDataSource data source for sequence used to update the details when a frame is received.
     * @param locationLocalDataSource data source for location used to store the frame location.
     * @param videoLocalDataSource data source for storing video information.
     * @param videoEncoder the encoder used to convert camera frames to video file.
     * @param metadataPhotoVideoCallback the callback trigger for when a photo/video entry is taken.
     * @return the concrete implementation of {@link RecordingPersistence}.
     */
    public static RecordingPersistence provideRecordingPersistence(@NonNull SequenceLocalDataSource sequenceLocalDataSource,
                                                                   @NonNull LocationLocalDataSource locationLocalDataSource,
                                                                   @NonNull VideoLocalDataSource videoLocalDataSource,
                                                                   @NonNull VideoEncoder videoEncoder,
                                                                   @NonNull MetadataPhotoVideoCallback metadataPhotoVideoCallback) {
        checkNotNull(sequenceLocalDataSource);
        checkNotNull(locationLocalDataSource);
        checkNotNull(videoLocalDataSource);
        checkNotNull(videoEncoder);
        checkNotNull(metadataPhotoVideoCallback);
        return new VideoPersistenceManager(sequenceLocalDataSource, locationLocalDataSource, videoLocalDataSource, videoEncoder, metadataPhotoVideoCallback);
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

    /**
     * Provides {@code MapViewModelFactory} which handles the functionality for map view.
     * @param locationLocalDataSource the source data for {@code Location} in the app.
     * @param userDataSource the source data for {@code User} in the app.
     * @param gridsLoader the loader for the grids required to load data from Jarvis.
     * @param geometryRetriever the geometry retriever which handles requests from network related to geometry.
     * @param applicationPreferences the application preferences used to determine the render map at login/logout.
     * @param recordingViewModel the view model for recording which is will be used to get data on the current recording sequence.
     * @return {@code MapViewModelFactory} instance.
     */
    public static MapViewModelFactory provideMapViewFactory(@NonNull LocationLocalDataSource locationLocalDataSource,
                                                            @NonNull UserDataSource userDataSource,
                                                            @NonNull GridsLoader gridsLoader,
                                                            @NonNull GeometryRetriever geometryRetriever,
                                                            @NonNull ApplicationPreferences applicationPreferences,
                                                            @NonNull RecordingViewModel recordingViewModel) {
        checkNotNull(geometryRetriever);
        checkNotNull(locationLocalDataSource);
        checkNotNull(userDataSource);
        checkNotNull(gridsLoader);
        checkNotNull(applicationPreferences);
        checkNotNull(recordingViewModel);
        return new MapViewModelFactory(locationLocalDataSource,
                userDataSource,
                gridsLoader,
                geometryRetriever,
                applicationPreferences,
                recordingViewModel);
    }

    /**
     * Provides {@code GridsLoader} which handles the load functionality for the grids.
     * @param fetchAssignedTasksUseCase the use case used in order to get the jarvis related tasks in the app.
     * @param genericJarvisApiErrorHandler the generic jarvis api error handler.
     * @return {@code GridsLoader} instance.
     */
    public static GridsLoader provideGridsLoader(@NonNull FetchAssignedTasksUseCase fetchAssignedTasksUseCase,
                                                 @NonNull GenericJarvisApiErrorHandler genericJarvisApiErrorHandler) {
        checkNotNull(fetchAssignedTasksUseCase);
        checkNotNull(genericJarvisApiErrorHandler);
        return new GridsLoader(fetchAssignedTasksUseCase, genericJarvisApiErrorHandler);
    }

    /**
     * Provides {@code MetadataSensorManager} which handles the functionality related to metadata sensor logging.
     * @return {@code MetadataSensorManager} instance.
     */
    public static MetadataSensorManager provideMetadataSensorManager() {
        return MetadataSensorManager.Companion.getINSTANCE();
    }

    /**
     * @param fetchAssignedTasksUseCase the use case for fetching assigned tasks.
     * @param currencyUtil which provide util methods for currency
     * @param genericJarvisApiErrorHandler which handles generic error for Jarvis API
     * @return {@code TasksViewModelFactory} instance which handles the functionality for tasks fragment.
     */
    public static TasksViewModelFactory provideTasksViewModelFactory(
            @NonNull FetchAssignedTasksUseCase fetchAssignedTasksUseCase,
            @NonNull CurrencyUtil currencyUtil,
            @NonNull GenericJarvisApiErrorHandler genericJarvisApiErrorHandler) {
        return new TasksViewModelFactory(
                fetchAssignedTasksUseCase,
                currencyUtil,
                genericJarvisApiErrorHandler);
    }

    /**
     * @param tasksApi repository to fetch assigned tasks.
     * @return {@code FetchAssignedTasksUseCase} instance that fetches assigned tasks for user.
     */
    public static FetchAssignedTasksUseCase provideFetchAssignedTasksUseCase(TasksApi tasksApi) {
        return new FetchAssignedTasksUseCaseImpl(tasksApi);
    }

    /**
     * @param needAuthorization states whether authorization interceptor is required
     * @param applicationPreferences used to fetch user token
     * @return {@code TasksApi} instance to make REST API call to fetch tasks.
     */
    public static TasksApi provideTasksApi(boolean needAuthorization, @NonNull ApplicationPreferences applicationPreferences) {
        return RetrofitFactory.provideJarvisRetrofitBuilder(needAuthorization, applicationPreferences)
                .create(TasksApi.class);
    }

    /**
     * @param applicationPreferences used to fetch user token
     * @return a new {@code OkHttpClient} instance for use in MapBox
     */
    public static OkHttpClient provideMapBoxOkHttpClient(@NonNull ApplicationPreferences applicationPreferences) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (LoginUtils.isLoginTypePartner(applicationPreferences)) {
            builder.addInterceptor(new JarvisRequestAuthorizationInterceptor(applicationPreferences));
        }
        return builder.build();
    }

    /**
     * @param taskDetailsUseCase the use case for fetching task details.
     * @return {@code TaskDetailsViewModelFactory} instance which handles the functionality for task details fragment.
     */
    public static TaskDetailsViewModelFactory provideTaskDetailsViewModelFactory(
            @NonNull TaskDetailsUseCase taskDetailsUseCase,
            @NonNull Context context,
            String taskId,
            @NonNull CurrencyUtil currencyUtil,
            @NonNull GenericJarvisApiErrorHandler genericJarvisApiErrorHandler) {
        return new TaskDetailsViewModelFactory(
                taskDetailsUseCase,
                provideUserRepository(context),
                taskId,
                currencyUtil,
                genericJarvisApiErrorHandler);
    }

    /**
     * @param tasksApi repository to fetch task details.
     * @return {@code TaskDetailsUseCase} instance that fetches task details.
     */
    public static TaskDetailsUseCase provideTaskDetailsUseCase(TasksApi tasksApi) {
        return new TaskDetailsUseCaseImpl(tasksApi);
    }

    /**
     * Provides CurrencyUtil which provide util methods for currency
     * @return {@code CurrencyUtil} instance
     */
    public static CurrencyUtil provideCurrencyUtil() {
        return new CurrencyUtilImpl();
    }

    /**
     * @return {@code JarvisLoginApi} instance to make REST API call for Jarvis Login.
     */
    public static JarvisLoginApi provideJarvisLoginApi() {
        return RetrofitFactory.provideGrabViewRetrofitBuilder().create(JarvisLoginApi.class);
    }

    /**
     * @param jarvisLoginApi repository for Jarvis login.
     * @return {@code JarvisLoginUseCase} instance for Jarvis login.
     */
    public static JarvisLoginUseCase provideJarvisLoginUseCase(JarvisLoginApi jarvisLoginApi) {
        return new JarvisLoginUseCaseImpl(jarvisLoginApi);
    }

    /**
     * @return {@code GenericJarvisApiErrorHandler} instance which handles generic error for Jarvis API
     */
    public static GenericJarvisApiErrorHandler provideGenericJarvisApiErrorHandler(
            @NonNull Context context,
            @NonNull ApplicationPreferences applicationPreferences) {
        return new GenericJarvisApiErrorHandlerImpl(
                provideJarvisLoginUseCase(provideJarvisLoginApi()),
                provideUserRepository(context),
                applicationPreferences);
    }

    /**
     * @param needAuthorization states whether authorization interceptor is required
     * @param applicationPreferences used to fetch user token
     * @return {@code ReportIssueApi} instance to make REST API call to report issue.
     */
    public static ReportIssueApi provideReportIssueApi(
            boolean needAuthorization,
            @NonNull ApplicationPreferences applicationPreferences
    ) {
        return RetrofitFactory.provideJarvisRetrofitBuilder(
                needAuthorization, applicationPreferences).create(ReportIssueApi.class);
    }

    /**
     * @param reportIssueApi repository to report issue.
     * @return {@code ReportIssueUseCase} instance that report issues.
     */
    public static ReportIssueUseCase provideReportIssueUseCase(ReportIssueApi reportIssueApi) {
        return new ReportIssueUseCaseImpl(reportIssueApi);
    }

    /**
     * @param reportIssueUseCase the use case for reporting issues.
     * @param genericJarvisApiErrorHandler handles generic error for Jarvis API.
     * @return {@code ReportIssueViewModelFactory} instance which handles the functionality for report issue fragment.
     */
    public static ReportIssueViewModelFactory provideReportIssueViewModelFactory(
            @NonNull ReportIssueUseCase reportIssueUseCase,
            @NonNull GenericJarvisApiErrorHandler genericJarvisApiErrorHandler,
            @NonNull LocationService locationService) {
        return new ReportIssueViewModelFactory(
                reportIssueUseCase,
                genericJarvisApiErrorHandler,
                locationService);
    }

    /**
     * @param context the context required to instantiate specific resources.
     * @param factoryServerEndpointUrl the factory which holds all url endpoints for all app network requests.
     * @return {@code GeometryRetriever} instance which handles the functionality related to retrieving geometry.
     */
    public static GeometryRetriever provideGeometryRetriever(@NonNull Context context, @NonNull FactoryServerEndpointUrl factoryServerEndpointUrl) {
        return new GeometryRetriever(context, factoryServerEndpointUrl);
    }

    /**
     * @param context the {@code Context} required to load the application preferences.
     * @return {@code ApplicationPreferences} of the app.
     */
    public static ApplicationPreferences provideApplicationPreferences(@NonNull Context context) {
        return ((KVApplication) context.getApplicationContext()).getAppPrefs();
    }

    /**
     * @param locationService the {@code LocationService} required to for location updates.
     * @return {@code GpsTrailHelper} which provides a trail of locations.
     */
    public static GpsTrailHelper provideGpsTrailHelper(@NonNull LocationService locationService) {
        checkNotNull(locationService);
        return new GpsTrailHelper(locationService);
    }
}
