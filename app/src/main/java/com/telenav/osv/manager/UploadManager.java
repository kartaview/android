package com.telenav.osv.manager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.ExecutorDelivery;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.facebook.network.connectionclass.ConnectionClassManager;
import com.facebook.network.connectionclass.DeviceBandwidthSampler;
import com.telenav.osv.R;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.http.AuthRequest;
import com.telenav.osv.http.DeleteImageRequest;
import com.telenav.osv.http.DeleteSequenceRequest;
import com.telenav.osv.http.ListPhotosRequest;
import com.telenav.osv.http.ListRequestFilter;
import com.telenav.osv.http.ListSequencesRequest;
import com.telenav.osv.http.ListTracksFilter;
import com.telenav.osv.http.ListTracksRequest;
import com.telenav.osv.http.NearbyRequest;
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
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.LoadAllSequencesListener;
import com.telenav.osv.listener.OAuthResultListener;
import com.telenav.osv.listener.UploadProgressListener;
import com.telenav.osv.ui.fragment.MapFragment;
import com.telenav.osv.ui.fragment.OAuthDialogFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.NetworkUtils;
import com.telenav.osv.utils.Utils;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

/**
 * Created by Kalman on 10/6/2015.
 */
public class UploadManager implements Response.ErrorListener {

    /**
     * test environment or production,
     * if production, should be empty
     */
    public static final String[] URL_ENV = {"openstreetview.com/", "staging.openstreetview.com/", "testing.openstreetview.com/"};

    /**
     * version number, when it will be added to backend
     */
    public static final String URL_VER = "1.0/";
//    public static final String URL_VER = "";

    /**
     * request url for login details of OSM user
     */
    public static final String URL_USER_DETAILS = "http://api.openstreetmap.org/api/0.6/user/details";

    public static final int STATUS_IDLE = 0;

    public static final int STATUS_INDEXING = 1;

    public static final int STATUS_UPLOADING = 2;

    public static final int STATUS_PAUSED = 3;

    /**
     * consumer key used for oauth 1.0a
     */
    private static final String CONSUMER_KEY = "rBWV8Eaottv44tXfdLofdNvVemHOL62Lsutpb9tw";

    /**
     * consumer secret key used for oauth 1.0a
     */
    private static final String CONSUMER_SECRET_KEY = "rpmeZIp49sEjjcz91X9dsY0vD1PpEduixuPy8T6S";

    private static final String TAG = "UploadManager";

    private static final int API_ERROR_SEQUENCE_ID_OUT_OF_BOUNDS = 612;

    private static final int TRACKS_IPP = 1000;

    private static final String ACCESS_TOKEN_KEY = "ACCESS_TOKEN";

    /**
     * create sequence request url
     */
    public static String URL_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/";

    /**
     * delete sequence request url
     */
    public static String URL_DELETE_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/remove/";

    public static String URL_VIDEO = "http://" + "&&" + URL_VER + "video/";

    /**
     * delete photo request url
     */
    public static String URL_DELETE_PHOTO = "http://" + "&&" + URL_VER + "photo/remove/";

    /**
     * list ALL sequences request url
     */
    public static String URL_LIST_SEQUENCES = "http://" + "&&" + URL_VER + "list/";

    /**
     * list ALL sequences request url
     */
    public static String URL_LIST_TRACKS = "http://" + "&&" + URL_VER + "tracks/";

    /**
     * list ALL sequences request url
     */
    public static String URL_NEARBY_TRACKS = "http://" + "&&" + "nearby-tracks";

    /**
     * list ALL sequences request url
     */
    public static String URL_VERSION = "http://" + "&&" + "version";

    /**
     * list ALL sequences request url
     */
    public static String URL_LIST_MY_SEQUENCES = "http://" + "&&" + URL_VER + "list/my-list/";

    /**
     * list photos from a specific sequence url
     */
    public static String URL_LIST_PHOTOS = "http://" + "&&" + URL_VER + "sequence/photo-list/";

    /**
     * list details of my profile from a specific url
     */
    public static String URL_LIST_PROFILE_DETAILS = "http://" + "&&" + URL_VER + "user/details/";

    /**
     * download photo file reques url
     */
    public static String URL_DOWNLOAD_PHOTO = "http://" + "&&";

    public static int sUploadStatus = STATUS_IDLE;

    /**
     * finish a sequence upload and mark for processing
     */
    private static String URL_FINISH_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/finished-uploading/";

    private static String URL_AUTH = "http://" + "&&" + "auth/openstreetmap/client_auth";

    public final ConcurrentLinkedQueue<SequenceRequest> mCreateQueue = new ConcurrentLinkedQueue<>();

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

    /**
     * filter used for filtering only image upload requests
     */
    public UploadRequestFilter uploadFilter;

    /**
     * class used for managing upload tasks
     */
    public VideoUploaderQueue videoUploaderQueue;

    public int mCurrentServer = 0;

    /**
     * consumer object for oauth 1.0a
     */
    private CommonsHttpOAuthConsumer consumer;

    /**
     * provider object for oauth 1.0a
     */
    private CommonsHttpOAuthProvider provider;

    private Handler mBackgroundHandler;

    private HandlerThread mHandlerThread;

    private Handler mTracksHandler;

    private String mAccessToken;


    public UploadManager(Context context) {
        this.mContext = context;
        mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        HandlerThread handlerThread = new HandlerThread("Tracks", Thread.NORM_PRIORITY);
        handlerThread.start();
        mTracksHandler = new Handler(handlerThread.getLooper());
        this.mQueue = newRequestQueue(mContext, 4);
        this.mListQueue = newRequestQueue(mContext, 4);
        this.mSequenceQueue = newRequestQueue(mContext, 1);
        mSequenceQueue.stop();
        this.videoUploaderQueue = new VideoUploaderQueue(mContext);
        appPrefs = ((OSVApplication) mContext.getApplicationContext()).getAppPrefs();
        mCurrentServer = appPrefs.getIntPreference(PreferenceTypes.K_DEBUG_SERVER_TYPE);
        setEnvironment();
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
        URL_DOWNLOAD_PHOTO = URL_DOWNLOAD_PHOTO.replace("&&", URL_ENV[mCurrentServer]);
        URL_FINISH_SEQUENCE = URL_FINISH_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_TRACKS = URL_LIST_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_NEARBY_TRACKS = URL_NEARBY_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_PROFILE_DETAILS = URL_LIST_PROFILE_DETAILS.replace("&&", URL_ENV[mCurrentServer]);
        URL_VERSION = URL_VERSION.replace("&&", URL_ENV[mCurrentServer]);
        URL_AUTH = URL_AUTH.replace("&&", URL_ENV[mCurrentServer]);
        Log.d(TAG, "setEnvironment: " + URL_ENV[mCurrentServer]);
    }

    /**
     * uploads an image to a specific sequence
     * @param sequence parent folder
     * @param video image file
     * @param onlineSequenceID online sequence id
     * @param sequenceIndex the startIndex of the image
     * @param listener request listener
     */
    private void uploadVideo(final Sequence sequence, final OSVFile video, final int onlineSequenceID, final int sequenceIndex, final RequestListener listener) {
        if (!video.exists()) {
            Log.d(TAG, "uploadVideo: file doesn't exist: " + video.getPath());
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
                            Log.d(TAG, "uploadVideo: " + error.getLocalizedMessage());
                        }
                        if (apiCode == 660) {
                            SequenceDB.instance.deleteVideo(video, sequence.sequenceId, sequenceIndex);
                            videoUploaderQueue.markDone(sequence, true);
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
                                return;
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
                        SequenceDB.instance.deleteVideo(video, sequence.sequenceId, sequenceIndex);
                        videoUploaderQueue.markDone(sequence, true);
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_IMAGE);
                    }
                });
            }
        }, new ProgressiveEntity.DataProgressListener() {
            @Override
            public void onProgressChanged(long totalSent, long totalSize) {
                videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
            }
        }, getAccessToken(), video, onlineSequenceID, sequenceIndex);
        imageUploadReq.setRetryPolicy(new DefaultRetryPolicy(10000, 5, 1f));
        videoUploaderQueue.add(imageUploadReq);
    }


    /**
     * creates a sequence online, used before uploading the images
     * @param sequence folder of the sequence
     * @param listener resuest listener
     */
    private void createSequence(final Sequence sequence, final RequestListener listener) {
        final int onlineId = SequenceDB.instance.getOnlineId(sequence.sequenceId);
        final OSVFile metafile = new OSVFile(sequence.folder, "track.txt.gz");
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
                                        Log.d(TAG, "createSequence: " + new String(error.networkResponse.data));
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
                                    Log.d(TAG, "createSequence: interrupted at error");
                                    return;
                                }
                                if (mCreateQueue.size() != 0) {
                                    commitNextSequence();//creation failed after 5 tries, go to next one
                                } else {
                                    cancelUploadTasks();
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
                                int sequenceID;
                                try {

                                    JSONObject jsonObject;
                                    try {
                                        jsonObject = new JSONObject(response);
                                        sequenceID = jsonObject.getJSONObject("osv").getJSONObject("sequence").getInt("id");
                                        SequenceDB.instance.updateSequenceOnlineId(sequence.sequenceId, sequenceID);
                                        sequence.onlineSequenceId = sequenceID;
                                        if (metafile.exists()) {
                                            metafile.delete();
                                        }
                                        uploadSequence(sequence, sequenceID, listener);
                                        listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Log.d(TAG, "createSequence" + response);
                                } catch (Exception e) {
                                    Log.d(TAG, "createSequence: " + e.getLocalizedMessage());
                                }
                                if (Thread.interrupted()) {
                                    Log.d(TAG, "createSequence: interrupted at hardcoded");
                                    return;
                                }
                            }
                        });
                    }
                }, new ProgressiveEntity.DataProgressListener() {
                    @Override
                    public void onProgressChanged(long totalSent, long totalSize) {
                        videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
                    }
                }, sequence.sequenceId, getAccessToken(), position, metafile, sequence.appVersion, sequence.obd);
                seqRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 5, 1f));
                mCreateQueue.add(seqRequest);
            } else {
                Log.d(TAG, "createSequence: cursor has 0 elements");
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
                            if (metafile.exists()) {
                                metafile.delete();
                            }
                            uploadSequence(sequence, onlineId, listener);
                            listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE);
                            if (Thread.interrupted()) {
                                Log.d(TAG, "createSequence: interrupted at hardcoded");
                                return;
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
                                Log.d(TAG, "createSequence: interrupted at hardcoded");
                                return;
                            }
                        }
                    });
                }
            }, new ProgressiveEntity.DataProgressListener() {
                @Override
                public void onProgressChanged(long totalSent, long totalSize) {
                    videoUploaderQueue.partialProgressChanged(totalSent, totalSize);
                }
            }, sequence.sequenceId, getAccessToken(), "", null, sequence.appVersion, sequence.obd);
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
                                Log.d(TAG, "listSequences: " + new String(error.networkResponse.data));
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
                                Log.d(TAG, "nearby: " + new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (error instanceof TimeoutError) {
                            Log.d(TAG, "nearby: Timed out");
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
                                Log.d(TAG, "version: " + new String(error.networkResponse.data));
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
     * @param zoom
     */
    public void listTracks(final LoadAllSequencesListener listener, final String upperLeft, final String lowerRight, final int page, final float zoom) {

        ListTracksRequest seqRequest = new ListTracksRequest(URL_LIST_TRACKS, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.d(TAG, "listTracks: " + new String(error.networkResponse.data));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.onFinished();
                    }
                });
            }
        }, new Response.Listener<String>() {
            @Override
            public void onResponse(final String result) {
                mTracksHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result != null && !result.isEmpty()) {
                            try {
                                int alreadyDisplayed = page * TRACKS_IPP;
                                JSONObject obj = new JSONObject(result);
                                JSONArray tracks = obj.getJSONArray("currentPageItems");
                                int totalTracks = obj.getJSONArray("totalFilteredItems").getInt(0);
                                if (tracks == null) {
                                    listener.onFinished();
                                    return;
                                }
                                Log.d(TAG, "listTracks:  number of segments = " + tracks.length());
                                if (page == 1) {
                                    listener.onRequestFinished(null, -1);
                                }
                                if (tracks.length() > 0) {
                                    for (int i = alreadyDisplayed; i < alreadyDisplayed + tracks.length(); i++) {
                                        JSONArray track = tracks.getJSONObject(i - alreadyDisplayed).getJSONArray("track");
                                        final MapFragment.Polyline polyline = new MapFragment.Polyline(i + 1000);
                                        for (int j = 0; j < track.length(); j++) {
                                            if (Thread.interrupted()) {
                                                return;
                                            }
                                            double lat = track.getJSONArray(j).getDouble(0);
                                            double lon = track.getJSONArray(j).getDouble(1);
                                            polyline.getNodes().add(new ImageCoordinate(lon, lat, j));
                                        }
                                        listener.onRequestFinished(polyline, i + 1000);
                                    }
                                }
                                if (page * TRACKS_IPP < totalTracks) {
                                    int newPage = page + 1;
                                    listTracks(listener, upperLeft, lowerRight, newPage, zoom);
                                    return;
                                }
                                listener.onFinished();
                            } catch (Exception e) {
                                e.printStackTrace();
                                listener.onFinished();
                            }
                        } else {
                            listener.onFinished();
                        }
                    }
                });
            }
        }, upperLeft, lowerRight, page, TRACKS_IPP, zoom);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(3500, 5, 1f));
        mListQueue.add(seqRequest);
    }

    /**
     * finishes a sequence upload
     * @param sequence
     * @param listener
     */
    public void finishSequence(final Sequence sequence, final RequestListener listener) {
        SequenceFinishedRequest seqRequest = new SequenceFinishedRequest(URL_FINISH_SEQUENCE, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "finishSequence: error " + error.getLocalizedMessage());
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
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_SEQUENCE_FINISHED);
                        Log.d(TAG, "finishSequence: " + response);
                    }
                });
            }
        }, getAccessToken(), "" + sequence.onlineSequenceId);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 10, 1f));
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
            public void onErrorResponse(VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
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
     * delets an image from the server
     * @param imageId id of the img file on the server
     * @param listener
     */
    public void deleteImage(final int imageId, final RequestListener listener) {
        DeleteImageRequest seqRequest = new DeleteImageRequest(URL_DELETE_PHOTO, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
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
                        listener.requestFinished(RequestListener.STATUS_SUCCESS_DELETE_IMAGE);
                        Log.d(TAG, "deleteImage: " + response);
                    }
                });
            }
        }, imageId, getAccessToken());
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
        Cursor cursor = SequenceDB.instance.getVideos(sequence.sequenceId);
        if (cursor != null && cursor.getCount() > 0) {
            while (!cursor.isAfterLast()) {
                if (Thread.interrupted()) {
                    Log.d(TAG, "uploadSequence: interrupted");
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
        if (userName.equals("")) {
            listener.requestFinished(RequestListener.STATUS_FAILED, mContext.getString(R.string.not_logged_in));
            return;
        }
        ProfileRequest profileRequest = new ProfileRequest(URL_LIST_PROFILE_DETAILS, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.d(TAG, "getProfileDetails: " + new String(error.networkResponse.data));
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
                                Log.d(TAG, "listImages: " + new String(error.networkResponse.data));
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
    private void authenticate(final String requestToken, final String secretToken, final RequestResponseListener listener) {
        AuthRequest request = new AuthRequest(URL_AUTH, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                runInBackground(new Runnable() {
                    @Override
                    public void run() {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                Log.d(TAG, "authenticate: " + new String(error.networkResponse.data));
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
        mQueue.add(request);
    }

    private String getAccessToken() {
        if (mAccessToken == null) {
            mAccessToken = PreferenceManager.getDefaultSharedPreferences(mContext).getString(ACCESS_TOKEN_KEY, null);
        }
        return mAccessToken;
    }

    /**
     * displays a webview with the osm login site.
     * @param activity the ref for the support fragmenbt manager
     * @param listener response listener
     * @param onDetachListener on detach listener for the webview
     */
    public void logIn(final FragmentActivity activity, final RequestResponseListener listener, final OAuthDialogFragment.OnDetachListener onDetachListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET_KEY);

                    provider = new CommonsHttpOAuthProvider(
                            "https://www.openstreetmap.org/oauth/request_token",
                            "https://www.openstreetmap.org/oauth/access_token",
                            "https://www.openstreetmap.org/oauth/authorize");
                    provider.setOAuth10a(true);
                    String authUrl = provider.retrieveRequestToken(consumer, "osmlogin://telenav?");

                    showDialog(activity, authUrl, new OAuthResultListener() {
                        @Override
                        public void onResult(final String url) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Uri uri = Uri.parse(url);
                                        String oauthVerifier = uri.getQueryParameter("oauth_verifier");

                                        provider.retrieveAccessToken(consumer, oauthVerifier);

                                        HttpGet request = new HttpGet(URL_USER_DETAILS);
                                        // sign the request
                                        String requesToken = consumer.getToken();
                                        String secretToken = consumer.getTokenSecret();
                                        consumer.sign(request);
                                        HttpClient httpclient = new DefaultHttpClient();
                                        HttpResponse response;
                                        final String responseString;

                                        response = httpclient.execute(request);
                                        StatusLine statusLine = response.getStatusLine();
                                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                                            response.getEntity().writeTo(out);
                                            responseString = out.toString();
                                            out.close();
                                        } else {
                                            //Closes the connection.
                                            response.getEntity().getContent().close();
                                            throw new IOException(statusLine.getReasonPhrase());
                                        }
                                        authenticate(requesToken, secretToken, new RequestResponseListener() {
                                            @Override
                                            public void requestFinished(int status) {
                                                listener.requestFinished(RequestResponseListener.STATUS_FAILED, "status = " + status);
                                            }

                                            @Override
                                            public void requestFinished(int status, String result) {
                                                JSONObject obj = null;
                                                String accessToken = null;
                                                try {
                                                    obj = new JSONObject(result);
                                                    accessToken = obj.getJSONObject("osv").getString("access_token");
                                                    final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                                                    editor.putString(ACCESS_TOKEN_KEY, accessToken);
                                                    editor.apply();
                                                } catch (JSONException e) {
                                                    Log.d(TAG, "requestFinished: " + Log.getStackTraceString(e));
                                                }
                                                if (accessToken == null) {
                                                    listener.requestFinished(RequestResponseListener.STATUS_FAILED, "Could not parse access token");
                                                } else {
                                                    listener.requestFinished(RequestResponseListener.STATUS_SUCCESS_LOGIN, responseString);
                                                }
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        listener.requestFinished(RequestResponseListener.STATUS_FAILED, "" + e.getMessage());
                                    }
                                }
                            }).start();
                        }
                    }, onDetachListener);
                } catch (Exception e) {
                    Log.e(TAG, "logIn: logging in failed " + e.toString());
                    listener.requestFinished(RequestResponseListener.STATUS_FAILED, "" + e.getMessage());
                }
            }
        }).start();
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
        if (mBackgroundHandler == null) {
            mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
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
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundHandler.getLooper().getThread().interrupt();
            mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
            mHandlerThread.start();
            mBackgroundHandler = new Handler(mHandlerThread.getLooper());
            mSequenceQueue.cancelAll(new SequenceRequestFilter());
            int previousStatus = sUploadStatus;
            sUploadStatus = STATUS_IDLE;
            uploadFilter = new UploadRequestFilter();
            videoUploaderQueue.mVideoUploadQueue.cancelAll(uploadFilter);
            videoUploaderQueue.uploadTaskQueue.clear();
            videoUploaderQueue.progressListener.onUploadCancelled(getTotalSizeValue(), getRemainingSizeValue());
            SequenceDB.instance.interruptUploading();
            SequenceDB.instance.fixStatuses();
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
    public void cancelListTasks() {
        Log.d(TAG, "cancelListTasks: cancelled map list tasks");
        ListRequestFilter listFilter = new ListRequestFilter();
        mListQueue.cancelAll(listFilter);
    }

    /**
     * Display the oAuth web page in a dialog
     */
    public void showDialog(FragmentActivity activity, String authUrl, OAuthResultListener oAuthResultListener, OAuthDialogFragment.OnDetachListener detachListener) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.addToBackStack("dialog");

        // Create and show the dialog.
        OAuthDialogFragment newFragment = new OAuthDialogFragment();
        newFragment.setOnDetachListener(detachListener);
        newFragment.setURL(authUrl);
        newFragment.setResultListener(oAuthResultListener);
        newFragment.show(ft, OAuthDialogFragment.TAG);
    }

    public void uploadCacheIfAutoEnabled() {
        uploadCacheIfAutoEnabled(null);
    }

    public void uploadCacheIfAutoEnabled(final RequestListener listener) {
        if (sUploadStatus != STATUS_IDLE) {
            if (listener != null) {
                listener.requestFinished(RequestListener.STATUS_FAILED);
            }
            return;
        }
        final boolean auto = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_AUTO, false);
        final boolean data = appPrefs.getBooleanPreference(PreferenceTypes.K_UPLOAD_DATA_ENABLED, false);
        if (auto && (data || NetworkUtils.isWifiInternetAvailable(mContext)) && sUploadStatus == STATUS_IDLE) {
            uploadCache(listener, Sequence.getStaticSequences().values());
            Log.d(TAG, "uploadCacheIfAutoEnabled: with listener: " + listener);
        }
    }

    public void uploadCache(final RequestListener listener, final Collection<Sequence> sequences) {
        if (sUploadStatus != STATUS_IDLE) {
            if (listener != null) {
                listener.requestFinished(RequestListener.STATUS_FAILED);
            }
            return;
        }
        final Collection<Sequence> sequencesCopy = new ArrayList<>(sequences);
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String userId = appPrefs.getStringPreference(PreferenceTypes.K_USER_ID);
        if (userName.equals("") || userId.equals("")) {
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
                    Iterator<Sequence> iter = sequencesCopy.iterator();
                    while (iter.hasNext()) {
                        Sequence seq = iter.next();
                        if (seq.getStatus() == Sequence.STATUS_FINISHED || SequenceDB.instance.getNumberOfFrames(seq.sequenceId) <= 0) {
                            iter.remove();
                        } else {
                            seq.setStatus(Sequence.STATUS_INDEXING);
                        }
                    }
                    videoUploaderQueue.initialize(sequencesCopy);
                    for (final Sequence sequence : sequencesCopy) {
                        if (Thread.interrupted()) {
                            Log.d(TAG, "uploadCache: interrupted");
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
                    Log.d(TAG, "uploadCache: " + e.getLocalizedMessage());
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
                            Log.d(TAG, "consistencyCheck: " + e.getLocalizedMessage());
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
                                                Log.d(TAG, "consistencyCheck: " + e.getLocalizedMessage());
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
            Log.d(TAG, "consistencyCheck: " + e.getLocalizedMessage());
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

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "newRequestQueue: " + Log.getStackTraceString(e));
        }
        HttpStack stack = new HurlStack();
        Network network = new BasicNetwork(stack);
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("BackgroundUpload", Thread.NORM_PRIORITY);
        }
        Handler bh = new Handler(mHandlerThread.getLooper());
        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network, nrOfThreads, new ExecutorDelivery(bh));
        queue.start();

        return queue;
    }

    public void cancelTracks() {
        mListQueue.cancelAll(new ListTracksFilter());
        try {
            mTracksHandler.getLooper().getThread().interrupt();
        } catch (Exception e) {
            Log.d(TAG, "cancelTracks: " + Log.getStackTraceString(e));
        }
        mTracksHandler.removeCallbacksAndMessages(null);
        HandlerThread handlerThread = new HandlerThread("Tracks", Thread.NORM_PRIORITY);
        handlerThread.start();
        mTracksHandler = new Handler(handlerThread.getLooper());
    }

    public int getRemainingSequences() {
        return mCreateQueue.size();
    }

    public long getRemainingSizeValue() {
        long number = 0;
        for (final Sequence sequence : videoUploaderQueue.mSequences) {
            number = number + Utils.folderSize(Sequence.getLocalSequence(sequence.sequenceId).folder);
        }
        return number;
    }

    public long getTotalSizeValue() {
        if (videoUploaderQueue.mTotalSize == 0) {
            long number = 0;
            for (final Sequence sequence : videoUploaderQueue.mSequences) {
                number = number + Sequence.getLocalSequence(sequence.sequenceId).size;
            }
            videoUploaderQueue.mTotalSize = number;
        }
        return videoUploaderQueue.mTotalSize;
    }

    public void resetUploadStats() {
        videoUploaderQueue.mTotalSize = 0;
        videoUploaderQueue.numberOfFailed = 0;
        videoUploaderQueue.numberOfSuccessful = 0;
    }

    public void logOut() {
        if (!Utils.DEBUG || !appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH)) {
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            editor.clear();
            editor.commit();
            CookieSyncManager.createInstance(mContext);
            CookieManager.getInstance().removeAllCookie();
        }
        mAccessToken = null;

        appPrefs.saveStringPreference(PreferenceTypes.K_USER_ID, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_NAME, "");
    }


    private class VideoUploaderQueue implements RequestQueue.RequestFinishedListener<Object> {

        int numberOfFailed = 0;

        int numberOfSuccessful = 0;

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

        void add(VideoRequest request) {
            uploadTaskQueue.add(request);
            mVideoUploadQueue.add(request);
        }

        void initialize(Collection<Sequence> sequences) {
            numberOfFailed = 0;
            numberOfSuccessful = 0;
            mTotalSize = 0;
            mSequences.clear();
            mSequences.addAll(sequences);
            for (final Sequence sequence : sequences) {
                int number = (int) SequenceDB.instance.getNumberOfVideos(sequence.sequenceId);
                mTotalSize = mTotalSize + sequence.size;
                sequence.numberOfVideos = number;
            }
            Log.d(TAG, "initialize: mTotalSize = " + mTotalSize);

            if (sUploadStatus == STATUS_IDLE) {
                videoUploaderQueue.progressListener.onUploadStarted(mTotalSize);
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
                Log.d(TAG, "commitImages: empty upload queue - cancelled");
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
                    progressListener.onUploadStarted(getTotalSizeValue());
                    if (progressListener != null) {
                        progressListener.onProgressChanged(Math.max(getTotalSizeValue(), 1), getRemainingSizeValue());
                    }
                }
            }
        }

        @Override
        public void onRequestFinished(Request<Object> request) {
            Log.d(TAG, "onRequestFinished: for video file");
            if (request instanceof VideoRequest) {
                uploadTaskQueue.remove(request);
            }
        }

        void markDone(final Sequence sequence, boolean success) {
            if (sUploadStatus == STATUS_IDLE) {
                cancelUploadTasks();
                return;
            }
            if (sUploadStatus == STATUS_PAUSED) {
                pause();
                return;
            }
            if (progressListener != null) {
                progressListener.onImageUploaded(sequence, success);
            }

            if (!success) {
                sequence.failedCount++;
            }
            sequence.imageCount = (int) SequenceDB.instance.getNumberOfFrames(sequence.sequenceId) - sequence.failedCount;
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
                                    Log.d(TAG, "finishSequence: failed ");
                                    numberOfFailed++;
                                } else {
                                    Log.d(TAG, "finishSequence: success ");
                                    numberOfSuccessful++;
                                }
                            }
                        });
                    }
                });
                sequence.setStatus(Sequence.STATUS_FINISHED);
                if (progressListener != null) {
                    progressListener.onSequenceUploaded(sequence);
                }
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

        public void partialProgressChanged(long totalSent, long fileSize) {
            long totalSize = getTotalSizeValue();
            final long remainingSize = getRemainingSizeValue();
            if (progressListener != null) {
//                final int progress = (int) (((totalSize - (remainingSize - totalSent)) * 100) / totalSize);
//                Log.d(TAG, "partialProgressChanged: totalSize: " + totalSize + ", remainingSize: " + remainingSize + ", totalSent: " + totalSent + ", percentage is " + progress);
                progressListener.onProgressChanged(Math.max(totalSize, 1), remainingSize - totalSent);
            }
        }

        private void finishUpload() {
            Log.d(TAG, "finishUpload: finishing");
            if (progressListener != null) {
                progressListener.onProgressChanged(Math.max(getTotalSizeValue(), 1), 0);
                progressListener.onUploadFinished(numberOfSuccessful, numberOfFailed);
            }
            DeviceBandwidthSampler.getInstance().stopSampling();
            sUploadStatus = STATUS_IDLE;
            mVideoUploadQueue.stop();
            numberOfFailed = 0;
            numberOfSuccessful = 0;
            SequenceDB.instance.fixStatuses();
        }
    }
}
