package com.telenav.osv.manager.network;

import javax.inject.Inject;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.telenav.osv.data.AccountPreferences;
import com.telenav.osv.http.ListTracksRequest;
import com.telenav.osv.http.NearbyRequest;
import com.telenav.osv.http.requestFilters.NearbyRequestFilter;
import com.telenav.osv.item.network.GeometryCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsvRequestResponseListener;
import com.telenav.osv.manager.network.parser.GeometryParser;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.NearbyTracksParser;
import com.telenav.osv.utils.Log;
import static com.telenav.osv.data.Preferences.URL_ENV;

/**
 * *
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GeometryRetriever extends NetworkManager implements Response.ErrorListener {

    private static final String TAG = "GeometryRetriever";

    private static final int TRACKS_TO_LOAD = 100000;

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_TRACKS = "http://" + "&&" + URL_VER + "tracks/";

    /**
     * list ALL sequences request url
     */
    private static String URL_NEARBY_TRACKS = "http://" + "&&" + "nearby-tracks";

    private Handler mTracksHandler;

    private RequestQueue.RequestFilter mTrackFilter = request -> request instanceof ListTracksRequest;

    private GeometryParser mGeometryParser = new GeometryParser();

    private NearbyTracksParser mNearbyTracksParser = new NearbyTracksParser();

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    @Inject
    public GeometryRetriever(Context context, AccountPreferences prefs) {
        super(context, prefs);
        HandlerThread handlerThread = new HandlerThread("Tracks", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mTracksHandler = new Handler(handlerThread.getLooper());
        this.mQueue = newRequestQueue(mContext, 4);

    }

    @Override
    protected void setupUrls() {
        URL_LIST_TRACKS = URL_LIST_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_NEARBY_TRACKS = URL_NEARBY_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        Log.d(TAG, "setEnvironment: " + URL_ENV[mCurrentServer]);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * error listener for volley
     * @param error error thrown
     */
    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e(TAG, "onErrorResponse" + mHttpResponseParser.parse(error).toString());
    }

    /**
     * lists all existing sequences from the server, no images are downloaded, only details
     * @param listener request listener
     * @param zoom zoom level
     */
    public void listSegments(final NetworkResponseDataListener<GeometryCollection> listener, final String upperLeft, final String lowerRight,
                             final float zoom) {
        Log.d(TAG, "listSegments: cancelling previous requests");
        mQueue.cancelAll(mTrackFilter);
        mTracksHandler.removeCallbacksAndMessages(null);
        ListTracksRequest seqRequest =
                new ListTracksRequest(URL_LIST_TRACKS, new OsvRequestResponseListener<GeometryParser, GeometryCollection>(mGeometryParser) {

                    @Override
                    public void onSuccess(final int status, final GeometryCollection geometryCollection) {
                        Log.d(TAG, "listSegments: onResponse: tracks response finished");
                        mTracksHandler.post(() -> listener.requestFinished(status, geometryCollection));
                    }

                    @Override
                    public void onFailure(final int status, final GeometryCollection geometryCollection) {
                        mTracksHandler.post(() -> listener.requestFailed(status, geometryCollection));
                    }
                }, upperLeft, lowerRight, 1, TRACKS_TO_LOAD, zoom);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 3, 1f));
        mQueue.add(seqRequest);
    }

    public void nearby(final NetworkResponseDataListener<TrackCollection> listener, String lat, String lon) {
        NearbyRequest seqRequest =
                new NearbyRequest(URL_NEARBY_TRACKS, new OsvRequestResponseListener<NearbyTracksParser, TrackCollection>(mNearbyTracksParser) {

                    @Override
                    public void onSuccess(final int status, final TrackCollection trackCollection) {
                        runInBackground(() -> {
                            listener.requestFinished(status, trackCollection);
                            Log.d(TAG, "nearby: successful");
                        });
                    }

                    @Override
                    public void onFailure(final int status, final TrackCollection trackCollection) {
                        runInBackground(() -> {
                            listener.requestFailed(status, trackCollection);
                            Log.d(TAG, "nearby: successful");
                        });
                    }
                }, lat, lon, 50);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(18000, 1, 1f));
        mQueue.add(seqRequest);
    }

    /**
     * cancells all upload request added
     */
    public void cancelNearby() {
        Log.d(TAG, "cancelListTasks: cancelled map list tasks");
        NearbyRequestFilter listFilter = new NearbyRequestFilter();
        mQueue.cancelAll(listFilter);
    }
}
