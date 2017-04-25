package com.telenav.osv.manager.location;

import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKBoundingBox;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.network.matcher.BoundingBoxChangedEvent;
import com.telenav.osv.event.network.matcher.CoverageEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.SegmentEvent;
import com.telenav.osv.event.network.matcher.SegmentsReceivedEvent;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.listener.LoadAllSequencesListener;
import com.telenav.osv.manager.network.UploadManager;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 31/10/16.
 */

public class PositionMatcher {

    private static final String TAG = "PositionMatcher";

    private static final float MAX_RESOLUTION_ZOOM = 19;

    private final UploadManager mUploadManager;

    private final Object matcherSyncObject = new Object();

    private SKBoundingBox mTriggerBB;

    private SKBoundingBox mLoadedBB;

    private HandlerThread mHandlerThread = new HandlerThread("Matcher", Process.THREAD_PRIORITY_BACKGROUND);

    private Handler mBackgroundHandler;

    private ArrayList<Polyline> mPolylines;

    private boolean requestSent = false;

    public PositionMatcher(Context context) {
        mHandlerThread.start();
        mBackgroundHandler = new Handler(mHandlerThread.getLooper());
        mUploadManager = ((OSVApplication) context.getApplicationContext()).getUploadManager();
        EventBus.register(this);
    }

    private static SKBoundingBox getBoundingBoxForRegion(SKCoordinate coords, double dist) {
        return getBoundingBox(coords.getLatitude(), coords.getLongitude(), dist);
    }

    private static boolean pointIsInBB(SKBoundingBox bb, double lat, double lon) {
        return bb != null && (bb.getTopLeft().getLatitude() >= lat && lat >= bb.getBottomRight().getLatitude() && bb.getTopLeft().getLongitude() <= lon && lon <= bb.getBottomRight().getLongitude());
    }

    private static SKBoundingBox getBoundingBox(double lat, double lng, double meters) {
        double topLeftLat = lat + getMercFromDist(meters);
        double topLeftLon = lng - getMercFromDist(meters);
        double bottomRightLat = lat - getMercFromDist(meters);
        double bottomRightLon = lng + getMercFromDist(meters);
        return new SKBoundingBox(new SKCoordinate(topLeftLat, topLeftLon), new SKCoordinate(bottomRightLat, bottomRightLon));
    }

    private static double getMercFromDist(double meters) {
        return meters / 110000;
    }

    private static double getMercFromDistCos(double meters, double lat) {
        return meters / (110000 * Math.cos(lat));
    }

    public void onLocationChanged(Location location) {
        final SKCoordinate coordinate = new SKCoordinate(location.getLatitude(), location.getLongitude());
        SKBoundingBox smallBB = getBoundingBoxForRegion(coordinate, 500);
        boolean locationIsCovered = isCoverageForLocation(location);
        //==========================================================
        //send request if needed
        requestNewDataIfNeeded(smallBB, coordinate);
        if (locationIsCovered) {
            //======================================================
            //match the position
            EventBus.postSticky(new CoverageEvent(true));
            Log.d(TAG, "onLocationChanged: matching on local data");
            synchronized (matcherSyncObject) {
                match(coordinate);
            }
        } else {
            //======================================================
            //cannot match the position
            EventBus.postSticky(new CoverageEvent(false));
            Log.d(TAG, "onLocationChanged: matched segment called location not covered");
            EventBus.postSticky(new MatchedSegmentEvent(null, null));
        }
        //==========================================================
        //display debug overlays
        if (Utils.DEBUG) {
            EventBus.post(new BoundingBoxChangedEvent(mTriggerBB, mLoadedBB, smallBB));
        }
    }


    private void requestNewDataIfNeeded(SKBoundingBox smallBB, final SKCoordinate coordinate) {
        if (assessBBDifference(mTriggerBB, smallBB) && !requestSent) {
            requestSent = true;
            Log.d(TAG, "requestNewDataIfNeeded: loading segments for " + " coordinate " + coordinate);
            final SKBoundingBox requestedBB = getBoundingBoxForRegion(coordinate, 2000);
            final SKBoundingBox newLastBB = getBoundingBoxForRegion(coordinate, 1500);
            Log.d(TAG, "requestNewDataIfNeeded: " + newLastBB);
            final ArrayList<Polyline> newPolylines = new ArrayList<>();
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "requestNewDataIfNeeded: sending request");

                    mUploadManager.listSegments(new LoadAllSequencesListener() {
                        @Override
                        public void onRequestFailed() {
                            Log.d(TAG, "requestNewDataIfNeeded: could not retrieve segments");
                            requestSent = false;
                        }

                        @Override
                        public void onRequestSuccess() {
                            EventBus.post(new SegmentsReceivedEvent());
                        }

                        @Override
                        public void onRequestFinished(final Polyline polyline, final int id) {
                            if (polyline != null) {
                                synchronized (matcherSyncObject) {
                                    newPolylines.add(polyline);
                                    EventBus.post(new SegmentEvent(polyline));//todo this is not needed, just display the same time
                                }
                            }
                        }

                        @Override
                        public void onFinished(Polyline polyline) {
                            mTriggerBB = newLastBB;
                            mLoadedBB = requestedBB;
                            synchronized (matcherSyncObject) {
                                if (mPolylines == null) {
                                    mPolylines = new ArrayList<>();
                                }
                                mPolylines.clear();
                                mPolylines.addAll(newPolylines);
                                if (mPolylines.isEmpty()) {
                                    Log.d(TAG, "requestNewDataIfNeeded: no segments were recived");
                                } else {
                                    if (polyline == null) {
                                        polyline = new Polyline(15050);
                                        polyline.coverage = 0;
                                    }
                                }
                                Log.d(TAG, "requestNewDataIfNeeded: request " + mPolylines.size() + " segments received");
                                synchronized (matcherSyncObject) {
                                    EventBus.postSticky(new MatchedSegmentEvent(polyline, mPolylines));
                                }
                                requestSent = false;
                            }
                        }
                    }, coordinate, requestedBB.getTopLeft().getLatitude() + "," + requestedBB.getTopLeft().getLongitude(), requestedBB.getBottomRight().getLatitude() + ","
                            + requestedBB.getBottomRight().getLongitude(), 1, MAX_RESOLUTION_ZOOM);
                }
            });
        }
    }

    private boolean isCoverageForLocation(Location location) {
        return pointIsInBB(mLoadedBB, location.getLatitude(), location.getLongitude());
    }

    private void match(SKCoordinate coordinate) {
        if (mPolylines != null) {
            synchronized (matcherSyncObject) {
                double bestDistance = Double.MAX_VALUE;
                Polyline bestPolyline = null;
                for (Polyline polyline : mPolylines) {
                    SKCoordinate start, end;
                    List<SKCoordinate> track = polyline.getNodes();
                    if (coordinate != null && !track.isEmpty()) {
                        start = track.get(0);
                        end = track.get(track.size() - 1);
//                        double distanceToPos = ComputingDistance.getDistanceFromLine(coordinate, start, end);
                        double distanceToPos = ComputingDistance.getDistanceFromSegment(coordinate, start, end);
                        if (distanceToPos < bestDistance) {
                            bestDistance = distanceToPos;
                            bestPolyline = polyline;
                        }
                    }
                }
                if (bestDistance > getMercFromDist(15)) {
                    bestPolyline = new Polyline(404);
                    bestPolyline.coverage = 0;
                }
                Log.d(TAG, "match: matched segment called");
                EventBus.postSticky(new MatchedSegmentEvent(bestPolyline, mPolylines));
            }
        }
    }

    private boolean assessBBDifference(SKBoundingBox mLastBB, SKBoundingBox bbnormal) {
        double bottomLat = bbnormal.getBottomRight().getLatitude();
        double bottomLon = bbnormal.getBottomRight().getLongitude();
        double topLat = bbnormal.getTopLeft().getLatitude();
        double topLon = bbnormal.getTopLeft().getLongitude();
        if (mLastBB == null) {
            Log.d(TAG, "assessBBDifference: previous bb null");
            return true;
        }
        boolean changed = !pointIsInBB(mLastBB, bottomLat, bottomLon) || !pointIsInBB(mLastBB, topLat, topLon);
        if (!changed) {
            Log.d(TAG, "assessBBDifference: not changed enough");
        }
        return changed;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStarted(RecordingEvent event) {
        if (event.started) {
        } else {
        }
    }
}
