package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;
import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKBoundingBox;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.positioner.SKPosition;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.SdkEnabledEvent;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.LocationEvent;
import com.telenav.osv.event.hardware.gps.TrackChangedEvent;
import com.telenav.osv.event.network.matcher.BoundingBoxChangedEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.SegmentEvent;
import com.telenav.osv.event.network.matcher.SegmentsReceivedEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.ui.PositionerEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.LoadAllSequencesListener;
import com.telenav.osv.manager.Recorder;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Fragment holding the map or the placeholder
 * Created by Kalman on 11/9/15.
 */
public class MapFragment extends Fragment implements SensorEventListener, PlaybackManager.PlaybackListener {

    public final static String TAG = "MapFragment";

    /**
     * id for selected image marker annotation
     */
    public static final byte VIA_POINT_ICON_ID = 4;

    public static final int MODE_IDLE = 0;

    public static final int MODE_TRACK_PREVIEW = 1;

    public static final int MODE_RECORDING_SCREEN = 2;

    /**
     * time, in milliseconds, from the moment when the application receives new
     * GPS values
     */
    private static final int MINIMUM_TIME_UNTILL_MAP_CAN_BE_UPDATED = 30;

    /**
     * defines how smooth the movement will be (1 is no smoothing and 0 is never
     * updating).
     */
    private static final float SMOOTH_FACTOR_COMPASS = 0.1f;

    private static final int TRACK_POLYLINE_ID = 10000156;

    /**
     * true, if compass mode is available
     */
    public static boolean compassAvailable;

    public Sequence mCurrentSequence;

    /**
     * fragment's view
     */
    public View view;

    public boolean mRecording = false;

    public boolean mIsSmall = false;

    /**
     * the values returned by magnetic sensor
     */
    private float[] orientationValues;

    /**
     * last time when received GPS signal
     */
    private long lastTimeWhenReceivedGpsSignal;

    /**
     * the current value of the z axis ; at each new step it is updated with the
     * new value
     */
    private float currentCompassValue;

    /**
     * the latest exact screen orientation (given by the
     * getExactScreenOrientation method) that was recorded
     */
    private int lastExactScreenOrientation = -1;

    /**
     * Surface view for displaying the map
     */
//    private SKMapSurfaceView mapView;

    /**
     * Tells if heading is currently active
     */
    private boolean headingOn;

    /**
     * the app prefs
     */
    private ApplicationPreferences appPrefs;

    /**
     * the list of images with coordinates used for previewing a sequence
     */
    private ArrayList<ImageCoordinate> mPreviewNodes;

    private OSVActivity activity;

    /**
     * custom annotation view
     */
//    private SKAnnotationView redAnnotationView;

    /**
     * custom annotation view
     */
//    private SKAnnotationView greenAnnotationView;

    /**
     * custom annotation view
     */
//    private SKAnnotationView blueAnnotationView;


//    private ArrayList<ImageView> annotationViewList;

//    private SKAnnotation mSelectedPositionAnnotation;

//    private ProgressBar mLoadingIndicator;

    private Handler mBackgroundHandler;

    private Polyline mTrackPolyline = new Polyline(TRACK_POLYLINE_ID);

    private FloatingActionButton recordButton;

    private FloatingActionButton positionButton;

    private SKBoundingBox boundingBoxUS;

    private boolean noPositionYet = true;

    private PlaybackManager mPlayer;

    private int mCurrentMode = MODE_IDLE;

    private SKBoundingBox mLastBB;

    private Polyline mMatchedPolyline;

    private Recorder mRecorder;

    private boolean mMapEnabled;

    private boolean mReceivedSegments;

    private View chessBackground;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (OSVActivity) getActivity();
        appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
        //force map disabled
        appPrefs.saveBooleanPreference(PreferenceTypes.K_MAP_DISABLED, true);
        mMapEnabled = false;

        view = inflater.inflate(R.layout.fragment_map, null);
        recordButton = (FloatingActionButton) view.findViewById(R.id.record_button);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cameraPermitted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
                if (cameraPermitted == PackageManager.PERMISSION_GRANTED) {
                    if (!appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED) && appPrefs.getBooleanPreference(PreferenceTypes.K_RECORDING_MAP_ENABLED, true)) {
                        recordButton.setVisibility(View.INVISIBLE);
                        positionButton.setVisibility(View.INVISIBLE);
                    }
                }
                activity.openScreen(ScreenComposer.SCREEN_RECORDING);
            }
        });
        HandlerThread handlerThread = new HandlerThread("Loader", Thread.NORM_PRIORITY);
        handlerThread.start();
        mBackgroundHandler = new Handler(handlerThread.getLooper());
        positionButton = (FloatingActionButton) view.findViewById(R.id.position_button);
        recordButton.setVisibility(View.INVISIBLE);
        positionButton.setVisibility(View.INVISIBLE);
        if (mMapEnabled) {
            final Runnable addMapRunnable = new Runnable() {
                @Override
                public void run() {
                    View map;
                    map = inflater.inflate(R.layout.partial_map, null);
                    FrameLayout holder = (FrameLayout) view.findViewById(R.id.frameLayout);
                    holder.addView(map);
                }
            };
            if (!appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_DISABLED)/* && !Maps.initialized()*/) {
                Log.d(TAG, "onCreateView: needs to initialize maps");
            } else {
                addMapRunnable.run();
            }
            positionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPositionerClicked(null);
                }
            });
            boundingBoxUS = new SKBoundingBox(new SKCoordinate(49.384358, -124.848974), new SKCoordinate(24.396308, -66.885444));
        } else {
            View map;
            map = inflater.inflate(R.layout.partial_map_placeholder, null);
            FrameLayout holder = (FrameLayout) view.findViewById(R.id.frameLayout);
            holder.addView(map);
            recordButton.setVisibility(View.VISIBLE);
            positionButton.setVisibility(View.INVISIBLE);
        }
        Log.d(TAG, "onCreateView: ");
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                onViewChanged(/*mapViewGroup,*/ right, bottom);
            }
        });
        return view;
    }

    private void onViewChanged(/*SKMapViewHolder mapViewGroup,*/ int width, int height) {
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        Log.d(TAG, "onSurfaceChanged: map is " + width + "x" + height + ", while screen is " + point.x + "x" + point.y);
        int screen = activity.getCurrentScreen();
        boolean isSmall = (width < point.x / 2) && (height < point.y / 2) && (screen == ScreenComposer.SCREEN_RECORDING || screen == ScreenComposer.SCREEN_RECORDING_HINTS);
        boolean maximized = (width >= point.x / 10 * 9) && (height >= point.y / 10 * 9);
        int mode = mCurrentMode;
        if (maximized) {
            enterMode(MODE_IDLE);
        } else if (screen == ScreenComposer.SCREEN_RECORDING || screen == ScreenComposer.SCREEN_RECORDING_HINTS) {
            enterMode(MODE_RECORDING_SCREEN);
        } else {
            enterMode(MODE_TRACK_PREVIEW);
        }
//        if (isSmall != mIsSmall || mCurrentMode != mode) {
        mIsSmall = isSmall;
        Log.d(TAG, "onSurfaceChanged: mIsSmall = " + mIsSmall);
        setup(/*mapViewGroup*/);
//        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecorder = activity.getApp().getRecorder();
//        if (mCurrentMode != MODE_IDLE
//                || activity.getCurrentScreen() == FlowManager.SCREEN_RECORDING
//                || activity.getCurrentScreen() == FlowManager.SCREEN_RECORDING_HINTS
//                || activity.getCurrentScreen() == FlowManager.SCREEN_PREVIEW) {
//            recordButton.setVisibility(View.INVISIBLE);
//            positionButton.setVisibility(View.INVISIBLE);
//        } else {
//            view.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    recordButton.setVisibility(View.VISIBLE);
//                    positionButton.setVisibility(View.VISIBLE);
//                }
//            }, 300);
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.register(this);
        if (mMapEnabled) {
            if (headingOn) {
                startOrientationSensor();
            }
            mTrackPolyline = new Polyline(10000156);
            mTrackPolyline.setLineSize(0);
            mTrackPolyline.setOutlineSize(4);
            mTrackPolyline.setOutlineDottedPixelsSkip(25);
            mTrackPolyline.setOutlineDottedPixelsSolid(25);
            mTrackPolyline.setColor(new float[]{0f / 255f, 122f / 255f, 255f / 255f, 1.0f});
            mTrackPolyline.setOutlineColor(new float[]{0f / 255f, 122f / 255f, 255f / 255f, 1.0f});
        }
    }

    @Override
    public void onStop() {
        if (mMapEnabled) {
            if (headingOn) {
                stopOrientationSensor();
            }
            if (compassAvailable) {
                stopOrientationSensor();
            }
        }
        EventBus.unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onPositionerClicked(PositionerEvent event) {
//        if (mapView != null) {
            if (activity.checkPermissionsForGPS()) {
                if (!Utils.isGPSEnabled(activity)) {
                    activity.resolveLocationProblem(false);
                } else {
                    if (mRecorder.hasPosition()) {
//                        mapView.centerOnCurrentPosition(16, true, 1000);//zoomlevel, anim time
                    } else {
                        activity.showSnackBar("Waiting for GPS position...", Snackbar.LENGTH_SHORT);
                    }
                }
            }
//        }
    }

//    //onMapSurfaceCreated
//    @Override
    public void onSurfaceCreated(/*SKMapViewHolder mapHolder*/) {
        Log.d(TAG, "onSurfaceCreated: concurrency ");
        chessBackground = view.findViewById(R.id.chess_board_background);
        chessBackground.setVisibility(View.GONE);
//        mapView = mapHolder.getMapSurfaceView();
//        mapView.getMapSettings().setCityPoisShown(false);
//        mapView.getMapSettings().setGeneratedPoisShown(false);
//        mapView.getMapSettings().setImportantPoisShown(false);
//        mapView.getMapSettings().setMapPoiIconsShown(false);
//        mapView.getMapSettings().setHouseNumbersShown(false);
//        mapView.setZOrderMediaOverlay(true);
        if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MAP) {
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final double lat = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT);
                    final double lon = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON);
                    if (lat != 0 && lon != 0) {
                        Log.d(TAG, "run: lat lon = " + lat + ", " + lon);
//                        SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(lat, lon));
//                        mapView.setPositionAsCurrent(new SKCoordinate(lat, lon), 20, true);
//                        mapView.centerOnCurrentPosition(16, true, 1000);//zoomlevel, anim time
                    } else if (lat == 0 && lon == 0) {
//                        mapView.setZoom(1);
//                        mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
                    }
//                    mapView.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            diplayLocalSequences();
//                            if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MAP) {
//                                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_TAP_ON_MAP, false)) {
//                                    activity.showSnackBar(R.string.tip_map_screen, Snackbar.LENGTH_LONG, R.string.got_it_label, new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_HINT_TAP_ON_MAP, true);
//                                        }
//                                    });
//                                }
//                            }
//                        }
//                    }, 1040);
                }
            }, 1000);
        }
        resizeStopped();
    }

//    @Override
//    public void onSurfaceChanged(SKMapViewHolder mapViewHolder, int width, int height) {
//        onViewChanged(mapViewHolder,width,height);
//    }

    private void setup(/*SKMapViewHolder mapViewGroup*/) {
        if (mCurrentMode == MODE_IDLE) {
//            if (mapViewGroup != null) {
//                mapViewGroup.showScoutTextView();
//                if (mapView == null){
//                    mapView = mapViewGroup.getMapSurfaceView();
//                }
//            }
            if (recordButton != null && positionButton != null) {
                recordButton.setVisibility(View.VISIBLE);
                positionButton.setVisibility(View.VISIBLE);
            }
//            applySettingsOnMapView(true);
            final double lat = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT);
            final double lon = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON);
//            if (lat == 0 && lon == 0) {
//                if (mapView != null) {
//                    mapView.setZoom(1);
//                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
//                }
//            } else {
//                if (noPositionYet) {
//                    SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(lat, lon));
//                }
//            }
            mLastBB = null;
//            if (mapView != null) {
//                mapView.clearOverlay(TRACK_POLYLINE_ID);
//                mapView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (lat == 0 && lon == 0) {
//                            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
//                        } else {
//                            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
//                        }
//                    }
//                });
//            }
        } else {
            if (recordButton != null && positionButton != null) {
                recordButton.setVisibility(View.INVISIBLE);
                positionButton.setVisibility(View.INVISIBLE);
            }
//            if (this.mapViewGroup != null) {
//                this.mapViewGroup.hideScoutTextView();
//            }
            if (mCurrentMode == MODE_RECORDING_SCREEN) {
                mBackgroundHandler.removeCallbacksAndMessages(null);
                mLastBB = null;
//                if (mapView != null) {
//                    mapView.setZoom(16);
//                    applySettingsOnMapView(false);
//                    mapView.setZOrderMediaOverlay(true);
//                    mapView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_ARROW_SMALL);
//                        }
//                    });
//                }
            } else if (mCurrentMode == MODE_TRACK_PREVIEW && mPlayer != null) {
//                if (mapView != null) {
//                    applySettingsOnMapView(true);
//                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
//                }
                if (mPlayer != null) {
                    displaySequence(mPlayer.getTrack(), !mPlayer.isSafe(), mPlayer.getSequence().skipToValue);
                }
            } else {
                Log.d(TAG, "setup: player is null");
            }
        }
    }

    /**
     * reads and displayes all locally cached sequences from the files system
     */
    private void diplayLocalSequences() {
        if (mMapEnabled) {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (SequenceDB.instance != null) {
                        synchronized (Sequence.getStaticSequences()) {
                            for (Sequence sequence : Sequence.getStaticSequences().values()) {
                                if (sequence.originalImageCount > 0) {
                                    try {
                                        displayPolyline(sequence.polyline);
                                    } catch (NumberFormatException e) {
                                        Log.d(TAG, "diplayLocalSequences: " + e.getLocalizedMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * displays local and online sequences
     */
    public void refreshDisplayedSequences() {
        if (activity == null || !mMapEnabled) {
            return;
        }
        final SKCoordinateRegion skCoordinateRegion = /*mapView.getCurrentMapRegion()*/ new SKCoordinateRegion();
        SKBoundingBox bbnormal = /*mapView.getBoundingBoxForRegion(skCoordinateRegion)*/ new SKBoundingBox(new SKCoordinate(),new SKCoordinate());
        if (assessBBDifference(mLastBB, bbnormal)) {
            final float zoom = skCoordinateRegion.getZoomLevel();
            SKCoordinate coord = skCoordinateRegion.getCenter();
            if (zoom < 5) {
//                mapView.clearAllOverlays();
                mLastBB = null;
                return;
            }
            Log.d(TAG, "refreshDisplayedSequences: loading tracks for zoom level " + zoom + " , coordinate " + coord);
            skCoordinateRegion.setZoomLevel(zoom - 2);
            final SKBoundingBox boundingBox = /*mapView.getBoundingBoxForRegion(skCoordinateRegion)*/new SKBoundingBox(new SKCoordinate(),new SKCoordinate());;
//            mLastBB = boundingBox;
            //intermediary bb
            skCoordinateRegion.setZoomLevel(zoom - 1f);
            mLastBB = /*mapView.getBoundingBoxForRegion(skCoordinateRegion)*/new SKBoundingBox(new SKCoordinate(),new SKCoordinate());;
            Log.d(TAG, "refreshDisplayedSequences: " + mLastBB);
//            ((OSVApplication) activity.getApplication()).getUploadManager().cancelTracks();
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
//                    if (mapView != null) {
                        Log.d(TAG, "refreshDisplayedSequences: sending request");
                        ((OSVApplication) activity.getApplication()).getUploadManager().listSegments(new LoadAllSequencesListener() {

                            @Override
                            public void onRequestFailed() {
                                mLastBB = null;
                                Log.d(TAG, "refreshDisplayedSequences: could not retrieve segments");
                            }

                            @Override
                            public void onRequestSuccess() {

                            }

                            @Override
                            public void onRequestFinished(final Polyline polyline, final int id) {
                                if (polyline != null) {
                                    displayPolyline(polyline);
                                }
                            }

                            @Override
                            public void onFinished(Polyline matched) {

                            }
                        }, null, boundingBox.getTopLeft().getLatitude() + "," + boundingBox.getTopLeft().getLongitude(), boundingBox.getBottomRight().getLatitude() + ","
                                + boundingBox.getBottomRight().getLongitude(), 1, /*mapView.getMapSettings().isMapZoomingEnabled() ? mapView.getZoomLevel() :*/ 19);
//                    }
                }
            });
        } else {
            Log.d(TAG, "refreshDisplayedSequences: not changed enough");
        }
    }


//    /**
//     * Customize the map view
//     */
//    private void applySettingsOnMapView(boolean interactive) {
//        if (mapView != null) {
//            mapView.getMapSettings().setMapRotationEnabled(interactive);
//            mapView.getMapSettings().setMapZoomingEnabled(interactive || Utils.DEBUG);
//            mapView.getMapSettings().setMapPanningEnabled(interactive);
//            mapView.getMapSettings().setZoomWithAnchorEnabled(interactive);
//            mapView.getMapSettings().setInertiaRotatingEnabled(interactive);
//            mapView.getMapSettings().setInertiaZoomingEnabled(interactive);
//            mapView.getMapSettings().setInertiaPanningEnabled(interactive);
//            mapView.getMapSettings().setFollowPositions(!interactive);
//            setHeading(!interactive);
//            updateMapStyle();
//        }
//    }
//
//
//    /**
//     * updates the current map style to the new style given as parameter
//     */
//    private void updateMapStyle() {
//        SKMapViewStyle style = new SKMapViewStyle(SplashActivity.mapResourcesDirPath + "grayscalestyle" + "/", "grayscalestyle.json");
//        SKMapSettings skMapSettings = mapView.getMapSettings();
//        String currentMapStyle = skMapSettings.getMapStyle().getStyleFileName();
//        String nextMapStyle = style.getStyleFileName();
//
//        if (!currentMapStyle.equals(nextMapStyle)) {
//            skMapSettings.setMapStyle(style);
//        }
//    }
//
//    @Override
//    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {
//        if (mCurrentMode == MODE_IDLE && mapView != null) {
//            Log.d(TAG, "onMapRegionChangeEnded: ");
//            refreshDisplayedSequences();
//        }
//    }

    private boolean assessBBDifference(SKBoundingBox mLastBB, SKBoundingBox bbnormal) {
        Log.d(TAG, "assessBBDifference: large is " + mLastBB);
        Log.d(TAG, "assessBBDifference: small is " + bbnormal);
        double bottomLat = bbnormal.getBottomRight().getLatitude();
        double bottomLon = bbnormal.getBottomRight().getLongitude();
        double topLat = bbnormal.getTopLeft().getLatitude();
        double topLon = bbnormal.getTopLeft().getLongitude();
        boolean value = mLastBB == null || !pointIsInBB(mLastBB, bottomLat, bottomLon) || !pointIsInBB(mLastBB, topLat, topLon);
        Log.d(TAG, "assessBBDifference: " + value);
        return value;
    }

    private boolean pointIsInBB(SKBoundingBox bb, double lat, double lon) {
        return bb.getTopLeft().getLatitude() >= lat && lat >= bb.getBottomRight().getLatitude() && bb.getTopLeft().getLongitude() <= lon && lon <= bb.getBottomRight()
                .getLongitude();
    }

//    // map interaction callbacks ...
//    @Override
//    public void onActionPan() {
//    }
//
//    @Override
//    public void onActionZoom() {
//    }
//
//    @Override
//    public void onCustomPOISelected(SKMapCustomPOI customPoi) {
//        onMapInteraction();
//    }
//
//    @Override
//    public void onCurrentPositionSelected() {
//        onMapInteraction();
//    }
//
//    @Override
//    public void onObjectSelected(int i) {
//        onMapInteraction();
//    }
//
//    @Override
//    public void onDoubleTap(SKScreenPoint point) {
//        onMapInteraction();
//    }

//    @Override
    public void onSingleTap(/*SKScreenPoint skScreenPoint*/) {
        if (onMapInteraction()) {
            return;
        }
//        if (mapView == null) {
//            return;
//        }

        if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MAP) {
            activity.enableProgressBar(true);
            SKCoordinate tappedCoords = /*mapView.pointToCoordinate(skScreenPoint);*/ new SKCoordinate();
            ((OSVApplication) activity.getApplication()).getUploadManager().nearby(new RequestResponseListener() {
                @Override
                public void requestFinished(int status, String result) {
                    Log.d(TAG, "nearby: " + result);
                    activity.enableProgressBar(false);
                    boolean ok = false;
                    try {
                        JSONObject obj = new JSONObject(result);
                        JSONObject osv = obj.getJSONObject("osv");
                        Log.d(TAG, "requestFinished: " + osv);
                        ok = true;
                    } catch (Exception e) {
                        Log.d(TAG, "requestFinished: ");
                    }
                    if (ok) {
                        activity.openScreen(ScreenComposer.SCREEN_NEARBY, result);
                    } else {
                        activity.showSnackBar(getString(R.string.nearby_no_result_label), Snackbar.LENGTH_SHORT);
                    }

                }

                @Override
                public void requestFinished(int status) {
                    Log.w(TAG, "nearby: error");
                    activity.enableProgressBar(false);

                }
            }, "" + tappedCoords.getLatitude(), "" + tappedCoords.getLongitude(), 50);
        }
    }

//    @Override
    public void onInternetConnectionNeeded() {
        if (activity != null) {
            activity.showSnackBar(R.string.map_no_internet_connection, Snackbar.LENGTH_LONG);
        }
    }

//    @Override
    public void onMapActionUp(/*SKScreenPoint skScreenPoint*/) {
        if (mCurrentMode == MODE_RECORDING_SCREEN) {
            if (mIsSmall) {
                EventBus.post(new PreviewSwitchEvent(false));
                return;
            } else if (mRecording) {
                mRecorder.takePhoto();
                return;
            }
            return;
        } else {
            if (mCurrentMode == MODE_IDLE /*&& mapView != null*/) {
                Log.d(TAG, "onMapActionUp: ");
                refreshDisplayedSequences();
            }
        }
        Log.d(TAG, "onMapActionUp: reached end");
    }

//    @Override
    public void onPOIClusterSelected(/*SKPOICluster skpoiCluster*/) {
        onMapInteraction();
    }

//    @Override
    public void onMapPOISelected(/*SKMapPOI skMapPOI*/) {
        onMapInteraction();
    }

//    @Override
    public void onAnnotationSelected(/*SKAnnotation skAnnotation*/) {
        onMapInteraction();
    }

//    @Override
    public void onLongPress(/*SKScreenPoint point*/) {
        onMapInteraction();
    }

    private boolean onMapInteraction() {
        if (activity != null) {
            activity.cancelNearby();
            if (mCurrentMode == MODE_RECORDING_SCREEN) {
                if (mIsSmall) {
                    EventBus.post(new PreviewSwitchEvent(false));
                    return true;
                } else if (mRecording) {
                    mRecorder.takePhoto();
                    return true;
                }
                return true;
            }
            if (!mMapEnabled) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param newCompassValue new z value returned by the sensors
     */
    private void applySmoothAlgorithm(float newCompassValue) {
        if (Math.abs(newCompassValue - currentCompassValue) < 180) {
            currentCompassValue = currentCompassValue + SMOOTH_FACTOR_COMPASS * (newCompassValue - currentCompassValue);
        } else {
            if (currentCompassValue > newCompassValue) {
                currentCompassValue = (currentCompassValue + SMOOTH_FACTOR_COMPASS * ((360 + newCompassValue - currentCompassValue) % 360) + 360) % 360;
            } else {
                currentCompassValue = (currentCompassValue - SMOOTH_FACTOR_COMPASS * ((360 - newCompassValue + currentCompassValue) % 360) + 360) % 360;
            }
        }
    }


    /**
     * Enables/disables heading mode
     * @param enabled for enabled
     */
    private void setHeading(boolean enabled) {
        if (enabled) {
            headingOn = true;
//            mapView.getMapSettings().setFollowPositions(true);
            startOrientationSensor();
//            mapView.centerOnCurrentPosition(16, true, 1000);
        } else {
            headingOn = false;
//            mapView.getMapSettings().setFollowPositions(false);
            stopOrientationSensor();
        }
    }

    /**
     * Activates the orientation sensor
     */
    private void startOrientationSensor() {
        orientationValues = new float[3];
        SensorManager sensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);
        Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Deactivates the orientation sensor
     */
    private void stopOrientationSensor() {
        orientationValues = null;
        SensorManager sensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        //mapView.reportNewHeading(t.values[0]);
        switch (event.sensor.getType()) {

            case Sensor.TYPE_ORIENTATION:
                if (orientationValues != null) {
                    System.arraycopy(event.values, 0, orientationValues, 0, orientationValues.length);
                    if (orientationValues[0] != 0) {
                        if ((System.currentTimeMillis() - lastTimeWhenReceivedGpsSignal) > MINIMUM_TIME_UNTILL_MAP_CAN_BE_UPDATED) {
                            applySmoothAlgorithm(orientationValues[0]);
                            int currentExactScreenOrientation = Utils.getExactScreenOrientation(activity);
                            if (lastExactScreenOrientation != currentExactScreenOrientation) {
                                lastExactScreenOrientation = currentExactScreenOrientation;
                                switch (lastExactScreenOrientation) {
                                    case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
//                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.PORTRAIT);
                                        break;
                                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
//                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.PORTRAIT_UPSIDEDOWN);
                                        break;
                                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
//                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.LANDSCAPE_RIGHT);
                                        break;
                                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
//                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.LANDSCAPE_LEFT);
                                        break;
                                }
                            }

                            // report to NG the new value
                            if (orientationValues[0] < 0) {
//                                mapView.reportNewHeading(-orientationValues[0]);
                            } else {
//                                mapView.reportNewHeading(orientationValues[0]);
                            }

                            lastTimeWhenReceivedGpsSignal = System.currentTimeMillis();
                        }
                    }
                }
                break;
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onTrackChanged(TrackChangedEvent event) {
        Log.d(TAG, "onTrackChanged: track size is " + event.track.size());
        refreshTrackPolyline(event.track);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLocationChanged(LocationEvent event) {
        if (mMapEnabled) {
            if (/*mapView != null && */mCurrentMode == MODE_IDLE) {
//                mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
            }
            if (noPositionYet) {
                noPositionYet = false;
                final double latitude = event.location.getLatitude();
                final double longitude = event.location.getLongitude();
                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_RUN_COUNTER)) {
                    appPrefs.saveBooleanPreference(PreferenceTypes.K_RUN_COUNTER, true);
                    setMetrics(latitude, longitude);
                }
                appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) latitude);
                appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) longitude);
                setSignDetectionRegion(latitude, longitude);
//                if (mapView != null) {
//                    mapView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (mapView != null && mCurrentMode == MODE_IDLE) {
//                                if (latitude == 0 && longitude == 0) {
//                                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
//                                } else {
//                                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
//                                }
//                            }
//                        }
//                    });
//                }

            }
        }
    }

    private void setMetrics(double latitude, double longitude) {
        if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeft().getLatitude(), boundingBoxUS.getTopLeft().getLongitude(), boundingBoxUS.getBottomRight()
                        .getLatitude(),
                boundingBoxUS.getBottomRight().getLongitude())) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, false);
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, true);
        }
    }

    private void setSignDetectionRegion(double latitude, double longitude) {
        //TODOdo reinitialize sign detection
        if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeft().getLatitude(), boundingBoxUS.getTopLeft().getLongitude(), boundingBoxUS.getBottomRight()
                        .getLatitude(),
                boundingBoxUS.getBottomRight().getLongitude())) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_REGION_US, true);
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_REGION_US, false);
        }
    }


//    public void displaySequence(final Sequence sequence, boolean isLocal, final int selectedIndex) {
//        displaySequence(sequence, isLocal, selectedIndex, null);
//    }
//
//    public void displaySequence(final Sequence sequence, boolean isLocal, final int selectedIndex, final RequestListener listener) {
//        if (sequence == null) {
//            return;
//        }
////        mLoadingIndicator.setVisibility(View.VISIBLE);
//        if (!activity.getCurrentFragment().equals(TAG)) {
//            activity.openScreen(FlowManager.SCREEN_MAP);
//        }
//        if (isLocal) {
//        } else {
//            final ArrayList<ImageFile> nodes = new ArrayList<>();
//            UploadManager uploadManager = ((OSVApplication) activity.getApplication()).getUploadManager();
//            final int finalSeqId = sequence.sequenceId;
//            uploadManager.listImages(sequence.sequenceId, new RequestResponseListener() {
//                @Override
//                public void requestFinished(final int status, final String result) {
//                    if (status == RequestResponseListener.STATUS_FAILED) {
//                        Log.d(TAG, "displaySequence: failed, " + result);
//                        return;
//                    }
//                    if (result != null && !result.isEmpty()) {
//                        try {
//                            JSONObject obj = new JSONObject(result);
//                            JSONArray array = obj.getJSONObject("osv").getJSONArray("photos");
//                            for (int i = 0; i < array.length(); i++) {
//                                String link = UploadManager.URL_DOWNLOAD_PHOTO + array.getJSONObject(i).getString("lth_name");
//                                String thumbLink = "";
//                                try {
//                                    thumbLink = UploadManager.URL_DOWNLOAD_PHOTO + array.getJSONObject(i).getString("th_name");
//                                } catch (Exception e) {
//                                    Log.d(TAG, "displaySequence: " + e.getLocalizedMessage());
//                                }
//                                int index = array.getJSONObject(i).getInt("sequence_index");
//                                int id = array.getJSONObject(i).getInt("id");
//                                double lat = array.getJSONObject(i).getDouble("lat");
//                                double lon = array.getJSONObject(i).getDouble("lng");
//                                if (lat != 0.0 && lon != 0.0) {
//                                    nodes.add(new ImageFile(finalSeqId, link, thumbLink, id, index, new SKCoordinate(lon, lat), false));
//                                }
//                            }
//                            Collections.sort(nodes, new Comparator<ImageFile>() {
//                                @Override
//                                public int compare(ImageFile lhs, ImageFile rhs) {
//                                    return lhs.index - rhs.index;
//                                }
//                            });
//                            Log.d(TAG, "listImages: id=" + finalSeqId + " length=" + nodes.size());
//                            activity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
////                                    MainActivity.sLastSequence = sequence.sequenceId;
//                                    displaySequence(nodes, false);
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//
//                @Override
//                public void requestFinished(final int status) {
//
//                }
//            });
//        }
//    }

    /**
     * displays a specific sequence, other sequences get cleared
     * @param nodes the coordinates and the images
     */
    @SuppressWarnings("unchecked")
    public void displaySequence(final ArrayList<SKCoordinate> nodes, boolean isLocal, int startIndex) {
        Log.d(TAG, "displaySequence: ");
        // set the nodes on the polyline
        if (nodes != null && nodes.size() != 0 /*&& mapView != null*/) {
            mCurrentSequence = mPlayer.getSequence();
            int sequenceId = mCurrentSequence.sequenceId;
//            mapView.clearAllOverlays();
            mLastBB = null;
//            mapView.deleteAllAnnotationsAndCustomPOIs();
//            if (!fromMap) {
//                activity.showActionBar(activity.getResources().getString(R.string.overview_label), true);
//            } else {
//                activity.hideActionBar();
//            }
            mPreviewNodes = (ArrayList<ImageCoordinate>) nodes.clone();
            final ArrayList<SKCoordinate> coords = new ArrayList<>();
            coords.addAll(nodes);
            Polyline polyline = new Polyline(sequenceId);
            polyline.isLocal = isLocal;
            polyline.coverage = 15;
            polyline.setNodes(coords);
//            Log.d(TAG, "displaySequence: ");
            displayPolyline(polyline);
            zoomToPolyline(coords, (int) Utils.dpToPx(activity, 15), (int) Utils.dpToPx(activity, 15));
//            mapView.deleteAnnotation(VIA_POINT_ICON_ID);
//            mSelectedPositionAnnotation.setLocation(nodes.get(Math.min(startIndex, nodes.size() - 1)));
//            mapView.addAnnotation(mSelectedPositionAnnotation, SKAnimationSettings.ANIMATION_PULSE_CCP);
        } else {
            Log.w(TAG, "displaySequence: nodes size is 0");
        }
    }

    private void zoomToPolyline(ArrayList<SKCoordinate> coords, int paddingX, int paddingY) {
        Log.d(TAG, "zoomToPolyline: ");
        double northLat = 0, westLon = 0, southLat = 0, eastLon = 0;
        for (SKCoordinate coord : coords) {
            double lon = coord.getLongitude();
            double lat = coord.getLatitude();
            if (northLat == 0) northLat = lat;
            if (westLon == 0) westLon = lon;
            if (southLat == 0) southLat = lat;
            if (eastLon == 0) eastLon = lon;
            westLon = Math.min(lon, westLon);
            southLat = Math.min(lat, southLat);
            northLat = Math.max(northLat, lat);
            eastLon = Math.max(eastLon, lon);
        }

        SKBoundingBox boundingBox = new SKBoundingBox(new SKCoordinate(northLat, westLon), new SKCoordinate(southLat, eastLon));
//        mapView.fitBoundingBox(boundingBox, paddingX, paddingY, paddingY / 2, paddingY / 2);
    }

    /**
     * displayes a polyline for a list of coords
     */
    public void displayPolyline(Polyline polyline) {
        if (/*mapView != null && */polyline != null && polyline.getNodes().size() > 0) {
//            Log.d(TAG, "displayPolyline: " + polyline.getIdentifier() + ", size " + polyline.getNodes().size());
            // set polyline color
            if (polyline.isLocal) {
                polyline.setColor(new float[]{0f, 0f, 0f, 1.0f});  //black
                polyline.setOutlineColor(new float[]{0f, 0f, 0f, 1.0f});  //black
            } else {
                if (Utils.DEBUG) {
                    float[] color = new float[4];
                    color[3] = 0.3f;
                    switch (polyline.coverage) {
                        case 0:
                            color[0] = 189f / 255f;
                            color[1] = 16f / 255f; // white
                            color[2] = 224f / 255f;
                            break;
                        case 1:
                        case 2:
                            color[0] = 0f;
                            color[1] = 1f; // cyan
                            color[2] = 230f / 255f;
                            break;
                        case 3:
                        case 4:
                            color[0] = 0f;
                            color[1] = 1f; // green
                            color[2] = 118f / 255f;
                            break;
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                            color[0] = 240f / 255f;
                            color[1] = 1f; // yellow
                            color[2] = 0f;
                            break;
                        case 10:
                        default:
                            color[0] = 1f;
                            color[1] = 0f; // red
                            color[2] = 0f;
                            break;
                    }
                    polyline.setColor(color);
                    polyline.setOutlineColor(color);
                } else {
                    polyline.setColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, Math.min(polyline.coverage, 10) * 0.09f + 0.1f}); //accent color
                    polyline.setOutlineColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, Math.min(polyline.coverage, 10) * 0.09f + 0.1f}); //accent color
                }
            }
            polyline.setOutlineSize(3);
            polyline.setOutlineDottedPixelsSolid(50000);
            polyline.setOutlineDottedPixelsSkip(1);
            polyline.setLineSize(3);
//            mapView.addPolyline(polyline);
        }
    }

    /**
     * removes the currently viewed sequence and displayes all sequences
     */
    public void removeSequence() {
        if (mPreviewNodes != null) {
            mPreviewNodes.clear();
        }
//        if (mapView != null) {
//            if (mCurrentSequence != null){
//                mapView.clearOverlay(mCurrentSequence.sequenceId);
//            }
//            mapView.deleteAllAnnotationsAndCustomPOIs();
//            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
//        }
        mCurrentSequence = null;
        System.gc();
        diplayLocalSequences();
    }

    /**
     * views the image from startIndex, moving the marker on the map
     * @param index startIndex of the image in the sequence list
     */
    private void viewFrame(int index) {
        if (mPreviewNodes == null || mPreviewNodes.size() <= index) {
            Log.w(TAG, "viewFrame: sorry returning");
            return;
        }
//        mSelectedPositionAnnotation.setLocation(mPreviewNodes.get(index));
//        mapView.updateAnnotation(mSelectedPositionAnnotation);

    }

    public void refreshTrackPolyline(ArrayList<SKCoordinate> track) {
        if (mRecording && mTrackPolyline != null /*&& mapView != null*/) {
            mTrackPolyline.setNodes(track);
//            mapView.addPolyline(mTrackPolyline);
            if (!Utils.DEBUG) {
//                mapView.setZoomSmooth(16, 1000);
            }
        }
    }

    public void setSource(PlaybackManager source) {
        mPlayer = source;
        mPlayer.addPlaybackListener(this);
    }

    private void enterMode(int mode) {
        Log.d(TAG, "enterMode: " + mode);
        mCurrentMode = mode;
        switch (mode) {
            case MODE_IDLE:
                removeSequence();
                mRecording = false;
                recordButton.setVisibility(View.VISIBLE);
                break;
            case MODE_RECORDING_SCREEN:
            case MODE_TRACK_PREVIEW:
                recordButton.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onPrepared() {
        Log.d(TAG, "onPrepared: ");
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer != null) {
                        displaySequence(mPlayer.getTrack(), !mPlayer.isSafe(), mPlayer.getSequence().skipToValue);
                    }
                }
            });
        }
    }

    @Override
    public void onProgressChanged(final int index) {
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onProgressChanged: " + index);
                    viewFrame(index);
                }
            });
        }
    }

    @Override
    public void onExit() {
        removeSequence();
    }

    //@formatter:off
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {}

    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {}

    public void onRotateMap() {}

    public void onMapActionDown(/*SKScreenPoint skScreenPoint*/) {}

    public void onCompassSelected() {}

    public void onInternationalisationCalled(int i) {}

    public void onDebugInfo(double v, float v1, double v2) {}

    public void onBoundingBoxImageRendered(int i) {}

    public void onGLInitializationError(String s) {}

    public void onScreenshotReady(Bitmap bitmap) {}

    public void onPlaying() {}

    public void onPaused() {}

    public void onStopped() {}

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onSdkEnabled(final SdkEnabledEvent event) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mMapEnabled = event.enabled;
                final View map;
                LayoutInflater inflater = activity.getLayoutInflater();
                if (mMapEnabled) {
//                    if (!SKMaps.getInstance().isSKMapsInitialized()) {
//                        Utils.initializeLibrary(getActivity(), null);
//                    }
                    boundingBoxUS = new SKBoundingBox(new SKCoordinate(49.384358, -124.848974), new SKCoordinate(24.396308, -66.885444));
                    map = inflater.inflate(R.layout.partial_map, null);
                    final FrameLayout holder = (FrameLayout) view.findViewById(R.id.frameLayout);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            holder.removeAllViews();
                            holder.addView(map);
//                            mapViewGroup = (SKMapViewHolder) map.findViewById(R.id.view_group_map);
//                            mapViewGroup.setMapSurfaceListener(MapFragment.this);

                            recordButton.setVisibility(View.VISIBLE);
                            positionButton.setVisibility(View.VISIBLE);
                            positionButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    onPositionerClicked(null);
                                }
                            });
                        }
                    });
                } else {
//                    SKMaps.getInstance().destroySKMaps();
                    map = inflater.inflate(R.layout.partial_map_placeholder, null);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FrameLayout holder = (FrameLayout) view.findViewById(R.id.frameLayout);
                            holder.removeAllViews();
                            holder.addView(map);
                            recordButton.setVisibility(View.VISIBLE);
                            positionButton.setVisibility(View.INVISIBLE);
//                            if (mapViewGroup != null) {
//                                mapViewGroup.setMapSurfaceListener(null);
//                            }
                        }
                    });
                }
            }
        });

        EventBus.clear(SdkEnabledEvent.class);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onRecordingStatusChanged(final RecordingEvent event) {
        mRecording = event.started;
        if (!event.started /*&& mapView != null*/) {
            Log.d(TAG, "onRecordingStatusChanged: deleting polyline");
//            mapView.clearOverlay(TRACK_POLYLINE_ID);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onBoundingBoxesChanged(BoundingBoxChangedEvent event) {
//        if (mapView != null) {
//            mapView.addPolyline(event.lastBB);
//            mapView.addPolyline(event.requestedBB);
//            mapView.addPolyline(event.smallBB);
//        }
    }

    @Subscribe(sticky = true)
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (!event.online /*&& mapView != null*/) {
//            mapView.clearOverlay(event.deletedSequenceId);
            EventBus.clear(SequencesChangedEvent.class);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onUploadFinished(UploadFinishedEvent event) {
//        if (mapView != null) {
//            mapView.clearAllOverlays();
            mLastBB = null;
            diplayLocalSequences();
            refreshDisplayedSequences();
//        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onMatched(MatchedSegmentEvent event) {
        if (Utils.DEBUG) {
            Polyline polyline = event.polyline;
            if (mMatchedPolyline != null) {
//                if (mapView != null) {
                    mMatchedPolyline.isLocal = false;
                    // set polyline color
//                polyline.setColor(new float[]{0.447f, 0.447f, 0.447f, 1.0f}); grey
                    mMatchedPolyline.setColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, 1f}); //washed out accent color
                    mMatchedPolyline.setOutlineColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, 1f}); //washed out accent color
                    mMatchedPolyline.setOutlineSize(3);
                    mMatchedPolyline.setOutlineDottedPixelsSolid(50000);
                    mMatchedPolyline.setOutlineDottedPixelsSkip(1);
                    mMatchedPolyline.setLineSize(3);
//                    mapView.addPolyline(mMatchedPolyline);
//                }
            }
            mMatchedPolyline = polyline;
            if (/*mapView != null && */mMatchedPolyline != null) {
                mMatchedPolyline.isLocal = false;
                // set polyline color
//                polyline.setColor(new float[]{0.447f, 0.447f, 0.447f, 1.0f}); grey
                mMatchedPolyline.setColor(new float[]{227f / 255f, 142f / 255f, 104f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                mMatchedPolyline.setOutlineColor(new float[]{227f / 255f, 142f / 255f, 104f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                mMatchedPolyline.setOutlineSize(3);
                mMatchedPolyline.setOutlineDottedPixelsSolid(50000);
                mMatchedPolyline.setOutlineDottedPixelsSkip(1);
                mMatchedPolyline.setLineSize(5);
//                mapView.addPolyline(mMatchedPolyline);
            }
            if (event.all != null && !mReceivedSegments) {
                mReceivedSegments = true;
                for (Polyline poly : event.all) {
//                    Log.d(TAG, "onMatched: displaying received polylines");
                    displayPolyline(poly);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSegmentsReceived(SegmentsReceivedEvent event) {
        if (!mReceivedSegments /*&& mapView != null*/) {
            this.mReceivedSegments = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onSegmentReceived(SegmentEvent event) {
//        Log.d(TAG, "onSegmentReceived: displaying polylines");
        displayPolyline(event.polyline);
    }

    public void setRecorder(Recorder recorder) {
        mRecorder = recorder;
    }

    /**
     * helper impl.
     */

    public void resizeStarted() {
//        if (mapViewGroup != null) {
//            mapViewGroup.setVisibility(View.GONE);
//        }
        if (chessBackground != null) {
            chessBackground.setVisibility(View.VISIBLE);
        }
        if (recordButton != null && positionButton != null) {
            recordButton.setVisibility(View.INVISIBLE);
            positionButton.setVisibility(View.INVISIBLE);
        }
    }

    public void resizeStopped() {
//        if (mapViewGroup != null) {
//            mapViewGroup.setVisibility(View.VISIBLE);
//        }
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (chessBackground != null) {
                    chessBackground.setVisibility(View.GONE);
                }
                switch (mCurrentMode) {
                    case MODE_IDLE:
                        if (recordButton != null && positionButton != null) {
                            recordButton.setVisibility(View.VISIBLE);
                            positionButton.setVisibility(View.VISIBLE);
                        }
                        break;
                    case MODE_RECORDING_SCREEN:
                    case MODE_TRACK_PREVIEW:
                        if (recordButton != null && positionButton != null) {
                            recordButton.setVisibility(View.INVISIBLE);
                            positionButton.setVisibility(View.INVISIBLE);
                        }
                        break;
                }
            }
        }, 300);
    }

    public int getCurrentMode() {
        return mCurrentMode;
    }
}
