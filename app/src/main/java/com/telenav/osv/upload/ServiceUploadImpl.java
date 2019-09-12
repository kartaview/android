package com.telenav.osv.upload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.event.SimpleEventBus;
import com.telenav.osv.common.listener.ListenerDefault;
import com.telenav.osv.common.service.OscBaseBinder;
import com.telenav.osv.data.frame.datasource.local.FrameLocalDataSource;
import com.telenav.osv.data.sequence.datasource.local.SequenceLocalDataSource;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.user.datasource.UserRepository;
import com.telenav.osv.data.video.datasource.VideoLocalDataSource;
import com.telenav.osv.network.FactoryServerEndpointUrl;
import com.telenav.osv.network.OscApi;
import com.telenav.osv.network.internet.NetworkCallback;
import com.telenav.osv.upload.notification.NotificationUploadManager;
import com.telenav.osv.upload.operation.UploadOperationSequence;
import com.telenav.osv.upload.progress.ProgressProcessor;
import com.telenav.osv.upload.progress.model.UploadUpdateProgress;
import com.telenav.osv.upload.status.ServiceUpload;
import com.telenav.osv.upload.status.UploadStatus;
import com.telenav.osv.upload.status.UploadUpdate;
import com.telenav.osv.utils.StringUtils;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Concrete implementation of {@link ServiceUpload} general interface which will handle all upload related functionality. This will have both functionality of a bindable and
 * non-bindable service.
 * <p> The flag for {@link #onStartCommand(Intent, int, int)} will be {@link #START_NOT_STICKY} since there is no desire to automatically start over once it crashes. If said use
 * case would happen inconsistent bugs would happen.
 * <p> The service implement
 * @author horatiuf
 * @see ServiceUpload
 */

public final class ServiceUploadImpl extends Service implements ServiceUpload, ListenerDefault {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = ServiceUploadImpl.class.getSimpleName();

    /**
     * Specify for the delay merge operations how many of them can be concurrent.
     */
    private static final int MERGE_DELAY_ERROR_CONCURRENT_NO = 1;

    private static final Object uploadProgressSyncObject = new Object();

    private final UploadServiceBinder binder = new UploadServiceBinder();

    private OscApi api;

    private FactoryServerEndpointUrl urlFactory;

    private OSVApplication osvApplication;

    private SequenceLocalDataSource sequenceLocalDataSource;

    private VideoLocalDataSource videoLocalDataSource;

    private FrameLocalDataSource frameLocalDataSource;

    private UserRepository userRepository;

    private Scheduler uploadScheduler;

    private Disposable uploadDisposable;

    private UploadStatus uploadStatus;

    private ProgressProcessor progressProcessor;

    private NotificationUploadManager notificationUploadManager;

    private UploadUpdateProgress uploadUpdateProgressFolder;

    private SimpleEventBus internetEventBus;

    private Handler mainHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "ServiceUploadImpl ==> onCreate");
        osvApplication = (OSVApplication) getApplication();
        internetEventBus = new SimpleEventBus();
        urlFactory = Injection.provideNetworkFactoryUrl(osvApplication.getAppPrefs());
        String networkUrl = urlFactory.getServerEndpoint();
        api = Injection.provideUploadOscApi(urlFactory.getServerEndpoint(), internetEventBus, osvApplication.getAppPrefs(), getApplicationContext(), this);
        Log.d(TAG, String.format("onCreate. Status: %s. Message: setting upload environment.", networkUrl));
        Context context = getApplicationContext();
        frameLocalDataSource = Injection.provideFrameLocalDataSource(context);
        videoLocalDataSource = Injection.provideVideoDataSource(context);
        userRepository = Injection.provideUserRepository(context);
        sequenceLocalDataSource = Injection.provideSequenceLocalDataSource(context,
                frameLocalDataSource,
                Injection.provideScoreLocalDataSource(context),
                Injection.provideLocationLocalDataSource(context),
                videoLocalDataSource);
        mainHandler = new Handler(context.getMainLooper());
        uploadScheduler = Schedulers.from(Executors.newSingleThreadExecutor());
        notificationUploadManager = new NotificationUploadManager();
        progressProcessor = new ProgressProcessor(getUploadUpdateConsumer());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChangeConnectivityMonitor();
        uploadDisposable = startUpload();
        notificationUploadManager.start(getApplicationContext());
        startForeground(notificationUploadManager.getNotificationId(), notificationUploadManager.getNotification());
        progressProcessor.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        progressProcessor.stop();
        if (uploadDisposable != null) {
            uploadDisposable.dispose();
        }
        if (uploadStatus != null) {
            mainHandler.post(() -> uploadStatus.onUploadStoped());
        }
        if (notificationUploadManager != null) {
            notificationUploadManager.stop(getApplicationContext());
        }
        if (uploadScheduler != null) {
            uploadScheduler.shutdown();
        }
        Log.d(TAG, "onDestroy. Status: upload stopped.");
        super.onDestroy();
    }

    @Override
    public void setListener(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    @Override
    public void onCallback() {
        mainHandler.post(() -> {
            if (uploadStatus != null) {
                uploadStatus.onNoInternetDetected();
            }
        });
    }

    /**
     * Creates the connectivity change monitor.
     */
    private void createChangeConnectivityMonitor() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCallback networkCallback = new NetworkCallback(internetEventBus);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                connectivityManager.registerNetworkCallback(
                        new NetworkRequest.Builder().build(),
                        networkCallback);
            }
        }
    }

    /**
     * @return the consumer which will be called by the progress processor.
     * <p> The consumer purpose is to update the upload status and to provide the notification updates regarding progress.
     */
    private Consumer<UploadUpdate> getUploadUpdateConsumer() {
        return (uploadUpdate) -> {
            Log.d(TAG, "onCreate consumer. Status: upload update. Message: Upload update callback.");
            notificationUploadManager.update(uploadUpdate.getPercentage(), getApplicationContext());
            mainHandler.post(() -> {
                synchronized (uploadProgressSyncObject) {
                    if (uploadStatus != null) {
                        uploadStatus.onUploadProgressUpdate(uploadUpdate);
                    }
                }
            });
        };
    }

    /**
     * Starts the upload process.
     * @return {@code Disposable} which will
     */
    private Disposable startUpload() {
        return userRepository
                .getUser()
                .filter(item -> !StringUtils.isEmpty(item.getAccessToken()))
                .flatMapCompletable(item -> {
                    if (uploadStatus != null) {
                        Log.d(TAG, "startUpload. Status: signal upload started. Message: Upload started callback.");
                        mainHandler.post(() -> uploadStatus.onUploadStarted());
                    }
                    Log.d(TAG, "startUpload. Status: create sequence(s) stream. Message: Creating a stream merge between all existent stream operations.");
                    return sequenceLocalDataSource
                            .getSequences(true, SequenceDetailsLocal.SequenceConsistencyStatus.VALID, SequenceDetailsLocal.SequenceConsistencyStatus.DATA_MISSING)
                            .filter(sequences -> !sequences.isEmpty())
                            .flatMapCompletable(sequences ->
                                    Completable.mergeDelayError(processSequences(sequences, item.getAccessToken()), MERGE_DELAY_ERROR_CONCURRENT_NO))
                            .retry();
                })
                .subscribeOn(uploadScheduler)
                .observeOn(uploadScheduler)
                .subscribe(
                        () -> {
                            Log.d(TAG, "onStartCommand. onNext. Status: success. Message: Successful start sequence request.");
                            if (uploadStatus != null) {
                                mainHandler.post(() -> uploadStatus.onUploadComplete());
                            }
                        },
                        throwable -> {
                            if (uploadStatus != null) {
                                mainHandler.post(() -> uploadStatus.onUploadError());
                            }
                            Log.d(TAG, String.format("onStartCommand. Status: error. Message: %s.", throwable.getLocalizedMessage()));
                        });
    }

    private Flowable<Completable> processSequences(List<LocalSequence> localSequences, String accessToken) {
        long fileSize = 0;
        List<Completable> completables = new ArrayList<>();
        for (LocalSequence localSequence : localSequences) {
            fileSize += localSequence.getLocalDetails().getDiskSize();
            completables.add(
                    new UploadOperationSequence(
                            localSequence.getID(),
                            sequenceLocalDataSource,
                            accessToken,
                            api,
                            frameLocalDataSource,
                            videoLocalDataSource,
                            uploadProgress -> {
                                if (uploadUpdateProgressFolder != null) {
                                    uploadUpdateProgressFolder.addChild(uploadProgress);
                                }
                            })
                            .getStream());
        }
        if (uploadUpdateProgressFolder == null) {
            Log.d(TAG, String.format("processSequences. Status: add total upload progress. Size: %s. Message: Create update progress with total size of sequences folder.",
                    fileSize));
            uploadUpdateProgressFolder = new UploadUpdateProgress(0, fileSize);
            progressProcessor.addChild(uploadUpdateProgressFolder);
        }
        return Flowable.fromIterable(completables);
    }

    /**
     * Concrete implementation for {@code OscBaseBinder} for current {@link ServiceUploadImpl}.
     * @see UploadServiceBinder
     */
    public class UploadServiceBinder extends OscBaseBinder {

        @Override
        public ServiceUploadImpl getService() {
            return ServiceUploadImpl.this;
        }
    }
}
