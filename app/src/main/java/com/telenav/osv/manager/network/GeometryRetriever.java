package com.telenav.osv.manager.network;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.http.ListTracksRequest;
import com.telenav.osv.http.NearbyRequest;
import com.telenav.osv.http.requestFilters.NearbyRequestFilter;
import com.telenav.osv.item.network.GeometryCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.jarvis.login.utils.LoginUtils;
import com.telenav.osv.listener.network.KVRequestResponseListener;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.parser.GeometryParser;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.NearbyParser;
import com.telenav.osv.network.endpoint.FactoryServerEndpointUrl;
import com.telenav.osv.network.endpoint.UrlGeometry;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * *
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GeometryRetriever extends NetworkManager implements Response.ErrorListener {

    private static final String TAG = "GeometryRetriever";

    private static final int TRACKS_TO_LOAD = 100000;

    private Handler mTracksHandler;

    private RequestQueue.RequestFilter mTrackFilter = request -> request instanceof ListTracksRequest;

    private GeometryParser mGeometryParser = new GeometryParser();

    private FactoryServerEndpointUrl factoryServerEndpointUrl;

    private NearbyParser mNearbyParser;

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    public GeometryRetriever(Context context, FactoryServerEndpointUrl factoryServerEndpointUrl) {
        super(context);
        this.factoryServerEndpointUrl = factoryServerEndpointUrl;
        mNearbyParser = new NearbyParser(factoryServerEndpointUrl);
        HandlerThread handlerThread = new HandlerThread("Tracks", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mTracksHandler = new Handler(handlerThread.getLooper());
        this.mQueue = newRequestQueue(mContext, 4);
        EventBus.register(this);
        VolleyLog.DEBUG = Utils.isDebugEnabled(mContext);
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
                new ListTracksRequest(factoryServerEndpointUrl.getGeometryEndpoint(UrlGeometry.LIST_SEQUENCES), new KVRequestResponseListener<GeometryParser,
                        GeometryCollection>(mGeometryParser) {

                    @Override
                    public void onSuccess(final int status, final GeometryCollection geometryCollection) {
                        Log.d(TAG, "listSegments: onResponse: tracks response finished");
                        mTracksHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFinished(status, geometryCollection);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status, final GeometryCollection geometryCollection) {
                        mTracksHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, geometryCollection);
                            }
                        });
                    }
                }, upperLeft, lowerRight, 1, TRACKS_TO_LOAD, zoom, LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(10000, 3, 1f));
        mQueue.add(seqRequest);
    }

    public void nearby(final NetworkResponseDataListener<TrackCollection> listener, String lat, String lon) {
        NearbyRequest seqRequest =
                new NearbyRequest(factoryServerEndpointUrl.getGeometryEndpoint(UrlGeometry.NEARBY_SEQUENCE),
                        new KVRequestResponseListener<NearbyParser, TrackCollection>(mNearbyParser) {

                            @Override
                            public void onSuccess(final int status, final TrackCollection trackCollection) {
                                runInBackground(new Runnable() {

                                    @Override
                                    public void run() {
                                        listener.requestFinished(status, trackCollection);
                                        Log.d(TAG, "nearby: successful");
                                    }
                                });
                            }

                            @Override
                    public void onFailure(final int status, final TrackCollection trackCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, trackCollection);
                                Log.d(TAG, "nearby: successful");
                            }
                        });
                    }
                }, lat, lon, 50, LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
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
