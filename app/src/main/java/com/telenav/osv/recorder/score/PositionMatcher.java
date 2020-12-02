package com.telenav.osv.recorder.score;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.telenav.osv.common.Injection;
import com.telenav.osv.common.model.KVLatLng;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.matcher.BoundingBoxChangedEvent;
import com.telenav.osv.event.network.matcher.CoverageEvent;
import com.telenav.osv.event.network.matcher.KVBoundingBox;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Segment;
import com.telenav.osv.item.network.GeometryCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.GeometryRetriever;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.ComputingDistance;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Component that matches position to segments received from backend
 * Created by Kalman on 31/10/16.
 */
public class PositionMatcher {

    private static final String TAG = "PositionMatcher";

    private static final float MAX_RESOLUTION_ZOOM = 19;

    private static final double MATCH_DISTANCE_LIMIT = 15d / 110000d;

    private static final int TWO_SEC = 2000;

    private static final int MAX_POSITIONS_HISTORY = 5;

    private static final double[] HEADING_WEIGHTS = new double[]{2, 4, 8, 16};

    private final GeometryRetriever geometryRetriever;

    private final Object matcherSyncObject = new Object();

    private ArrayList<Polyline> polylines;

    private boolean requestSent = false;

    private long lastCheckTime;

    private SegmentsListener mSegmentsListener;

    private KVBoundingBox triggerBoundingBox;

    private KVBoundingBox loadedBoundingBox;

    private LinkedList<KVLatLng> mLastCoordinates = new LinkedList<>();

    public PositionMatcher(Context context) {
        if (context != null) {
            geometryRetriever = Injection.provideGeometryRetriever(context, Injection.provideNetworkFactoryUrl(Injection.provideApplicationPreferences(context)));
        } else {
            geometryRetriever = null;
        }
    }

    static KVBoundingBox getBoundingBoxForRegion(KVLatLng coords, double dist) {
        return getBoundingBox(coords.getLat(), coords.getLon(), dist);
    }

    private static boolean pointIsInBB(KVBoundingBox bb, double lat, double lon) {
        return bb != null &&
                (bb.getTopLeft().getLat() >= lat && lat >= bb.getBottomRight().getLat() && bb.getTopLeft().getLon() <= lon &&
                        lon <= bb.getBottomRight().getLon());
    }

    private static KVBoundingBox getBoundingBox(double lat, double lng, double meters) {
        double topLeftLat = lat + getMercFromDist(meters);
        double topLeftLon = lng - getMercFromDist(meters);
        double bottomRightLat = lat - getMercFromDist(meters);
        double bottomRightLon = lng + getMercFromDist(meters);
        return new KVBoundingBox(new KVLatLng(topLeftLat, topLeftLon, 0), new KVLatLng(bottomRightLat, bottomRightLon, 0));
    }

    private static double getMercFromDist(double meters) {
        return meters / 110000;
    }

    private static double calculateBearing(KVLatLng coord1, KVLatLng coord2) {
        double longDiff = coord2.getLon() - coord1.getLon();
        double y = Math.sin(longDiff) * Math.cos(coord2.getLat());
        double x = Math.cos(coord1.getLat()) * Math.sin(coord2.getLat()) -
                Math.sin(coord1.getLat()) * Math.cos(coord2.getLat()) * Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public void setSegmentsListener(SegmentsListener segmentsListener) {
        this.mSegmentsListener = segmentsListener;
    }

    @VisibleForTesting()
    public double getBearing(KVLatLng coordinate) {
        offerKVLatLngForBearing(coordinate);
        double totalWeight = 0;
        double totalWeightedBearing = 0;
        for (int i = 0; i < mLastCoordinates.size() - 1; i++) {
            KVLatLng coord1 = mLastCoordinates.get(i);
            KVLatLng coord2 = mLastCoordinates.get(i + 1);
            double bearing = calculateBearing(coord1, coord2);
            //fail-safe in order to not get indexOutOfBonds error in case of multiple threads
            int maxWeighIndex = HEADING_WEIGHTS.length - 1;
            int weightIndex = i;
            if (i > maxWeighIndex) {
                weightIndex = maxWeighIndex;
            }
            double weight = HEADING_WEIGHTS[weightIndex];
            totalWeight += weight;
            totalWeightedBearing += bearing * weight;
            Log.d(TAG, String.format("getBearing. Status: calculated weight. Message: Calculating total weight: %s", totalWeightedBearing));
        }
        double bearing = 0;
        if (totalWeight > 0) {
            bearing = totalWeightedBearing / totalWeight;
        }
        Log.d(TAG, "getBearing: " + bearing);
        return bearing;
    }

    Segment onLocationChanged(KVLatLng kvLatLng) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastCheckTime > TWO_SEC)) {
            lastCheckTime = currentTime;
            final KVLatLng coordinate = new KVLatLng(kvLatLng.getLat(), kvLatLng.getLon(), 0);
            KVBoundingBox smallBB = getBoundingBoxForRegion(coordinate, 500);
            //send request if needed
            requestNewDataIfNeeded(smallBB, coordinate);
            if (Utils.DEBUG) {
                EventBus.post(new BoundingBoxChangedEvent(triggerBoundingBox, loadedBoundingBox, smallBB));
            }
            if (isCoverageForKVLatLng(coordinate)) {
                return match(coordinate);
            } else {
                Log.d(TAG, "onOSVLatLngChanged: OSVLatLng not covered");
            }
        }
        return null;
    }

    Segment onPictureTaken(KVLatLng kvLatLng) {
        //        if (event.OSVLatLng.getAccuracy() > 500){todo what happens with tunnel mode pictures?
        //            return;
        //        }
        final KVLatLng coordinate = new KVLatLng(kvLatLng.getLat(), kvLatLng.getLon(), 0);
        KVBoundingBox smallBB = getBoundingBoxForRegion(coordinate, 500);
        //send request if needed
        requestNewDataIfNeeded(smallBB, coordinate);
        if (Utils.DEBUG) {
            EventBus.post(new BoundingBoxChangedEvent(triggerBoundingBox, loadedBoundingBox, smallBB));
        }
        boolean OSVLatLngIsCovered = isCoverageForKVLatLng(coordinate);
        if (OSVLatLngIsCovered) {
            //======================================================
            //match the position
            Log.d(TAG, "onPictureTaken: matching on local data");
            synchronized (matcherSyncObject) {
                return match(coordinate);
            }
        } else {
            //======================================================
            //cannot match the position
            Log.d(TAG, "onPictureTaken: OSVLatLng not covered");
            return null;
        }
        //==========================================================
        //display debug overlays
    }

    void offerNewSegments(KVLatLng coordinate, List<Polyline> segments, KVBoundingBox triggerBB, KVBoundingBox requestedBB) {
        triggerBoundingBox = triggerBB;
        loadedBoundingBox = requestedBB;
        synchronized (matcherSyncObject) {
            if (polylines == null) {
                polylines = new ArrayList<>();
            }
            polylines.clear();
            polylines.addAll(segments);
            if (polylines.isEmpty()) {
                Log.d(TAG, "requestNewDataIfNeeded: no segments were received");
            }
            Log.d(TAG, "requestNewDataIfNeeded: request " + polylines.size() + " segments received");
            requestSent = false;
            Log.d(TAG, "requestNewDataIfNeeded: segments received");
            if (mSegmentsListener != null) {
                mSegmentsListener.onSegmentsReceived(coordinate);
            }
            isCoverageForKVLatLng(coordinate);
        }
    }

    boolean isCoverageForKVLatLng(KVLatLng coordinate) {
        boolean isCoverage = pointIsInBB(loadedBoundingBox, coordinate.getLat(), coordinate.getLon());
        if (isCoverage) {
            EventBus.postSticky(new CoverageEvent(true));
        } else {
            EventBus.postSticky(new CoverageEvent(false));
        }
        return isCoverage;
    }

    Segment match(KVLatLng coordinate) {
        double historicalBearing = getBearing(coordinate);
        if (polylines != null) {
            synchronized (matcherSyncObject) {
                long time = System.currentTimeMillis();
                ArrayList<Segment> candidates = new ArrayList<>();
                for (Polyline polyline : polylines) {
                    List<KVLatLng> track = polyline.getNodes();
                    if (coordinate != null && !track.isEmpty()) {
                        for (int i = 0; i < track.size() - 1; i++) {
                            KVLatLng coord1 = track.get(i);
                            KVLatLng coord2 = track.get(i + 1);
                            double distanceToPos = ComputingDistance.getDistanceFromSegment(coordinate, coord1, coord2);
                            if (distanceToPos < MATCH_DISTANCE_LIMIT) {
                                double dist1 = ComputingDistance.distanceBetween(coord1, coordinate);
                                double dist2 = ComputingDistance.distanceBetween(coord2, coordinate);

                                KVLatLng firstCoord;
                                KVLatLng secondCoord;
                                if (dist1 < dist2) {
                                    firstCoord = coord1;
                                    secondCoord = coord2;
                                } else {
                                    firstCoord = coord2;
                                    secondCoord = coord1;
                                }
                                double bearing = calculateBearing(firstCoord, secondCoord);
                                double altBearing = (bearing + 180) % 360;
                                if ((distanceToPos < dist1 || distanceToPos < dist2) &&
                                        (Math.abs(bearing - historicalBearing) > Math.abs(altBearing - historicalBearing))) {
                                    bearing = altBearing;
                                    KVLatLng temp = firstCoord;
                                    firstCoord = secondCoord;
                                    secondCoord = temp;
                                }
                                double deltaBearing = Math.abs(bearing - historicalBearing);
                                Log.d(TAG, "match:" + polyline.getIdentifier() + " bearing for " + i + " is " + bearing + " delta is " + deltaBearing);

                                //                          0-20                0-180
                                float score = (float) (((distanceToPos / MATCH_DISTANCE_LIMIT) * 50.0 + (deltaBearing / 180) * 50.0) / 100f);
                                candidates.add(new Segment(distanceToPos, coordinate, polyline, firstCoord, secondCoord, deltaBearing, score));
                            }
                        }
                    }
                }

                Segment bestCandidate = null;
                float bestScore = 100000f;
                if (candidates.isEmpty()) {
                    return null;
                }
                for (Segment segment : candidates) {
                    if (segment.getScore() < bestScore) {
                        bestScore = segment.getScore();
                        bestCandidate = segment;
                    }
                }

                Log.d(TAG, "match: matched segment called on " + polylines.size() + " segments, run in " + (System.currentTimeMillis() - time));

                if (bestCandidate == null) {
                    Polyline polyline = new Polyline(404);
                    polyline.coverage = 0;
                    return new Segment(1000, coordinate, polyline, new KVLatLng(), new KVLatLng(), 0, 0);
                } else {
                    return bestCandidate;
                }
            }
        }
        return null;
    }

    void offerKVLatLngForBearing(KVLatLng coordinate) {
        mLastCoordinates.addLast(coordinate);
        while (mLastCoordinates.size() > MAX_POSITIONS_HISTORY) {
            mLastCoordinates.pollFirst();
        }
    }

    private void requestNewDataIfNeeded(KVBoundingBox smallBB, final KVLatLng coordinate) {
        if (assessBBDifference(triggerBoundingBox, smallBB) && !requestSent && geometryRetriever != null) {
            requestSent = true;
            Log.d(TAG, "requestNewDataIfNeeded: loading segments for " + " coordinate " + coordinate);
            final KVBoundingBox requestedBB = getBoundingBoxForRegion(coordinate, 3500);
            final KVBoundingBox triggerBB = getBoundingBoxForRegion(coordinate, 1500);
            Log.d(TAG, "requestNewDataIfNeeded: " + triggerBB);
            BackgroundThreadPool.post(() -> {
                Log.d(TAG, "requestNewDataIfNeeded: sending request");

                geometryRetriever.listSegments(new NetworkResponseDataListener<GeometryCollection>() {

                                                   @Override
                                                   public void requestFailed(int status, GeometryCollection details) {
                                                       Log.d(TAG, "requestNewDataIfNeeded: " + details);
                                                       requestSent = false;
                                                   }

                                                   @Override
                                                   public void requestFinished(int status, GeometryCollection collectionData) {
                                                       offerNewSegments(coordinate, collectionData.getSegmentList(), triggerBB, requestedBB);
                                                   }
                                               }, requestedBB.getTopLeft().getLat() + "," + requestedBB.getTopLeft().getLon(),
                        requestedBB.getBottomRight().getLat() + "," + requestedBB.getBottomRight().getLon(),
                        MAX_RESOLUTION_ZOOM);
            });
        }
    }

    private boolean assessBBDifference(KVBoundingBox mLastBB, KVBoundingBox bbnormal) {
        double bottomLat = bbnormal.getBottomRight().getLat();
        double bottomLon = bbnormal.getBottomRight().getLon();
        double topLat = bbnormal.getTopLeft().getLat();
        double topLon = bbnormal.getTopLeft().getLon();
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

    public interface SegmentsListener {

        void onSegmentsReceived(KVLatLng location);
    }
}
