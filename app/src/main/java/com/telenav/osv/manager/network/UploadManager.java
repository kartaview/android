package com.telenav.osv.manager.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.http.PhotoRequest;
import com.telenav.osv.http.ProgressiveEntity;
import com.telenav.osv.http.SequenceFinishedRequest;
import com.telenav.osv.http.SequenceRequest;
import com.telenav.osv.http.VideoRequest;
import com.telenav.osv.http.requestFilters.SequenceRequestFilter;
import com.telenav.osv.http.requestFilters.UploadRequestFilter;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.item.network.SequenceData;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsvRequestResponseListener;
import com.telenav.osv.manager.network.encoder.ScoreJsonEncoder;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.SequenceDataParser;
import com.telenav.osv.service.UploadJobService;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * *
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class UploadManager extends NetworkManager implements NetworkResponseDataListener<ApiResponse> {


    public static final int STATUS_IDLE = 0;

    public static final int STATUS_PAUSED = 3;

    private static final int STATUS_INDEXING = 1;

    private static final int STATUS_UPLOADING = 2;

    private static final String TAG = "UploadManager";

    public static int sUploadStatus = STATUS_IDLE;

    /**
     * create sequence request url
     */
    private static String URL_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/";

    private static String URL_VIDEO = "http://" + "&&" + URL_VER + "video/";

    private static String URL_PHOTO = "http://" + "&&" + URL_VER + "photo/";

    /**
     * finish a sequence upload and mark for processing
     */
    private static String URL_FINISH_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/finished-uploading/";

    private final ConcurrentLinkedQueue<SequenceRequest> mCreateQueue = new ConcurrentLinkedQueue<>();


    /**
     * request queue for operations
     * adding a request here will be automatically run in the next available time
     */
    private final RequestQueue mSequenceQueue;

    /**
     * filter used for filtering only image upload requests
     */
    private UploadRequestFilter uploadFilter;

    /**
     * class used for managing upload tasks
     */
    private VideoUploaderQueue videoUploaderQueue;

    private Handler mPartialResponseHandler;

    private WifiManager.WifiLock mWifiLock;

    private Handler mBackgroundHandler;

    private HandlerThread mHandlerThread;

    private SequenceDataParser mSequenceDataParser = new SequenceDataParser();

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();


    public UploadManager(Context context) {
        super(context);
        HandlerThread handlerThread2 = new HandlerThread("PartialResponse", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread2.start();
        mHandlerThread = new HandlerThread("BackgroundUpload", Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());

        mPartialResponseHandler = new Handler(handlerThread2.getLooper());
        this.mSequenceQueue = newRequestQueue(mContext, 1);
        mSequenceQueue.stop();
        this.videoUploaderQueue = new VideoUploaderQueue(mContext);
        setEnvironment();
        EventBus.register(this);
        VolleyLog.DEBUG = Utils.isDebugEnabled(mContext);
    }

    public static void cancelAutoUpload(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(UploadJobService.UPLOAD_JOB_ID);
        }
    }

    public static void scheduleAutoUpload(Context context) {
        try {
            ApplicationPreferences appPrefs = ((OSVApplication) context.getApplicationContext()).getAppPrefs();
            final boolean autoSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && autoSet) {
                JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                jobScheduler.cancel(UploadJobService.UPLOAD_JOB_ID);
                JobInfo.Builder builder = new JobInfo.Builder(UploadJobService.UPLOAD_JOB_ID, new ComponentName(context, UploadJobService.class))
                        .setRequiredNetworkType(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED) ? JobInfo.NETWORK_TYPE_ANY : JobInfo.NETWORK_TYPE_UNMETERED)
                        .setPersisted(true)
                        .setRequiresCharging(appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_CHARGING));
                int result = jobScheduler.schedule(builder.build());
                Log.d(TAG, "onCheckedChanged: scheduled upload task, result = " + result);
            }
        } catch (Exception e) {
            Log.d(TAG, "scheduleAutoUpload: " + Log.getStackTraceString(e));
        }
    }

    @Override
    protected void setEnvironment() {
        super.setEnvironment();
        URL_SEQUENCE = URL_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        URL_VIDEO = URL_VIDEO.replace("&&", URL_ENV[mCurrentServer]);
        URL_PHOTO = URL_PHOTO.replace("&&", URL_ENV[mCurrentServer]);
        URL_FINISH_SEQUENCE = URL_FINISH_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        Log.d(TAG, "setEnvironment: " + URL_ENV[mCurrentServer]);
    }

    /**
     * uploads an image to a specific sequence
     * @param sequence parent folder
     * @param video image file
     * @param onlineSequenceID online sequence id
     * @param sequenceIndex the fileIndex of the image
     * @param listener request listener
     */
    private void uploadVideo(final LocalSequence sequence, final OSVFile video, final int onlineSequenceID, final int sequenceIndex, final
    NetworkResponseDataListener<ApiResponse> listener) {
        if (!video.exists()) {
            Log.w(TAG, "uploadVideo: file doesn't exist: " + video.getPath());
            SequenceDB.instance.deleteVideo(video, sequence.getId(), sequenceIndex);
            return;
        }
        final VideoRequest imageUploadReq = new VideoRequest(URL_VIDEO, new OsvRequestResponseListener<HttpResponseParser, ApiResponse>(mHttpResponseParser) {

            @Override
            public void onSuccess(final int status, final ApiResponse apiResponse) {
                Log.d(TAG, "uploadVideo: success, entering background to delete file");
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "uploadVideo: video uploaded successfully: " + onlineSequenceID + "/" + video.getName());
                        sequence.setSize(sequence.getSize() - Utils.fileSize(video));
                        SequenceDB.instance.deleteVideo(video, sequence.getId(), sequenceIndex);
                        videoUploaderQueue.markDone(sequence);
                        listener.requestFinished(status, apiResponse);
                    }
                });
            }

            @Override
            public void onFailure(final int status, final ApiResponse apiResponse) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "uploadVideo: error uploading video: " + onlineSequenceID + "/" + video.getName() + ": " + apiResponse);
                        if (apiResponse.getApiCode() == API_DUPLICATE_ENTRY) {
                            sequence.setSize(sequence.getSize() - Utils.fileSize(video));
                            SequenceDB.instance.deleteVideo(video, sequence.getId(), sequenceIndex);
                            videoUploaderQueue.markDone(sequence);
                            listener.requestFinished(HTTP_OK, apiResponse);
                        } else {
                            if (apiResponse.getApiCode() == API_ARGUMENT_OUT_OF_RANGE && videoUploaderQueue.uploadTaskQueue.size() > 0) {
                                int nrRowsAffected = SequenceDB.instance.resetOnlineSequenceId(onlineSequenceID);
                                Log.d(TAG, "uploadVideo: rollback on sequence "
                                        + onlineSequenceID + ", nr of rows affected: " + nrRowsAffected);
                                final Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mContext, "Fixing sequence id...", Toast.LENGTH_LONG).show();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(mContext, "Try uploading the sequence now.", Toast.LENGTH_LONG).show();
                                            }
                                        }, 3030);
                                    }
                                });
                                Log.d(TAG, "uploadVideo: cancelling upload tasks");
                                cancelUploadTasks();
                            } else {
                                uploadVideo(sequence, video, onlineSequenceID, sequenceIndex, listener);
                            }
                        }
                    }
                });
            }
        }, new ProgressiveEntity.DataProgressListener() {
            @Override
            public void onProgressChanged(long totalSent, long totalSize) {
                videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
            }
        }, getAccessToken(), video, onlineSequenceID, sequenceIndex, mPartialResponseHandler);
        imageUploadReq.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
        videoUploaderQueue.add(imageUploadReq);
    }

    /**
     * uploads an image to a specific sequence
     * @param sequence parent folder
     * @param image image file
     * @param onlineSequenceID online sequence id
     * @param sequenceIndex the fileIndex of the image
     * @param listener request listener
     */
    private void uploadImage(final LocalSequence sequence, final OSVFile image, final int onlineSequenceID, final int sequenceIndex, final double lat, final double lon, final int
            acc, final NetworkResponseDataListener<ApiResponse> listener) {
        if (!image.exists()) {
            Log.w(TAG, "uploadImage: file doesn't exist: " + image.getPath());
            SequenceDB.instance.deletePhoto(image, sequence.getId(), sequenceIndex);
            return;
        }
        final PhotoRequest imageUploadReq = new PhotoRequest(URL_PHOTO, new OsvRequestResponseListener<HttpResponseParser, ApiResponse>(mHttpResponseParser) {

            @Override
            public void onSuccess(final int status, final ApiResponse apiResponse) {
                Log.d(TAG, "uploadImage: success, entering background to delete file");
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "uploadImage: image uploaded successfully: " + onlineSequenceID + "/" + image.getName());
                        sequence.setSize(sequence.getSize() - Utils.fileSize(image));
                        SequenceDB.instance.deletePhoto(image, sequence.getId(), sequenceIndex);
                        videoUploaderQueue.markDone(sequence);
                        listener.requestFinished(status, apiResponse);
                    }
                });
            }

            @Override
            public void onFailure(int status, final ApiResponse apiResponse) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "uploadImage: error uploading image: " + onlineSequenceID + "/" + image.getName() + ": " + apiResponse);

                        if (apiResponse.getApiCode() == API_DUPLICATE_ENTRY) {
                            apiResponse.setHttpCode(HTTP_OK);
                            apiResponse.setApiCode(API_SUCCESS);
                            sequence.setSize(sequence.getSize() - Utils.fileSize(image));
                            SequenceDB.instance.deletePhoto(image, sequence.getId(), sequenceIndex);
                            videoUploaderQueue.markDone(sequence);
                            listener.requestFinished(HTTP_OK, apiResponse);
                        } else {
                            if (apiResponse.getApiCode() == API_ARGUMENT_OUT_OF_RANGE && videoUploaderQueue.uploadTaskQueue.size() > 0) {
                                int nrRowsAffected = SequenceDB.instance.resetOnlineSequenceId(onlineSequenceID);
                                Log.d(TAG, "uploadImage: rollback on sequence "
                                        + onlineSequenceID + ", nr of rows affected: " + nrRowsAffected);
                                final Handler handler = new Handler(Looper.getMainLooper());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mContext, "Fixing sequence id...", Toast.LENGTH_LONG).show();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(mContext, "Try uploading the sequence now.", Toast.LENGTH_LONG).show();
                                            }
                                        }, 3030);
                                    }
                                });
                                Log.d(TAG, "uploadImage: cancelling upload tasks");
                                cancelUploadTasks();
                            } else {
                                uploadImage(sequence, image, onlineSequenceID, sequenceIndex, lat, lon, acc, listener);
                            }
                        }
                    }
                });
            }
        }, new ProgressiveEntity.DataProgressListener() {
            @Override
            public void onProgressChanged(long totalSent, long totalSize) {
                videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
            }
        }, getAccessToken(), image, onlineSequenceID, sequenceIndex, lat, lon, acc, mPartialResponseHandler);
        imageUploadReq.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
        videoUploaderQueue.add(imageUploadReq);
    }

    /**
     * creates a sequence online, used before uploading the images
     * @param sequence folder of the sequence
     */
    private void createSequence(final LocalSequence sequence) {
        final int onlineId = SequenceDB.instance.getOnlineId(sequence.getId());
        OSVFile metafile = new OSVFile(sequence.getFolder(), "track.txt.gz");
        if (!metafile.exists()) {
            metafile = new OSVFile(sequence.getFolder(), "track.txt");
        }
        final OSVFile finalMetafile = metafile;
        Log.d(TAG, "createSequence: creating sequence request");
        if (onlineId == -1) {
            Cursor cursor = SequenceDB.instance.getFrames(sequence.getId());
            if (cursor != null && cursor.getCount() > 0) {
                String position = "" + cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LAT)) + "," + cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LON));
                final SequenceRequest seqRequest = new SequenceRequest(URL_SEQUENCE, new OsvRequestResponseListener<SequenceDataParser, SequenceData>(mSequenceDataParser) {

                    @Override
                    public void onSuccess(final int status, final SequenceData sequenceData) {
                        runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                int updatedRows = SequenceDB.instance.updateSequenceOnlineId(sequence.getId(), sequenceData.getOnlineID());
                                if (updatedRows > 0) {
                                    sequence.setOnlineId(sequenceData.getOnlineID());
                                    if (finalMetafile.exists()) {
                                        sequence.setSize(sequence.getSize() - Utils.fileSize(finalMetafile));
                                        finalMetafile.delete();
                                    }
                                    uploadSequence(sequence, sequenceData.getOnlineID(), UploadManager.this);
                                    requestFinished(status, sequenceData);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(int status, final SequenceData sequenceData) {
                        runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                Log.w(TAG, "createSequence: " + sequenceData);

                                if (Thread.interrupted()) {
                                    Log.w(TAG, "createSequence: interrupted at error");
                                    return;
                                }
                                createSequence(sequence);
                                commitNextSequence();
                            }
                        });
                    }
                }, new ProgressiveEntity.DataProgressListener() {
                    @Override
                    public void onProgressChanged(long totalSent, long totalSize) {
                        videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
                    }
                }, sequence.getId(), getAccessToken(), position, metafile, sequence.getAppVersion(), sequence.hasObd(), sequence.getScore(), ScoreJsonEncoder.encode(sequence
                        .getScoreHistories()),
                        mPartialResponseHandler);
                seqRequest.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
                seqRequest.setShouldCache(false);
                mCreateQueue.add(seqRequest);
            } else {
                Log.w(TAG, "createSequence: cursor has 0 elements");
            }
            if (cursor != null) {
                cursor.close();
            }
        } else {//we need a request finished notification; hardcoded to fail since we have the sequence created already.
            sequence.setOnlineId(onlineId);
            SequenceRequest seqReq = new SequenceRequest("fail_on_purpose", new OsvRequestResponseListener<SequenceDataParser, SequenceData>(mSequenceDataParser) {

                @Override
                public void onSuccess(final int status, final SequenceData sequenceData) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            uploadSequence(sequence, onlineId, UploadManager.this);
                            requestFinished(status, sequenceData);
                            if (Thread.interrupted()) {
                                Log.w(TAG, "createSequence: interrupted at hardcoded");
                            }
                        }
                    });

                }

                @Override
                public void onFailure(final int status, final SequenceData sequenceData) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            if (finalMetafile.exists()) {
                                sequence.setSize(sequence.getSize() - Utils.fileSize(finalMetafile));
                                finalMetafile.delete();
                            }
                            uploadSequence(sequence, onlineId, UploadManager.this);
                            requestFinished(status, sequenceData);
                            if (Thread.interrupted()) {
                                Log.w(TAG, "createSequence: interrupted at hardcoded");
                            }

                        }
                    });
                }
            }, new ProgressiveEntity.DataProgressListener() {
                @Override
                public void onProgressChanged(long totalSent, long totalSize) {
                    videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
                }
            }, sequence.getId(), getAccessToken(), "", null, sequence.getAppVersion(), sequence.hasObd(), sequence.getScore(), ScoreJsonEncoder.encode(sequence.getScoreHistories
                    ()),
                    mPartialResponseHandler);
            seqReq.setShouldCache(false);
            seqReq.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
            mCreateQueue.add(seqReq);//need to add indexing cache notification
        }
    }

    private void commitNextSequence() {
        Log.d(TAG, "commitNextSequence: " + mCreateQueue.size() + "\n" +
                " -------------------------------------------------------------------------------------------------------------" + "\n"
                + "------------------------------------------------------------------------------------------------------------" + "\n"
                + "------------------------------------------------------------------------------------------------------------");
        sUploadStatus = STATUS_INDEXING;
        if (mCreateQueue.size() == 0) {
            videoUploaderQueue.finishUpload();
            return;
        }
        if (videoUploaderQueue.progressListener != null) {
            videoUploaderQueue.progressListener.onUploadingMetadata();
        }
        synchronized (mCreateQueue) {
            SequenceRequest request = mCreateQueue.poll();
            while (request == null) {
                if (mCreateQueue.size() == 0) {
                    return;
                }
                request = mCreateQueue.poll();
            }
            mSequenceQueue.add(request);
            mSequenceQueue.start();
        }
    }

    /**
     * finishes a sequence upload
     * @param sequence sequence
     * @param listener listener
     */
    private void finishSequence(final LocalSequence sequence, final NetworkResponseDataListener<ApiResponse> listener) {
        SequenceFinishedRequest seqRequest = new SequenceFinishedRequest(URL_FINISH_SEQUENCE, new OsvRequestResponseListener<HttpResponseParser, ApiResponse>(mHttpResponseParser) {
            @Override
            public void onSuccess(final int status, final ApiResponse apiResponse) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "finishSequence: " + apiResponse);
                        listener.requestFinished(status, apiResponse);
                    }
                });
            }

            @Override
            public void onFailure(final int status, final ApiResponse apiResponse) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.w(TAG, "finishSequence: error " + apiResponse);
                        finishSequence(sequence, listener);
                        listener.requestFailed(status, apiResponse);
                    }
                });
            }
        }, getAccessToken(), "" + sequence.getOnlineId());
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
        mQueue.add(seqRequest);
    }

    /**
     * uploads a sequence folder, containing images
     * @param sequence the sequence folder
     * @param sequenceIdOnline id
     * @param listener request listener
     */
    private void uploadSequence(LocalSequence sequence, final int sequenceIdOnline, NetworkResponseDataListener<ApiResponse> listener) {
        if (videoUploaderQueue.progressListener != null) {
            videoUploaderQueue.progressListener.onIndexingSequence(sequence, mCreateQueue.size());
        }
        if (sequence.isSafe()) {
            Cursor cursor = SequenceDB.instance.getFrames(sequence.getId());
            if (cursor != null && cursor.getCount() > 0) {
                while (!cursor.isAfterLast()) {
                    if (Thread.interrupted()) {
                        Log.w(TAG, "uploadSequence: interrupted");
                        cursor.close();
                        return;
                    }
                    String path = cursor.getString(cursor.getColumnIndex(SequenceDB.FRAME_FILE_PATH));
                    int index = cursor.getInt(cursor.getColumnIndex(SequenceDB.FRAME_SEQ_INDEX));
                    double lat = cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LAT));
                    double lon = cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LON));
                    int acc = cursor.getInt(cursor.getColumnIndex(SequenceDB.FRAME_ACCURACY));
                    OSVFile img = new OSVFile(path);
                    uploadImage(sequence, img, sequenceIdOnline, index, lat, lon, acc, listener);
                    if (videoUploaderQueue.progressListener != null) {
                        videoUploaderQueue.progressListener.onPreparing(videoUploaderQueue.uploadTaskQueue.size());
                    }
                    cursor.moveToNext();
                }
                cursor.close();
                sequence.setStatus(LocalSequence.STATUS_UPLOADING);
            }
            if (cursor != null) {
                cursor.close();
            }
        } else {
            Cursor cursor = SequenceDB.instance.getVideos(sequence.getId());
            if (cursor != null && cursor.getCount() > 0) {
                while (!cursor.isAfterLast()) {
                    if (Thread.interrupted()) {
                        Log.w(TAG, "uploadSequence: interrupted");
                        cursor.close();
                        return;
                    }
                    String path = cursor.getString(cursor.getColumnIndex(SequenceDB.VIDEO_FILE_PATH));
                    int index = cursor.getInt(cursor.getColumnIndex(SequenceDB.VIDEO_INDEX));
                    OSVFile img = new OSVFile(path);
                    uploadVideo(sequence, img, sequenceIdOnline, index, listener);
                    if (videoUploaderQueue.progressListener != null) {
                        videoUploaderQueue.progressListener.onPreparing(videoUploaderQueue.uploadTaskQueue.size());
                    }
                    cursor.moveToNext();
                }
                cursor.close();
                sequence.setStatus(LocalSequence.STATUS_UPLOADING);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        if (videoUploaderQueue.uploadTaskQueue.isEmpty()) {
            finishSequence(sequence, new NetworkResponseDataListener<ApiResponse>() {
                @Override
                public void requestFailed(int status, ApiResponse details) {
                    Log.w(TAG, "finishSequence: failed ");
                }

                @Override
                public void requestFinished(int status, ApiResponse details) {
                    Log.d(TAG, "finishSequence: success ");
                }

            });
        }
        //requests are done, start uploading
        mSequenceQueue.stop();
        videoUploaderQueue.commit();
    }

    /**
     * cancells all upload request added
     */
    public void cancelUploadTasks() {
        if (sUploadStatus != STATUS_IDLE) {
            mCreateQueue.clear();
            mSequenceQueue.stop();
            videoUploaderQueue.mVideoUploadQueue.stop();
            Log.d(TAG, "cancelUploadTasks: cancelled all upload tasks");
            try {
                mPartialResponseHandler.removeCallbacksAndMessages(null);
                mPartialResponseHandler.getLooper().getThread().interrupt();
            } catch (Exception ignored) {}
            try {
                mBackgroundHandler.removeCallbacksAndMessages(null);
                mBackgroundHandler.getLooper().getThread().interrupt();
            } catch (Exception ignored) {}
            if (mHandlerThread != null) {
                try {
                    mHandlerThread.quit();
                } catch (Exception ignored) {}
            }
            mHandlerThread = new HandlerThread("BackgroundUpload", Process.THREAD_PRIORITY_BACKGROUND);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            HandlerThread handlerThread2 = new HandlerThread("PartialResponse", Process.THREAD_PRIORITY_BACKGROUND);
            handlerThread2.start();
            mPartialResponseHandler = new Handler(handlerThread2.getLooper());
            mSequenceQueue.cancelAll(new SequenceRequestFilter());
            sUploadStatus = STATUS_IDLE;
            uploadFilter = new UploadRequestFilter();
            videoUploaderQueue.mVideoUploadQueue.cancelAll(uploadFilter);
            videoUploaderQueue.uploadTaskQueue.clear();

            if (videoUploaderQueue.progressListener != null) {
                videoUploaderQueue.progressListener.onUploadCancelled(getTotalSizeValue(), getRemainingSizeValue());
            }
            SequenceDB.instance.interruptUploading();
            SequenceDB.instance.fixStatuses();
            if (NetworkUtils.isWifiOn(mContext) || mWifiLock != null) {
                try {
                    if (mWifiLock != null) {
                        mWifiLock.release();
                        mWifiLock = null;
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public void pauseUpload() {
        videoUploaderQueue.pause();
    }

    public void resumeUpload() {
        final boolean dataSet = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
        if (isUploading()) {
            if (isPaused()) {
                if (NetworkUtils.isInternetAvailable(mContext)) {
                    if (dataSet || NetworkUtils.isWifiInternetAvailable(mContext)) {
                        videoUploaderQueue.resume();
                    }
                }
            }
        }
    }

    public void uploadCache(final Collection<LocalSequence> sequences) {
        if (sUploadStatus != STATUS_IDLE) {
            if (videoUploaderQueue.progressListener != null) {
                videoUploaderQueue.progressListener.onUploadCancelled(0, 0);
            }

            return;
        }
        sUploadStatus = STATUS_INDEXING;
        final List<LocalSequence> sequencesCopy = new ArrayList<>(sequences);
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("") || sequencesCopy.isEmpty()) {
            if (videoUploaderQueue.progressListener != null) {
                videoUploaderQueue.progressListener.onUploadCancelled(0, 0);
            }
            return;
        }
        videoUploaderQueue.mVideoUploadQueue.stop();
        mSequenceQueue.stop();
        runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalSequence.order(sequencesCopy);
                    Iterator<LocalSequence> iter = sequencesCopy.iterator();
                    while (iter.hasNext()) {
                        LocalSequence seq = iter.next();
                        if (SequenceDB.instance.getNumberOfFrames(seq.getId()) <= 0) {
                            iter.remove();
                        } else {
                            seq.setStatus(LocalSequence.STATUS_INDEXING);
                        }
                    }
                    videoUploaderQueue.initialize(sequencesCopy);
                    for (final LocalSequence sequence : sequencesCopy) {
                        if (Thread.interrupted()) {
                            Log.w(TAG, "uploadCache: interrupted");
                            return;
                        }
                        if (!sequence.isExternal() || Utils.checkSDCard(mContext)) {
                            createSequence(sequence);
                        }
                    }
                    if (Thread.interrupted()) {
                        Log.d(TAG, "createSequence: interrupted at hardcoded");
                        return;
                    }
                    commitNextSequence();
                } catch (Exception e) {
                    if (Fabric.isInitialized()) {
                        Crashlytics.logException(e);
                    }
                    Log.w(TAG, "uploadCache: " + Log.getStackTraceString(e));
                }
            }
        });
    }

    public void setUploadProgressListener(UploadProgressListener listener) {
        videoUploaderQueue.setUploadProgressListener(listener);
    }

    public boolean isUploading() {
        return sUploadStatus != STATUS_IDLE;
    }

    public boolean isPaused() {
        return sUploadStatus == STATUS_PAUSED;
    }

    public boolean isIndexing() {
        return sUploadStatus == STATUS_INDEXING;
    }

    public long getRemainingSizeValue() {
        long number = 0;
        for (final LocalSequence sequence : videoUploaderQueue.mSequences) {
            number = number + sequence.getSize();
        }
        return number;
    }

    public int getOriginalSequencesNumber() {
        return videoUploaderQueue.mSequences.size();
    }

    private long getTotalSizeValue() {
        if (videoUploaderQueue.mTotalSize == 0) {
            long number = 0;
            for (final LocalSequence sequence : videoUploaderQueue.mSequences) {
                number = number + sequence.getOriginalSize();
            }
            videoUploaderQueue.mTotalSize = number;
        }
        return videoUploaderQueue.mTotalSize;
    }

    public void resetUploadStats() {
        videoUploaderQueue.mTotalSize = 0;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLoginChanged(LoginChangedEvent event) {
        mAccessToken = null;
    }

    @Override
    void runInBackground(Runnable runnable) {
        if (mBackgroundHandler == null
                || mBackgroundHandler.getLooper() == null
                || !mBackgroundHandler.getLooper().getThread().isAlive()) {
            mHandlerThread = new HandlerThread("BackgroundUpload", Process.THREAD_PRIORITY_BACKGROUND);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            Log.d(TAG, "runInBackground: new thread starting");
        }
        mBackgroundHandler.post(runnable);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void requestFinished(int status, ApiResponse response) {
        Log.d(TAG, "requestFinished: " + response);
    }

    @Override
    public void requestFailed(int status, ApiResponse response) {
        Log.d(TAG, "requestFailed: " + response);
    }

    private class VideoUploaderQueue implements RequestQueue.RequestFinishedListener<Object> {

        /**
         * upload progress listener
         */
        UploadProgressListener progressListener;

        /**
         * separate upload request queue, for progress checking
         */
        private ConcurrentLinkedQueue<StringRequest> uploadTaskQueue = new ConcurrentLinkedQueue<>();

        private ConcurrentLinkedQueue<StringRequest> tempTaskQueue = new ConcurrentLinkedQueue<>();

        private RequestQueue mVideoUploadQueue;

        private long mTotalSize = 0;

        private ConcurrentLinkedQueue<LocalSequence> mSequences = new ConcurrentLinkedQueue<>();

        VideoUploaderQueue(Context context) {
            mVideoUploadQueue = newRequestQueue(context, 1);
            mVideoUploadQueue.addRequestFinishedListener(this);
            mVideoUploadQueue.stop();
        }

        void add(StringRequest request) {
            if (sUploadStatus == STATUS_PAUSED) {
                Log.d(TAG, "add: adding request to tempTaskQueue");
                tempTaskQueue.add(request);
            } else {
                Log.d(TAG, "add: adding request to live uploadQueue");
                uploadTaskQueue.add(request);
                mVideoUploadQueue.add(request);
            }
        }

        void initialize(Collection<LocalSequence> sequences) {
            if (NetworkUtils.isWifiOn(mContext)) {
                WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (mWifiLock == null) {
                    mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Upload-wifi-lock");
                }
                mWifiLock.acquire();
            }
            mTotalSize = 0;
            mSequences.clear();
            mSequences.addAll(sequences);
            boolean thereIsSafe = false;
            for (final LocalSequence sequence : sequences) {
                thereIsSafe = thereIsSafe || sequence.isSafe();
                int number = (int) SequenceDB.instance.getNumberOfVideos(sequence.getId());
                mTotalSize = mTotalSize + sequence.getOriginalSize();
                sequence.setVideoCount(number);
            }
            if (mVideoUploadQueue != null) {
                mVideoUploadQueue.removeRequestFinishedListener(this);
            }
            Log.d(TAG, "initialize: multithreaded upload mode = " + thereIsSafe);
            mVideoUploadQueue = newRequestQueue(mContext, thereIsSafe ? 4 : 1);
            mVideoUploadQueue.addRequestFinishedListener(this);
            mVideoUploadQueue.stop();

            Log.d(TAG, "initialize: mTotalSize = " + mTotalSize);

            if (sUploadStatus == STATUS_IDLE && progressListener != null) {
                progressListener.onUploadStarted(mTotalSize, mSequences.size());
            }
        }

        void commit() {
            if (!uploadTaskQueue.isEmpty()) {
                if (progressListener != null) {
                    progressListener.onIndexingFinished();
                }
                sUploadStatus = STATUS_UPLOADING;
                Log.d(TAG, "commitImages: " + uploadTaskQueue.size() + " images" +
                        "  ------------------------------------------------------------------------------------------------------------"
                        + "------------------------------------------------------------------------------------------------------------"
                        + "------------------------------------------------------------------------------------------------------------");
                ConnectionClassManager.getInstance().reset();
                ConnectionClassManager.getInstance().register(progressListener);
                DeviceBandwidthSampler.getInstance().startSampling();
                mVideoUploadQueue.start();
            } else {
                commitNextSequence();
            }
        }

        void pause() {
            Log.d(TAG, "pause: called upload pause");
            if (uploadTaskQueue.size() > 0 && sUploadStatus != STATUS_IDLE && sUploadStatus != STATUS_INDEXING) {
                tempTaskQueue.clear();
                tempTaskQueue.addAll(uploadTaskQueue);
                Log.d(TAG, "pause: saving " + tempTaskQueue.size() + " requests");
                mVideoUploadQueue.stop();
                uploadFilter = new UploadRequestFilter();
                mVideoUploadQueue.cancelAll(uploadFilter);
                uploadTaskQueue.clear();
                mVideoUploadQueue.stop();
                Log.d(TAG, "pause: upload paused");
                int previousStatus = sUploadStatus;
                sUploadStatus = STATUS_PAUSED;
                if (progressListener != null && previousStatus != STATUS_PAUSED) {
                    progressListener.onUploadPaused();
                }
            }
        }

        void resume() {
            Log.d(TAG, "resume: called upload resume");
            uploadTaskQueue.clear();
            if ((uploadTaskQueue.size() > 0 || tempTaskQueue.size() > 0) && sUploadStatus == STATUS_PAUSED) {
                for (StringRequest req : tempTaskQueue) {
                    if (req.isCanceled()) {
                        req.reInit();
                    }
                    req.reInit();
                    mVideoUploadQueue.add(req);
                    uploadTaskQueue.add(req);
                }
                tempTaskQueue.clear();
                Log.d(TAG, "resume: continuing " + uploadTaskQueue.size() + " requests");
                mVideoUploadQueue.start();
                Log.d(TAG, "resume: upload resumed");
                sUploadStatus = STATUS_UPLOADING;
                if (progressListener != null) {
                    progressListener.onUploadResumed();
                }
            }
        }

        void setUploadProgressListener(UploadProgressListener listener) {
            progressListener = listener;
            if (progressListener != null) {
                if (sUploadStatus != STATUS_IDLE) {
                    progressListener.onUploadStarted(getTotalSizeValue(), mCreateQueue.size());
                    progressListener.onProgressChanged(Math.max(getTotalSizeValue(), 1), getRemainingSizeValue());
                }
            }
        }

        @Override
        public void onRequestFinished(Request<Object> request) {
            if (request instanceof VideoRequest || request instanceof PhotoRequest) {
                Log.d(TAG, "onRequestFinished: for video or photo file");
                //noinspection SuspiciousMethodCalls
                uploadTaskQueue.remove(request);
            }
        }

        void markDone(final LocalSequence sequence) {
            if (sUploadStatus == STATUS_IDLE) {
                cancelUploadTasks();
                return;
            }
            if (sUploadStatus == STATUS_PAUSED) {
                pause();
                return;
            }
            if (progressListener != null) {
                progressListener.onImageUploaded(sequence);
            }

            try {
                sequence.setFrameCount((int) SequenceDB.instance.getNumberOfFrames(sequence.getId()));
            } catch (Exception e) {
                Log.d(TAG, "markDone: " + Log.getStackTraceString(e));
            }
            sequence.decreaseVideoCount();

            long totalSize = getTotalSizeValue();
            final long remainingSize = getRemainingSizeValue();

            Log.d(TAG, "markDone: " + " imageCount " + sequence.getFrameCount());
            if (sequence.getFrameCount() <= 0) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        finishSequence(sequence, new NetworkResponseDataListener<ApiResponse>() {
                            @Override
                            public void requestFailed(int status, ApiResponse details) {
                                Log.d(TAG, "finishSequence: failed ");
                            }

                            @Override
                            public void requestFinished(int status, ApiResponse details) {
                                Log.w(TAG, "finishSequence: success ");
                            }
                        });
                    }
                });
                LocalSequence.deleteSequence(sequence.getId());
                if (progressListener != null) {
                    progressListener.onSequenceUploaded(sequence);
                }
                EventBus.postSticky(new SequencesChangedEvent(false, sequence.getId()));
            }
            if (progressListener != null) {
                progressListener.onProgressChanged(Math.max(totalSize, 1), remainingSize);
            }
            if (uploadTaskQueue.isEmpty() && mCreateQueue.isEmpty()) {
                finishUpload();
            } else if (uploadTaskQueue.isEmpty()) {
                synchronized (mCreateQueue) {
                    commitNextSequence();
                }
            }
        }

        @SuppressWarnings("unused")
        private void partialProgressChanged(long totalSent, long fileSize) {
            long totalSize = getTotalSizeValue();
            final long remainingSize = getRemainingSizeValue();
            if (progressListener != null && sUploadStatus != STATUS_IDLE) {
//                final int progress = (int) (((totalSize - (remainingSize - totalSent)) * 100) / totalSize);
//                Log.d(TAG, "partialProgressChanged: totalSize: " + totalSize + ", remainingSize: " + remainingSize + ", totalSent: " + totalSent + ", percentage is " + progress);
                progressListener.onProgressChanged(Math.max(totalSize, 1), remainingSize - totalSent);
            }
        }

        private void finishUpload() {
            Log.d(TAG, "finishUpload: finishing");
            if (progressListener != null) {
                progressListener.onProgressChanged(Math.max(getTotalSizeValue(), 1), 0);
                progressListener.onUploadFinished();
            }
            DeviceBandwidthSampler.getInstance().stopSampling();
            sUploadStatus = STATUS_IDLE;
            mVideoUploadQueue.stop();
            try {
                if (NetworkUtils.isWifiOn(mContext) || mWifiLock != null) {
                    if (mWifiLock != null) {
                        mWifiLock.release();
                        mWifiLock = null;
                    }
                }
            } catch (Exception ignored) {}
            SequenceDB.instance.fixStatuses();
            ((OSVApplication) mContext.getApplicationContext()).consistencyCheck();
        }
    }
}
