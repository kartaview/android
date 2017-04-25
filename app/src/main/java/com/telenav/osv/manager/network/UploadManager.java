package com.telenav.osv.manager.network;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.http.AuthRequest;
import com.telenav.osv.http.DeleteSequenceRequest;
import com.telenav.osv.http.LeaderboardRequest;
import com.telenav.osv.http.LeaderboardRequestFilter;
import com.telenav.osv.http.ListPhotosRequest;
import com.telenav.osv.http.ListRequestFilter;
import com.telenav.osv.http.ListSequencesRequest;
import com.telenav.osv.http.ListTracksRequest;
import com.telenav.osv.http.NearbyRequest;
import com.telenav.osv.http.NearbyRequestFilter;
import com.telenav.osv.http.PhotoRequest;
import com.telenav.osv.http.ProfileRequest;
import com.telenav.osv.http.ProgressiveEntity;
import com.telenav.osv.http.RequestListener;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.http.SequenceFinishedRequest;
import com.telenav.osv.http.SequenceRequest;
import com.telenav.osv.http.SequenceRequestFilter;
import com.telenav.osv.http.UploadRequestFilter;
import com.telenav.osv.http.VersionRequest;
import com.telenav.osv.http.VideoRequest;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.LoadAllSequencesListener;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;

/**
 * *
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class UploadManager implements Response.ErrorListener {

    /**
     * test environment or production,
     * if production, should be empty
     */
    public static final String[] URL_ENV = {"openstreetview.com/", "staging.openstreetview.com/", "testing.openstreetview.com/", "beta.openstreetcam.org/"};

    public static final int STATUS_IDLE = 0;
//    public static final String URL_VER = "";

    public static final int STATUS_PAUSED = 3;

    private static final int STATUS_INDEXING = 1;

    private static final int STATUS_UPLOADING = 2;

    /**
     * version number, when it will be added to backend
     */
    private static final String URL_VER = "1.0/";


    private static final String TAG = "UploadManager";

    private static final int API_ERROR_SEQUENCE_ID_OUT_OF_BOUNDS = 612;

    private static final int TRACKS_IPP = 100000;

    private static final int UPLOAD_REQUEST_TIMEOUT = 30000;

    /**
     * download photo file reques url
     */
    public static String URL_DOWNLOAD_PHOTO = "http://" + "&&";

    public static int sUploadStatus = STATUS_IDLE;

    static String URL_AUTH_OSM = "http://" + "&&" + "auth/openstreetmap/client_auth";

    static String URL_AUTH_GOOGLE = "http://" + "&&" + "auth/google/client_auth";

    static String URL_AUTH_FACEBOOK = "http://" + "&&" + "auth/facebook/client_auth";

    /**
     * create sequence request url
     */
    private static String URL_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/";

    /**
     * delete sequence request url
     */
    private static String URL_DELETE_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/remove/";

    private static String URL_VIDEO = "http://" + "&&" + URL_VER + "video/";

    private static String URL_PHOTO = "http://" + "&&" + URL_VER + "photo/";

    /**
     * delete photo request url
     */
    private static String URL_DELETE_PHOTO = "http://" + "&&" + URL_VER + "photo/remove/";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_SEQUENCES = "http://" + "&&" + URL_VER + "list/";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_TRACKS = "http://" + "&&" + URL_VER + "tracks/";

    /**
     * list ALL sequences request url
     */
    private static String URL_NEARBY_TRACKS = "http://" + "&&" + "nearby-tracks";

    /**
     * list ALL sequences request url
     */
    private static String URL_VERSION = "http://" + "&&" + "version";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_MY_SEQUENCES = "http://" + "&&" + URL_VER + "list/my-list/";

    /**
     * list photos from a specific sequence url
     */
    private static String URL_LIST_PHOTOS = "http://" + "&&" + URL_VER + "sequence/photo-list/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_PROFILE_DETAILS = "http://" + "&&" + URL_VER + "user/details/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_LEADERBOARD = "http://" + "&&" + "gm-leaderboard";

    /**
     * finish a sequence upload and mark for processing
     */
    private static String URL_FINISH_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/finished-uploading/";

    private final ConcurrentLinkedQueue<SequenceRequest> mCreateQueue = new ConcurrentLinkedQueue<>();

    /**
     * context used for operations, should use application context
     */
    private final Context mContext;

    /**
     * request queue for operations
     * adding a request here will be automatically run in the next available time
     */
    private final RequestQueue mQueue;

    /**
     * request queue for operations
     * adding a request here will be automatically run in the next available time
     */
    private final RequestQueue mSequenceQueue;

    private final ApplicationPreferences appPrefs;

    private final RequestQueue mListQueue;

    public int mCurrentServer = 0;

    /**
     * filter used for filtering only image upload requests
     */
    private UploadRequestFilter uploadFilter;

    /**
     * class used for managing upload tasks
     */
    private VideoUploaderQueue videoUploaderQueue;

    private Handler mBackgroundHandler;

    private HandlerThread mHandlerThread;
    private HandlerThread mQueueThread;

    private Handler mTracksHandler;

    private String mAccessToken;

    private Handler mPartialResponseHandler;

    private WifiManager.WifiLock mWifiLock;

    public UploadManager(Context context) {
        this.mContext = context;
        mQueueThread = new HandlerThread("QueueThread", Thread.NORM_PRIORITY);
        mQueueThread.start();
        mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        HandlerThread handlerThread = new HandlerThread("Tracks", Thread.NORM_PRIORITY);
        handlerThread.start();
        mTracksHandler = new Handler(handlerThread.getLooper());
        HandlerThread handlerThread2 = new HandlerThread("PartialResponse", Thread.NORM_PRIORITY);
        handlerThread2.start();
        mPartialResponseHandler = new Handler(handlerThread2.getLooper());
        this.mQueue = newRequestQueue(mContext, 4);
        this.mListQueue = newRequestQueue(mContext, 4);
        this.mSequenceQueue = newRequestQueue(mContext, 1);
        mSequenceQueue.stop();
        this.videoUploaderQueue = new VideoUploaderQueue(mContext);
        appPrefs = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs();
        mCurrentServer = appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE);
        setEnvironment();
        EventBus.register(this);
        VolleyLog.DEBUG = Utils.isDebugEnabled(mContext);
    }

    private void setEnvironment() {
        if (!Utils.isDebugBuild(mContext) && !appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_ENABLED)) {
            mCurrentServer = 0;
        }
        URL_DELETE_PHOTO = URL_DELETE_PHOTO.replace("&&", URL_ENV[mCurrentServer]);
        URL_DELETE_SEQUENCE = URL_DELETE_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_MY_SEQUENCES = URL_LIST_MY_SEQUENCES.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_SEQUENCES = URL_LIST_SEQUENCES.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_PHOTOS = URL_LIST_PHOTOS.replace("&&", URL_ENV[mCurrentServer]);
        URL_SEQUENCE = URL_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        URL_VIDEO = URL_VIDEO.replace("&&", URL_ENV[mCurrentServer]);
        URL_PHOTO = URL_PHOTO.replace("&&", URL_ENV[mCurrentServer]);
        URL_DOWNLOAD_PHOTO = URL_DOWNLOAD_PHOTO.replace("&&", URL_ENV[mCurrentServer]);
        URL_FINISH_SEQUENCE = URL_FINISH_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_TRACKS = URL_LIST_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_NEARBY_TRACKS = URL_NEARBY_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_PROFILE_DETAILS = URL_PROFILE_DETAILS.replace("&&", URL_ENV[mCurrentServer]);
        URL_LEADERBOARD = URL_LEADERBOARD.replace("&&", URL_ENV[mCurrentServer]);
        URL_VERSION = URL_VERSION.replace("&&", URL_ENV[mCurrentServer]);
        URL_AUTH_OSM = URL_AUTH_OSM.replace("&&", URL_ENV[mCurrentServer]);
        URL_AUTH_FACEBOOK = URL_AUTH_FACEBOOK.replace("&&", URL_ENV[mCurrentServer]);
        URL_AUTH_GOOGLE = URL_AUTH_GOOGLE.replace("&&", URL_ENV[mCurrentServer]);
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
    private void uploadVideo(final Sequence sequence, final OSVFile video, final int onlineSequenceID, final int sequenceIndex, final RequestListener listener) {
        if (!video.exists()) {
            Log.w(TAG, "uploadVideo: file doesn't exist: " + video.getPath());
            return;
        }
        final VideoRequest imageUploadReq = new VideoRequest(URL_VIDEO, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "uploadVideo: error uploading video: " + onlineSequenceID + "/" + video.getName(), error);
                        int apiCode = 0;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.e(TAG, "uploadVideo: " + new String(error.networkResponse.data));
                                JSONObject ob = new JSONObject(new String(error.networkResponse.data));
                                apiCode = ob.getJSONObject("status").getInt("apiCode");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(TAG, "uploadVideo: " + error.getLocalizedMessage());
                        }
                        if (apiCode == 660) {
                            sequence.size = sequence.size - Utils.fileSize(video);
                            SequenceDB.instance.deleteVideo(video, sequence.sequenceId, sequenceIndex);
                            videoUploaderQueue.markDone(sequence);
                            listener.requestFinished(RequestListener.STATUS_SUCCESS_IMAGE);
                        } else {
                            if (apiCode == API_ERROR_SEQUENCE_ID_OUT_OF_BOUNDS && videoUploaderQueue.uploadTaskQueue.size() > 0) {
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
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "uploadVideo: success, entering background to delete file");
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "uploadVideo: video uploaded successfully: " + onlineSequenceID + "/" + video.getName());
                        sequence.size = sequence.size - Utils.fileSize(video);
                        SequenceDB.instance.deleteVideo(video, sequence.sequenceId, sequenceIndex);
                        videoUploaderQueue.markDone(sequence);
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_IMAGE);
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
    private void uploadImage(final Sequence sequence, final OSVFile image, final int onlineSequenceID, final int sequenceIndex, final double lat, final double lon, final int
            acc, final RequestListener listener) {
        if (!image.exists()) {
            Log.w(TAG, "uploadImage: file doesn't exist: " + image.getPath());
            return;
        }
        final PhotoRequest imageUploadReq = new PhotoRequest(URL_PHOTO, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "uploadImage: error uploading image: " + onlineSequenceID + "/" + image.getName(), error);
                        int apiCode = 0;
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.e(TAG, "uploadImage: " + new String(error.networkResponse.data));
                                JSONObject ob = new JSONObject(new String(error.networkResponse.data));
                                apiCode = ob.getJSONObject("status").getInt("apiCode");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.w(TAG, "uploadImage: " + error.getLocalizedMessage());
                        }
                        if (apiCode == 660) {
                            sequence.size = sequence.size - Utils.fileSize(image);
                            SequenceDB.instance.deletePhoto(image, sequence.sequenceId, sequenceIndex);
                            videoUploaderQueue.markDone(sequence);
                            listener.requestFinished(RequestListener.STATUS_SUCCESS_IMAGE);
                        } else {
                            if (apiCode == API_ERROR_SEQUENCE_ID_OUT_OF_BOUNDS && videoUploaderQueue.uploadTaskQueue.size() > 0) {
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
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "uploadImage: success, entering background to delete file");
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "uploadImage: image uploaded successfully: " + onlineSequenceID + "/" + image.getName());
                        sequence.size = sequence.size - Utils.fileSize(image);
                        SequenceDB.instance.deletePhoto(image, sequence.sequenceId, sequenceIndex);
                        videoUploaderQueue.markDone(sequence);
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_IMAGE);
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
     * @param listener resuest listener
     */
    private void createSequence(final Sequence sequence, final RequestListener listener) {
        final int onlineId = SequenceDB.instance.getOnlineId(sequence.sequenceId);
        OSVFile metafile = new OSVFile(sequence.folder, "track.txt.gz");
        if (!metafile.exists()) {
            metafile = new OSVFile(sequence.folder, "track.txt");
        }
        final OSVFile finalMetafile = metafile;
        Log.d(TAG, "createSequence: creating sequence request");
        if (onlineId == -1) {
            Cursor cursor = SequenceDB.instance.getFrames(sequence.sequenceId);
            if (cursor != null && cursor.getCount() > 0) {
                String position = "" + cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LAT)) + "," + cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LON));
                final SequenceRequest seqRequest = new SequenceRequest(URL_SEQUENCE, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                if (error.networkResponse != null && error.networkResponse.data != null && error.networkResponse.data.length > 0) {
                                    try {
                                        Log.w(TAG, "createSequence: " + new String(error.networkResponse.data));
                                        JSONObject ob = new JSONObject(new String(error.networkResponse.data));
                                        Log.d(TAG, ob.toString());
                                        Toast.makeText(mContext, R.string.error_creating_sequence, Toast.LENGTH_SHORT).show();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Log.d(TAG, "" + error);
                                }
                                listener.requestFinished(RequestListener.STATUS_FAILED);
                                if (Thread.interrupted()) {
                                    Log.w(TAG, "createSequence: interrupted at error");
                                    return;
                                }
                                createSequence(sequence, listener);
                                commitNextSequence();
                            }
                        });

                    }
                }, new Response.Listener<String>() {
                    @Override
                    public void onResponse(final String response) {
                        runInBackground(new Runnable() {
                            @Override
                            public void run() {
                                int sequenceID;
                                try {

                                    JSONObject jsonObject;
                                    try {
                                        jsonObject = new JSONObject(response);
                                        sequenceID = jsonObject.getJSONObject("osv").getJSONObject("sequence").getInt("id");
                                        SequenceDB.instance.updateSequenceOnlineId(sequence.sequenceId, sequenceID);
                                        sequence.onlineSequenceId = sequenceID;
                                        if (finalMetafile.exists()) {
                                            sequence.size = sequence.size - Utils.fileSize(finalMetafile);
                                            finalMetafile.delete();
                                        }
                                        uploadSequence(sequence, sequenceID, listener);
                                        listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Log.d(TAG, "createSequence" + response);
                                } catch (Exception e) {
                                    Log.w(TAG, "createSequence: " + e.getLocalizedMessage());
                                }
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
                }, sequence.sequenceId, getAccessToken(), position, metafile, sequence.appVersion, sequence.obd, sequence.score, sequence.getFlatScoreDetails(),
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
            sequence.onlineSequenceId = onlineId;
            SequenceRequest seqReq = new SequenceRequest("fail_on_purpose", new Response.ErrorListener() {
                @Override
                public void onErrorResponse(final VolleyError error) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            if (finalMetafile.exists()) {
                                sequence.size = sequence.size - Utils.fileSize(finalMetafile);
                                finalMetafile.delete();
                            }
                            uploadSequence(sequence, onlineId, listener);
                            listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE);
                            if (Thread.interrupted()) {
                                Log.w(TAG, "createSequence: interrupted at hardcoded");
                            }

                        }
                    });
                }
            }, new Response.Listener<String>() {
                @Override
                public void onResponse(final String response) {
                    runInBackground(new Runnable() {
                        @Override
                        public void run() {
                            uploadSequence(sequence, onlineId, listener);
                            listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE);
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
            }, sequence.sequenceId, getAccessToken(), "", null, sequence.appVersion, sequence.obd, sequence.score, sequence.getFlatScoreDetails(), mPartialResponseHandler);
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
            return;
        }
        videoUploaderQueue.progressListener.onUploadingMetadata();
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
     * lists a certain amount of sequences from the server from the given page
     * @param listener request listener
     * @param pageNumber number of the page
     * @param itemsPerPage number of items per page
     */
    public void listSequences(final RequestResponseListener listener, int pageNumber, int itemsPerPage) {
        ListSequencesRequest seqRequest = new ListSequencesRequest(URL_LIST_MY_SEQUENCES, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "listSequences: " + new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_LIST_SEQUENCE, response);
                        Log.d(TAG, "listSequences: successful");
                    }
                });
            }
        }, getAccessToken(), pageNumber, itemsPerPage);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(3500, 5, 1f));
        seqRequest.setShouldCache(false);
        cancelListTasks();
        mListQueue.add(seqRequest);
    }

    public void nearby(final RequestResponseListener listener, String lat, String lon, int radius) {
        NearbyRequest seqRequest = new NearbyRequest(URL_NEARBY_TRACKS, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "nearby: " + new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (error instanceof TimeoutError) {
                            Log.w(TAG, "nearby: Timed out");
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_NEARBY, response);
                        Log.d(TAG, "nearby: successful");
                    }
                });
            }
        }, lat, lon, radius);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(18000, 1, 1f));
        mQueue.add(seqRequest);
    }

    public void version(final RequestResponseListener listener) {
        VersionRequest seqRequest = new VersionRequest(URL_VERSION, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "version: " + new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_NEARBY, response);
                        Log.d(TAG, "version: successful");
                    }
                });
            }
        });
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 2, 1f));
        cancelListTasks();
        mQueue.add(seqRequest);
    }


    /**
     * lists all existing sequences from the server, no images are downloaded, only details
     * @param listener request listener
     * @param zoom zoom level
     */
    public void listSegments(final LoadAllSequencesListener listener, final SKCoordinate position, final String upperLeft, final String lowerRight, final int page, final float
            zoom) {
        Log.d(TAG, "listSegments: starting for page " + page);
        ListTracksRequest seqRequest = new ListTracksRequest(URL_LIST_TRACKS, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "listSegments: error " + error + ", details : "+ error.getLocalizedMessage());
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "listSegments: " + new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.onRequestFailed();
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String result) {
                Log.d(TAG, "listSegments: onResponse: tracks response finished");
                try {
                    if (result != null && !result.isEmpty()) {
                        JSONObject obj = new JSONObject(result);
                        final JSONArray tracks = obj.getJSONArray("currentPageItems");
                        final int totalTracks = obj.getJSONArray("totalFilteredItems").getInt(0);
                        if (page * TRACKS_IPP < totalTracks) {
                            int newPage = page + 1;
                            listSegments(listener, position, upperLeft, lowerRight, newPage, zoom);
                        }
                        mTracksHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Polyline bestPolyline = null;
                                double bestDistance = Double.MAX_VALUE;
                                Log.d(TAG, "tracks: handler processing");
                                try {
                                    final int alreadyDisplayed = page * TRACKS_IPP;
                                    if (tracks == null) {
                                        listener.onFinished(null);
                                        return;
                                    }
                                    listener.onRequestSuccess();
                                    Log.d(TAG, "listSegments:  number of segments = " + tracks.length());
//                                    if (page == 1) {
//                                        listener.onRequestFinished(null, -1);
//                                    }
                                    Log.d(TAG, "onResponse: totaltracks = " + totalTracks + " , page = " + page + " , already displayed = " + alreadyDisplayed);
                                    if (tracks.length() > 0) {
                                        for (int i = alreadyDisplayed; i < alreadyDisplayed + tracks.length(); i++) {
                                            JSONArray track = tracks.getJSONObject(i - alreadyDisplayed).getJSONArray("track");
                                            int coverage = -1;
                                            try {
                                                coverage = tracks.getJSONObject(i - alreadyDisplayed).getInt("coverage");
                                            } catch (Exception ignored) {}
                                            final Polyline polyline = new Polyline(i);
                                            polyline.coverage = coverage;
                                            SKCoordinate start = null, end = null;
                                            for (int j = 0; j < track.length(); j++) {
                                                if (Thread.interrupted()) {
                                                    return;
                                                }
                                                double lat = track.getJSONArray(j).getDouble(0);
                                                double lon = track.getJSONArray(j).getDouble(1);
                                                end = new ImageCoordinate(lat, lon, j);
                                                if (start == null) {
                                                    start = end;
                                                }
                                                polyline.getNodes().add(end);
                                            }
                                            if (position != null) {
                                                double distanceToPos = ComputingDistance.getDistanceFromLine(position, start, end);
                                                if (distanceToPos < bestDistance) {
                                                    bestDistance = distanceToPos;
                                                    bestPolyline = polyline;
                                                }
                                            }
                                            listener.onRequestFinished(polyline, i);
                                        }
                                    }

                                    if (bestDistance > 15) {
                                        bestPolyline = null;
                                    }
                                    listener.onFinished(bestPolyline);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    listener.onFinished(null);
                                }
                            }
                        });

                    } else {
                        listener.onFinished(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onFinished(null);
                }
            }
        }, upperLeft, lowerRight, page, TRACKS_IPP, zoom);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(10000,3,1f));
        mListQueue.add(seqRequest);
    }

    /**
     * finishes a sequence upload
     * @param sequence sequence
     * @param listener listener
     */
    private void finishSequence(final Sequence sequence, final RequestListener listener) {
        SequenceFinishedRequest seqRequest = new SequenceFinishedRequest(URL_FINISH_SEQUENCE, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.w(TAG, "finishSequence: error " + error.getLocalizedMessage());
//                        listener.requestFinished(RequestListener.STATUS_FAILED);
                        finishSequence(sequence, listener);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE_FINISHED);
                        Log.d(TAG, "finishSequence: " + response);
                    }
                });
            }
        }, getAccessToken(), "" + sequence.onlineSequenceId);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(UPLOAD_REQUEST_TIMEOUT, 0, 1f));
        mQueue.add(seqRequest);
    }


    /**
     * deletes a sequence from the server, together with the images that it contains
     * @param sequenceId online id of the sequence
     * @param listener request listener
     */
    public void deleteSequence(final int sequenceId, final RequestListener listener) {
        DeleteSequenceRequest seqRequest = new DeleteSequenceRequest(URL_DELETE_SEQUENCE, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String response = new String(error.networkResponse.data);
                            Log.w(TAG, "deleteSequenceRecord: " + response);
                        }
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_DELETE_SEQUENCE);
                        Log.d(TAG, "deleteSequenceRecord: " + response);
                    }
                });
            }
        }, sequenceId, getAccessToken());
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 5, 1f));
        mQueue.add(seqRequest);
    }

    /**
     * uploads a sequence folder, containing images
     * @param sequence the sequence folder
     * @param sequenceIdOnline id
     * @param listener request listener
     */
    private void uploadSequence(Sequence sequence, final int sequenceIdOnline, RequestListener listener) {
        if (videoUploaderQueue.progressListener != null) {
            videoUploaderQueue.progressListener.onIndexingSequence(sequence, mCreateQueue.size());
        }
        if (sequence.safe) {
            Cursor cursor = SequenceDB.instance.getFrames(sequence.sequenceId);
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
                sequence.setStatus(Sequence.STATUS_UPLOADING);
            }
            if (cursor != null) {
                cursor.close();
            }
        } else {
            Cursor cursor = SequenceDB.instance.getVideos(sequence.sequenceId);
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
                sequence.setStatus(Sequence.STATUS_UPLOADING);
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        //requests are done, start uploading
        mSequenceQueue.stop();
        videoUploaderQueue.commit();
    }

    /**
     * error listener for volley
     * @param error error thrown
     */
    @Override
    public void onErrorResponse(VolleyError error) {
        try {
            JSONObject obj = new JSONObject(new String(error.networkResponse.data));
            Log.e(TAG, "onErrorResponse" + obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getProfileDetails(final RequestResponseListener listener) {
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("")) {
            listener.requestFinished(RequestListener.STATUS_FAILED, mContext.getString(R.string.not_logged_in));
            return;
        }
        ProfileRequest profileRequest = new ProfileRequest(URL_PROFILE_DETAILS, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "getProfileDetails: " + new String(error.networkResponse.data));
                                listener.requestFinished(RequestListener.STATUS_FAILED, new String(error.networkResponse.data));
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_PROFILE_DETAILS, response);
                        Log.d(TAG, "getProfileDetails: success");
                    }
                });
            }
        }, userName);
        mQueue.add(profileRequest);
    }

    public void getLeaderboardData(final RequestResponseListener listener, String date, String countryCode) {
        LeaderboardRequest leaderboardRequest = new LeaderboardRequest(URL_LEADERBOARD, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "getProfileDetails: " + new String(error.networkResponse.data));
                                listener.requestFinished(RequestListener.STATUS_FAILED, new String(error.networkResponse.data));
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_LEADERBOARD, response);
                        Log.d(TAG, "getProfileDetails: success");
                    }
                });
            }
        }, date, countryCode, null);
        mQueue.cancelAll(new LeaderboardRequestFilter());
        mQueue.add(leaderboardRequest);
    }

    /**
     * lists the details of the images in an online sequence
     * @param sequenceId online sequence id
     * @param listener request listener
     */
    public void listImages(final int sequenceId, final RequestResponseListener listener) {
        ListPhotosRequest seqRequest = new ListPhotosRequest(URL_LIST_PHOTOS, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "listImages: " + new String(error.networkResponse.data));
                                listener.requestFinished(RequestListener.STATUS_FAILED, new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_LIST_IMAGES, response);
                        Log.d(TAG, "listImages: success");
                    }
                });
            }
        }, sequenceId, getAccessToken());
        mQueue.add(seqRequest);
    }

    /**
     * lists the details of the images in an online sequence
     * @param listener request listener
     */
    void authenticate(final String url, final String requestToken, final String secretToken, final RequestResponseListener listener) {
        Log.d(TAG, "authenticate: " + url);
        AuthRequest request = new AuthRequest(url, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.w(TAG, "authenticate: " + new String(error.networkResponse.data));
                                listener.requestFinished(RequestListener.STATUS_FAILED, new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.requestFinished(RequestListener.STATUS_FAILED);
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_LOGIN, response);
                        Log.d(TAG, "authenticate: success");
                    }
                });
            }
        }, requestToken, secretToken);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 3, 1f));
        mQueue.add(request);
    }

    private String getAccessToken() {
        if (mAccessToken == null) {
            mAccessToken = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);
        }
        return mAccessToken;
    }
//
//    /**
//     * m,ethod to sign the requests for future impl. of the backend
//     */
//    public void signRequest() {
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//        String token = prefs.getString("OSM_OAUTH_TOKEN", "");
//        String tokenSecret = prefs.getString("OSM_OAUTH_TOKEN_SECRET", "");
//        if (token.equals("") || tokenSecret.equals("")) {
//            Toast.makeText(mContext, R.string.login_to_see_online_warning, Toast.LENGTH_SHORT).show();
//            return;
//        }
//        consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET_KEY);
//
//        provider = new CommonsHttpOAuthProvider(
//                "https://www.openstreetmap.org/oauth/request_token",
//                "https://www.openstreetmap.org/oauth/access_token",
//                "https://www.openstreetmap.org/oauth/authorize");
//        provider.setOAuth10a(true);
//        consumer.setTokenWithSecret(token, tokenSecret);
////        consumer.sign(request); //
//    }

    private void runInBackground(Runnable runnable) {
        if (mBackgroundHandler == null
                || mBackgroundHandler.getLooper() == null
                || !mBackgroundHandler.getLooper().getThread().isAlive()) {
            mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            Log.d(TAG, "runInBackground: new thread starting");
        }
        mBackgroundHandler.post(runnable);
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
            } catch (Exception ignored){}
            try {
                mBackgroundHandler.removeCallbacksAndMessages(null);
                mBackgroundHandler.getLooper().getThread().interrupt();
            } catch (Exception ignored){}
            if (mHandlerThread != null){
                try {
                    mHandlerThread.quit();
                } catch (Exception ignored){}
            }
            mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            HandlerThread handlerThread2 = new HandlerThread("PartialResponse", Thread.NORM_PRIORITY);
            handlerThread2.start();
            mPartialResponseHandler = new Handler(handlerThread2.getLooper());
            mSequenceQueue.cancelAll(new SequenceRequestFilter());
            sUploadStatus = STATUS_IDLE;
            uploadFilter = new UploadRequestFilter();
            videoUploaderQueue.mVideoUploadQueue.cancelAll(uploadFilter);
            videoUploaderQueue.uploadTaskQueue.clear();
            videoUploaderQueue.progressListener.onUploadCancelled(getTotalSizeValue(), getRemainingSizeValue());
            SequenceDB.instance.interruptUploading();
            SequenceDB.instance.fixStatuses();
            if (NetworkUtils.isWifiOn(mContext)) {
                try {
                    if (mWifiLock != null) {
                        mWifiLock.release();
                    }
                } catch (Exception ignored){}
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

    /**
     * cancells all upload request added
     */
    private void cancelListTasks() {
        Log.d(TAG, "cancelListTasks: cancelled map list tasks and listSegments");
        ListRequestFilter listFilter = new ListRequestFilter();
        mListQueue.cancelAll(listFilter);
    }

    /**
     * cancells all upload request added
     */
    public void cancelNearby() {
        Log.d(TAG, "cancelListTasks: cancelled map list tasks");
        NearbyRequestFilter listFilter = new NearbyRequestFilter();
        mQueue.cancelAll(listFilter);
    }


    public void uploadCache(final RequestListener listener, final Collection<Sequence> sequences) {
        if (sUploadStatus != STATUS_IDLE) {
            if (listener != null) {
                listener.requestFinished(RequestListener.STATUS_FAILED);
            }
            return;
        }
        final List<Sequence> sequencesCopy = new ArrayList<>(sequences);
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("") || sequencesCopy.isEmpty()) {
            if (listener != null) {
                listener.requestFinished(RequestListener.STATUS_FAILED);
            }
            return;
        }
        videoUploaderQueue.mVideoUploadQueue.stop();
        mSequenceQueue.stop();
        runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    Sequence.order(sequencesCopy);
                    Iterator<Sequence> iter = sequencesCopy.iterator();
                    while (iter.hasNext()) {
                        Sequence seq = iter.next();
                        if (SequenceDB.instance.getNumberOfFrames(seq.sequenceId) <= 0) {
                            iter.remove();
                        } else {
                            seq.setStatus(Sequence.STATUS_INDEXING);
                        }
                    }
                    videoUploaderQueue.initialize(sequencesCopy);
                    for (final Sequence sequence : sequencesCopy) {
                        if (Thread.interrupted()) {
                            Log.w(TAG, "uploadCache: interrupted");
                            return;
                        }
                        if (!sequence.mIsExternal || Utils.checkSDCard(mContext)) {
                            if (listener == null) {
                                createSequence(sequence, new RequestListener() {
                                    @Override
                                    public void requestFinished(final int status) {
                                    }
                                });
                            } else {
                                createSequence(sequence, listener);
                            }
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

    public void consistencyCheck() {
        Log.d(TAG, "consistencyCheck: starting");
        try {
            OSVFile osv = Utils.generateOSVFolder(mContext);
            for (OSVFile folder : osv.listFiles()) {
                if (folder.getName().contains("&")) {
                    Log.d(TAG, "consistencyCheck: renaming " + folder.getName());
                    OSVFile file = new OSVFile(folder.getParentFile(), folder.getName().replace("&", ""));
                    folder.renameTo(file);
                    folder = file;
                    Log.d(TAG, "consistencyCheck: renamed to " + folder.getName());
                }
                OSVFile[] imgs = folder.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.contains(".jpg") || filename.contains(".mp4");
                    }
                });
                if (imgs.length == 0) {
                    folder.delete();
                } else {
                    for (OSVFile img : imgs) {
                        try {
                            if (img.getName().endsWith("tmp")) {
                                String seqId = folder.getName().split("_")[1];
                                String index = img.getName().split("\\.")[0];
                                SequenceDB.instance.deleteVideo(Integer.valueOf(seqId), Integer.valueOf(index));
                                img.delete();
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
                        }
                    }
                    if (folder.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            return filename.contains(".jpg") || filename.contains(".mp4");
                        }
                    }).length == 0) {
                        folder.delete();
                    } else if (Utils.isInternetAvailable(mContext)) {
                        //we check if the onlineSequenceId, stored on the device, exists on the server also
                        final int onlineSequenceId = SequenceDB.instance.getOnlineId(Integer.valueOf(folder.getName().split("_")[1]));
                        if (onlineSequenceId != -1) {

                            final int sequenceId = Integer.valueOf(folder.getName().split("_")[1]);
                            Cursor cursor = SequenceDB.instance.getFrames(Integer.valueOf(folder.getName().split("_")[1]));
                            if (cursor != null && cursor.getCount() > 0) {
                                listImages(onlineSequenceId, new RequestResponseListener() {
                                    @Override
                                    public void requestFinished(int status, String result) {
                                        if (status == STATUS_FAILED) {
                                            try {
                                                JSONObject ob = new JSONObject(result);
                                                int apiCode = ob.getJSONObject("status").getInt("apiCode");
                                                if (apiCode == API_ERROR_SEQUENCE_ID_OUT_OF_BOUNDS) {
                                                    int nrRowsAffected = SequenceDB.instance.resetOnlineSequenceId(onlineSequenceId);
                                                    Log.d(TAG, "consistencyCheck: rollback on sequence " + sequenceId + ", nr of rows affected: " + nrRowsAffected);
                                                }
                                            } catch (JSONException e) {
                                                Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
                                            }
                                        }
                                    }

                                    @Override
                                    public void requestFinished(int status) {

                                    }
                                });
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "consistencyCheck: " + e.getLocalizedMessage());
        }
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

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    private RequestQueue newRequestQueue(Context context, int nrOfThreads) {
        File cacheDir = new File(context.getCacheDir(), "volley");

//        String userAgent = "volley/0";
//        try {
//            String packageName = context.getPackageName();
//            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
//            userAgent = packageName + "/" + info.versionCode;
//        } catch (PackageManager.NameNotFoundException e) {
//            Log.d(TAG, "newRequestQueue: " + Log.getStackTraceString(e));
//        }
        HttpStack stack = new HurlStack();
        Network network = new BasicNetwork(stack);
        if (mQueueThread == null) {
            mQueueThread = new HandlerThread("QueueThread", Thread.NORM_PRIORITY);
        }
        Handler bh = new Handler(mQueueThread.getLooper());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, nrOfThreads, new ExecutorDelivery(bh));
        queue.start();

        return queue;
    }

    public long getRemainingSizeValue() {
        long number = 0;
        for (final Sequence sequence : videoUploaderQueue.mSequences) {
            number = number + sequence.size;
        }
        return number;
    }

    public int getOriginalSequencesNumber() {
        return videoUploaderQueue.mSequences.size();
    }

    private long getTotalSizeValue() {
        if (videoUploaderQueue.mTotalSize == 0) {
            long number = 0;
            for (final Sequence sequence : videoUploaderQueue.mSequences) {
                number = number + sequence.originalSize;
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

        private ConcurrentLinkedQueue<Sequence> mSequences = new ConcurrentLinkedQueue<>();

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

        void initialize(Collection<Sequence> sequences) {
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
            for (final Sequence sequence : sequences) {
                thereIsSafe = thereIsSafe || sequence.safe;
                int number = (int) SequenceDB.instance.getNumberOfVideos(sequence.sequenceId);
                mTotalSize = mTotalSize + sequence.originalSize;
                sequence.numberOfVideos = number;
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
            if (uploadTaskQueue.size() > 0) {
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
                sUploadStatus = STATUS_IDLE;
                if (progressListener != null) {
                    progressListener.onUploadCancelled(1, 0);
                }
                Log.w(TAG, "commitImages: empty upload queue - cancelled");
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
                    mVideoUploadQueue.add(req);
                    uploadTaskQueue.add(req);
                }
                tempTaskQueue.clear();
                Log.d(TAG, "pause: continuing " + uploadTaskQueue.size() + " requests");
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
                    if (progressListener != null) {
                        progressListener.onProgressChanged(Math.max(getTotalSizeValue(), 1), getRemainingSizeValue());
                    }
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

        void markDone(final Sequence sequence) {
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
                sequence.imageCount = (int) SequenceDB.instance.getNumberOfFrames(sequence.sequenceId);
            } catch (Exception e) {
                Log.d(TAG, "markDone: " + Log.getStackTraceString(e));
            }
            sequence.decreaseVideoCount();

            long totalSize = getTotalSizeValue();
            final long remainingSize = getRemainingSizeValue();

            Log.d(TAG, "markDone: " + " imageCount " + sequence.imageCount);
            if (sequence.imageCount <= 0) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        finishSequence(sequence, new RequestListener() {
                            @Override
                            public void requestFinished(int status) {
                                if (status == STATUS_FAILED) {
                                    Log.w(TAG, "finishSequence: failed ");
//                                    numberOfFailed++;
                                } else {
                                    Log.d(TAG, "finishSequence: success ");
//                                    numberOfSuccessful++;
                                }
                            }
                        });
                    }
                });
                Sequence.deleteSequence(sequence.sequenceId);
                if (progressListener != null) {
                    progressListener.onSequenceUploaded(sequence);
                }
                EventBus.postSticky(new SequencesChangedEvent(false, sequence.sequenceId));
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
                if (NetworkUtils.isWifiOn(mContext)) {
                    if (mWifiLock != null) {
                        mWifiLock.release();
                    }
                }
            } catch (Exception ignored) {}
            SequenceDB.instance.fixStatuses();
            consistencyCheck();
        }
    }
}
