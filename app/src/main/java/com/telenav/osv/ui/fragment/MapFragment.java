package com.telenav.osv.ui.fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.api.Status;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKAnnotationView;
import com.skobbler.ngx.map.SKBoundingBox;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKPolyline;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.skobbler.ngx.trail.SKTrailManager;
import com.skobbler.ngx.trail.SKTrailType;
import com.telenav.osv.R;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.listener.LoadAllSequencesListener;
import com.telenav.osv.listener.LocationEventListener;
import com.telenav.osv.manager.PlaybackManager;
import com.telenav.osv.ui.list.ImageListAdapter;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * Created by Kalman on 11/9/15.
 */
public class MapFragment extends Fragment implements SKMapSurfaceListener, SensorEventListener, LocationEventListener, SKNavigationListener, PlaybackManager.PlaybackListener {

    public final static String TAG = "MapFragment";

    /**
     * id for selected image marker annotation
     */
    public static final byte VIA_POINT_ICON_ID = 4;

    public static final int MODE_IDLE = 0;

    public static final int MODE_PREVIEW = 1;

    public static final int MODE_RECORDING = 2;

    /**
     * starting point annotation id modifier
     */
    private static final byte GREEN_PIN_ICON_ID = 0;

    /**
     * end point annotation id modifier
     */
    private static final byte RED_PIN_ICON_ID = 1;

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

    /**
     * true, if compass mode is available
     */
    public static boolean compassAvailable;

    public Sequence mCurrentSequence;

    /**
     * fragment's view
     */
    public View view;

    public boolean mRecordingMode = false;

    /**
     * the view that holds the map view
     */
    private SKMapViewHolder mapViewGroup;

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
    private SKMapSurfaceView mapView;

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

    private MainActivity activity;

    private ImageListAdapter imageListAdapter;

    /**
     * custom annotation view
     */
    private SKAnnotationView redAnnotationView;

    /**
     * custom annotation view
     */
    private SKAnnotationView greenAnnotationView;

    /**
     * custom annotation view
     */
    private SKAnnotationView blueAnnotationView;

    private boolean mIsMapCreated;

    private HashMap<Integer, Polyline> mLocalSequences = new HashMap<>();

    private boolean needsToRefreshSequences = false;


    private ArrayList<ImageView> annotationViewList;

    private SKAnnotation mSelectedPositionAnnotation;

    private boolean mAppbarShown = false;

//    private ProgressBar mLoadingIndicator;

    private boolean mNeedsToSelectSequence = false;

    private FullscreenPreviewFragment.OnPageSelectedListener mFullscreenListener;

    private Handler mBackgroundHandler;

    private Polyline dottedPolyline;

    private FloatingActionButton recordButton;

    private FloatingActionButton positionButton;

    private boolean mIsSmall = false;

    private SKBoundingBox boundingBoxUS;

    private boolean isFirstPosition = true;

    private PlaybackManager mPlayer;

    private int mCurrentMode = MODE_IDLE;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_map, null);
        Log.d(TAG, "onCreateView: ");
        activity = (MainActivity) getActivity();
        appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
        mapViewGroup = (SKMapViewHolder) view.findViewById(R.id.view_group_map);
        mapViewGroup.setMapSurfaceListener(this);
        HandlerThread handlerThread = new HandlerThread("Loader", Thread.NORM_PRIORITY);
        handlerThread.start();
        mBackgroundHandler = new Handler(handlerThread.getLooper());
        dottedPolyline = new Polyline(10);
        mapViewGroup.onResume();
        if (headingOn) {
            startOrientationSensor();
        }
        recordButton = (FloatingActionButton) view.findViewById(R.id.record_button);
        recordButton.bringToFront();
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.openScreen(MainActivity.SCREEN_RECORDING);
            }
        });
        positionButton = (FloatingActionButton) view.findViewById(R.id.position_button);
        positionButton.bringToFront();
        positionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPositionerClicked();
            }
        });
        if (activity.mCameraHandlerService != null && activity.mCameraHandlerService.mShutterManager != null && activity.mCameraHandlerService.mShutterManager.isRecording()) {
            mRecordingMode = true;
        }
        if (mCurrentMode != MODE_IDLE) {
            recordButton.setVisibility(View.INVISIBLE);
            positionButton.setVisibility(View.INVISIBLE);
        } else {
            recordButton.setVisibility(View.VISIBLE);
            positionButton.setVisibility(View.VISIBLE);
        }
        boundingBoxUS = new SKBoundingBox(49.384358, -124.848974, 24.396308, -66.885444);
        activity.getApp().getLocationManager().setLocationListener(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (activity != null) {
            activity.getApp().getLocationManager().startLocationUpdates();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        activity.getApp().getLocationManager().setLocationListener(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mapViewGroup.onPause();
        if (headingOn) {
            stopOrientationSensor();
        }
        if (compassAvailable) {
            stopOrientationSensor();
        }
        if (annotationViewList != null) {
            annotationViewList.clear();
            annotationViewList = null;
        }
        super.onDestroy();
    }

    public void onPositionerClicked() {
        if (mIsMapCreated) {
            if (!activity.getApp().getLocationManager().isGPSEnabled()) {
                activity.resolveLocationProblem(false);
            } else {
                if (activity.getApp().getLocationManager().hasPosition()) {
                    mapView.centerMapOnCurrentPositionSmooth(17, 1000);//zoomlevel, anim time
                } else {
                    activity.showSnackBar("Waiting for GPS position...", Snackbar.LENGTH_SHORT);
                }
            }
        }
    }

//    public void editCurrentlyDisplayedSequence() {
//        if (mCurrentSequence != null) {
//            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
//            if (!mCurrentSequence.online) {
//                ft.addToBackStack(ImageGridFragment.TAG);
//                ImageGridFragment fragment = new ImageGridFragment();
//                fragment.setSource(imageListAdapter.getData(), 0); //imageListView.getSelectedItemPosition());
//                fragment.onUploadServiceConnected(activity.mUploadHandlerService);
//                ft.replace(R.id.content_frame_upper, fragment, ImageGridFragment.TAG).commit();
//            } else {
//                ft.addToBackStack(ImageGridOnlineFragment.TAG);
//                ImageGridOnlineFragment fragment = new ImageGridOnlineFragment();
//                fragment.setSource(imageListAdapter.getData(), 0); //imageListView.getSelectedItemPosition());
//
//                ft.replace(R.id.content_frame_upper, fragment, ImageGridOnlineFragment.TAG).commit();
//            }
//            activity.getSupportFragmentManager().executePendingTransactions();
//        }
//    }


    @Override
    public void onSurfaceCreated(SKMapViewHolder mapHolder) {
        Log.d(TAG, "onSurfaceCreated: ");
        View chessBackground = view.findViewById(R.id.chess_board_background);
        chessBackground.setVisibility(View.GONE);
        mapView = mapHolder.getMapSurfaceView();
        mapView.setZOrderMediaOverlay(true);
        if (mCurrentMode == MODE_IDLE) {
            mapHolder.showAllAttributionTextViews();
            positionButton.setVisibility(View.VISIBLE);
            recordButton.setVisibility(View.VISIBLE);
//            mapView.post(new Runnable() {
//                @Override
//                public void run() {
//                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
//                }
//            });
            applySettingsOnMapView();

            final double lat = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT);
            final double lon = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON);
            if (lat == 0 && lon == 0) {
                mapView.setZoom(1);
                mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
            } else {
                SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(lon, lat));
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (lat != 0 && lon != 0) {
                        mapView.centerMapOnCurrentPositionSmooth(17, 1000);//zoomlevel, anim time
                    }
                    mapView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            diplayLocalSequences();
//                            refreshDisplayedSequences();//a
                            FragmentManager fm = activity.getSupportFragmentManager();
                            if (activity.getCurrentFragment().equals(TAG) && fm != null && fm.getBackStackEntryCount() == 1) {
                                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_TAP_ON_MAP, false)) {
                                    activity.showSnackBar(R.string.tip_map_screen, Snackbar.LENGTH_LONG, "Got it", new Runnable() {
                                        @Override
                                        public void run() {
                                            appPrefs.saveBooleanPreference(PreferenceTypes.K_HINT_TAP_ON_MAP, true);
                                        }
                                    });
                                }
                            }
                        }
                    }, 1040);
                }
            }, 1000);

            mapView.clearAllOverlays();
            //fix for black textures, we keep a ref to the image views
            annotationViewList = new ArrayList<ImageView>();
            redAnnotationView = new SKAnnotationView();
            ImageView iv = new ImageView(activity);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            iv.setImageDrawable(activity.getResources().getDrawable(R.drawable.icon_point_end));
            redAnnotationView.setView(iv);
            annotationViewList.add(iv);
            greenAnnotationView = new SKAnnotationView();
            ImageView iv2 = new ImageView(activity);
            iv2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            iv2.setImageDrawable(activity.getResources().getDrawable(R.drawable.icon_point_start));
            greenAnnotationView.setView(iv2);
            annotationViewList.add(iv2);
            blueAnnotationView = new SKAnnotationView();
            ImageView iv3 = new ImageView(activity);
            iv3.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            iv3.setImageDrawable(activity.getResources().getDrawable(R.drawable.icon_point_position));
            blueAnnotationView.setView(iv3);
            annotationViewList.add(iv3);
            activity.continueAfterCrash();
            if (mIsMapCreated) {
                diplayLocalSequences();
//                refreshDisplayedSequences();
                if (SKNavigationManager.getInstance().getNavigationMode() == SKNavigationSettings.SKNavigationMode.CAR.getValue()) {
                    SKNavigationManager.getInstance().stopNavigation();
                }
                setHeading(false);
                mapView.getMapSettings().setMapRotationEnabled(true);
                mapView.getMapSettings().setMapPanningEnabled(true);
                mapView.getMapSettings().setMapZoomingEnabled(true);
                SKTrailManager.getInstance().setTrailType(new SKTrailType(true, new float[]{104f / 255f, 189f / 255f, 227f / 255f, 1.0f}, 5));
                SKTrailManager.getInstance().setShowTrail(false);
                SKTrailManager.getInstance().clearTrail();
//                SKTrailType sk = new SKTrailType()
//                SKTrailSettings trailSettings = new SKTrailSettings();
//                trailSettings.setPedestrianTrailEnabled(false, 1);
//                mapView.getMapSettings().setTrailSettings(trailSettings);
                mapView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (lat == 0 && lon == 0) {
                            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
                        } else {
                            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
                        }
                    }
                });
            }
            mIsMapCreated = true;
        } else {
            mapHolder.hideAllAttributionTextViews();
            positionButton.setVisibility(View.GONE);
            recordButton.setVisibility(View.GONE);
            if (mCurrentMode == MODE_RECORDING) {
                ((OSVApplication) activity.getApplication()).getUploadManager().cancelTracks();
                mBackgroundHandler.removeCallbacksAndMessages(null);
                mLocalSequences.clear();
                mapView.clearAllOverlays();
                mapView.deleteAllAnnotationsAndCustomPOIs();
                mapView.setZOrderMediaOverlay(true);
                SKNavigationSettings navSettings = new SKNavigationSettings();
                navSettings.setNavigationMode(SKNavigationSettings.SKNavigationMode.CAR);
                navSettings.setNavigationType(SKNavigationSettings.SKNavigationType.REAL);
                SKNavigationManager.getInstance().setNavigationListener(this);
                SKNavigationManager.getInstance().startNavigation(navSettings);
                setHeading(true);
                mapView.getMapSettings().setMapRotationEnabled(false);
                mapView.getMapSettings().setMapPanningEnabled(false);
                mapView.getMapSettings().setMapZoomingEnabled(false);
                SKTrailManager.getInstance().setTrailType(new SKTrailType(false, new float[]{104f / 255f, 189f / 255f, 227f / 255f, 1.0f}, 5));
                SKTrailManager.getInstance().setShowTrail(true);
//            SKTrailSettings trailSettings = new SKTrailSettings();
//            trailSettings.setPedestrianTrailEnabled(true, 1);
//            mapView.getMapSettings().setTrailSettings(trailSettings);
                mapView.setZoom(16);
                mapView.post(new Runnable() {
                    @Override
                    public void run() {
                        mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_ARROW_SMALL);
                    }
                });
            } else if (mCurrentMode == MODE_PREVIEW && mPlayer != null) {
                displaySequence(mPlayer.getTrack(), !mPlayer.isOnline());
            }
        }
    }

    /**
     * reads and displayes all locally cached sequences from the files system
     */
    private void diplayLocalSequences() {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (SequenceDB.instance != null) {
                    synchronized (Sequence.getStaticSequences()) {
                        mLocalSequences.clear();
                        for (Sequence sequence : Sequence.getStaticSequences().values()) {
                            if (sequence.originalImageCount > 0) {
                                try {
                                    mLocalSequences.put(sequence.sequenceId, sequence.polyline);
                                    displayPolyline(sequence.polyline.getNodes(), sequence.sequenceId * -1, true);
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

    /**
     * displays local and online sequences
     */
    public void refreshDisplayedSequences(final SKBoundingBox boundingBox) {
        if (activity == null) {
            return;
        }
        ((OSVApplication) activity.getApplication()).getUploadManager().cancelTracks();
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mapView != null) {
                    Log.d(TAG, "refreshDisplayedSequences: sending request");
                    ((OSVApplication) activity.getApplication()).getUploadManager().listTracks(new LoadAllSequencesListener() {
                        @Override
                        public void onRequestFinished(final Polyline polyline, final int id) {
//                            activity.runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
                            if (polyline != null) {
                                displayPolyline(polyline.getNodes(), id, false);
                            } else {
                                mapView.clearAllOverlays();
                            }
//                                }
//                            });
                        }

                        @Override
                        public void onFinished() {

                        }
                    }, boundingBox.getTopLeftLatitude() + "," + boundingBox.getTopLeftLongitude(), boundingBox.getBottomRightLatitude() + ","
                            + boundingBox.getBottomRightLongitude(), 1, mapView.getZoomLevel());
                }
            }
        });
    }


    /**
     * Customize the map view
     */

    private void applySettingsOnMapView() {
        mapView.getMapSettings().setMapRotationEnabled(true);
        mapView.getMapSettings().setMapZoomingEnabled(true);
        mapView.getMapSettings().setMapPanningEnabled(true);
        mapView.getMapSettings().setZoomWithAnchorEnabled(true);
        mapView.getMapSettings().setInertiaRotatingEnabled(true);
        mapView.getMapSettings().setInertiaZoomingEnabled(true);
        mapView.getMapSettings().setInertiaPanningEnabled(true);
        changeMapStyle();
    }

    /**
     * updates the map style and save it in preferences
     */
    public void changeMapStyle() {
        updateMapStyle(new SKMapViewStyle(SplashActivity.mapResourcesDirPath + "grayscalestyle" + "/", "grayscalestyle.json"));
    }

    /**
     * updates the current map style to the new style given as parameter
     * @param style - the new map style
     */
    private void updateMapStyle(SKMapViewStyle style) {
        SKMapSettings skMapSettings = mapView.getMapSettings();
        String currentMapStyle = skMapSettings.getMapStyle().getStyleFileName();
        String nextMapStyle = style.getStyleFileName();

        if (!currentMapStyle.equals(nextMapStyle)) {
            skMapSettings.setMapStyle(style);
        }
    }

    @Override
    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {
        if (mCurrentMode == MODE_IDLE && mapView != null) {
            SKBoundingBox bb = mapView.getBoundingBoxForRegion(skCoordinateRegion);
            refreshDisplayedSequences(bb);
        }
    }

    // map interaction callbacks ...
    @Override
    public void onActionPan() {
//        Log.d(TAG, "onActionPan: ");
        if (headingOn) {
            setHeading(false);
        }
        needsToRefreshSequences = true;
    }

    @Override
    public void onActionZoom() {
//        Log.d(TAG, "onActionZoom: ");
        needsToRefreshSequences = true;
    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI customPoi) {
        if (mRecordingMode || mIsSmall) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onCompassSelected() {

    }

    @Override
    public void onCurrentPositionSelected() {
        if (mRecordingMode || mIsSmall) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onObjectSelected(int i) {
        if (mRecordingMode || mIsSmall) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onInternationalisationCalled(int i) {

    }

    @Override
    public void onDebugInfo(double v, float v1, double v2) {

    }

    @Override
    public void onBoundingBoxImageRendered(int i) {

    }

    @Override
    public void onGLInitializationError(String s) {

    }

    @Override
    public void onScreenshotReady(Bitmap bitmap) {

    }

    @Override
    public void onDoubleTap(SKScreenPoint point) {
        if (mRecordingMode || mIsSmall) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {
        if (mRecordingMode && mIsSmall) {
            activity.switchPreviews();
            return;
        }
        if (mRecordingMode) {
            return;
        }
        if (mapView == null) {
            return;
        }

        if (activity.getCurrentFragment().equals(TAG)) {
            activity.enableProgressBar(true);
            SKCoordinate tappedCoords = mapView.pointToCoordinate(skScreenPoint);
            ((OSVApplication) activity.getApplication()).getUploadManager().nearby(new RequestResponseListener() {
                @Override
                public void requestFinished(int status, String result) {
                    Log.d(TAG, "nearby: " + result);
                    activity.enableProgressBar(false);
                    boolean ok = false;
                    try {
                        JSONObject obj = new JSONObject(result);
                        JSONObject osv = obj.getJSONObject("osv");
                        ok = true;
                    } catch (Exception e) {
                        Log.d(TAG, "requestFinished: ");
                    }
                    if (ok) {
                        activity.openScreen(MainActivity.SCREEN_NEARBY, result);
                    } else {
                        activity.showSnackBar(getString(R.string.nearby_no_result_label), Snackbar.LENGTH_SHORT);
                    }

                }

                @Override
                public void requestFinished(int status) {
                    Log.d(TAG, "nearby: error");
                    activity.enableProgressBar(false);

                }
            }, "" + tappedCoords.getLatitude(), "" + tappedCoords.getLongitude(), 50);
        }
    }

    @Override
    public void onRotateMap() {

    }

    @Override
    public void onInternetConnectionNeeded() {

    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {
//        mNeedsToSelectSequence = true;
    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {
        if (mRecordingMode && mIsSmall) {
            activity.switchPreviews();
            return;
        }
        if (mRecordingMode) {
            return;
        }
    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {
        if (mRecordingMode && mIsSmall) {
            activity.switchPreviews();
            return;
        }
        if (mRecordingMode) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {
        if (mRecordingMode && mIsSmall) {
            activity.switchPreviews();
            return;
        }
        if (mRecordingMode) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {
        if (mRecordingMode && mIsSmall) {
            activity.switchPreviews();
            return;
        }
        if (mRecordingMode) {
            return;
        }
        mNeedsToSelectSequence = true;
    }

    @Override
    public void onLongPress(SKScreenPoint point) {
        if (mRecordingMode || mIsSmall) {
            return;
        }
        mNeedsToSelectSequence = true;
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
     * @param enabled
     */
    private void setHeading(boolean enabled) {
        if (enabled) {
            headingOn = true;
            mapView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NAVIGATION);
            startOrientationSensor();
        } else {
            headingOn = false;
            mapView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE);
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
                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.PORTRAIT);
                                        break;
                                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.PORTRAIT_UPSIDEDOWN);
                                        break;
                                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.LANDSCAPE_RIGHT);
                                        break;
                                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                                        mapView.reportNewDeviceOrientation(SKMapSurfaceView.SKOrientationType.LANDSCAPE_LEFT);
                                        break;
                                }
                            }

                            // report to NG the new value
                            if (orientationValues[0] < 0) {
                                mapView.reportNewHeading(-orientationValues[0]);
                            } else {
                                mapView.reportNewHeading(orientationValues[0]);
                            }

                            lastTimeWhenReceivedGpsSignal = System.currentTimeMillis();
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(location.getLongitude(), location.getLatitude()));
        if (mapView != null && !mRecordingMode) {
            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
        }
        if (isFirstPosition) {
            isFirstPosition = false;
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            if (!appPrefs.getBooleanPreference(PreferenceTypes.K_RUN_COUNTER)) {
                appPrefs.saveBooleanPreference(PreferenceTypes.K_RUN_COUNTER, true);
                setMetrics(latitude, longitude);
            }
            setSignDetectionRegion(latitude, longitude);

        }

    }

    @Override
    public void onResolutionNeeded(Status status) {
        activity.setLocationResolution(status);
    }

    private void setMetrics(double latitude, double longitude) {
        if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeftLatitude(), boundingBoxUS.getTopLeftLongitude(), boundingBoxUS.getBottomRightLatitude(),
                boundingBoxUS.getBottomRightLongitude())) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, false);
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, true);
        }
    }

    private void setSignDetectionRegion(double latitude, double longitude) {
        //TODO reinitialize sign detection
        if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeftLatitude(), boundingBoxUS.getTopLeftLongitude(), boundingBoxUS.getBottomRightLatitude(),
                boundingBoxUS.getBottomRightLongitude())) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_US, true);
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_SIGN_DETECTION_US, false);
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
//            activity.openScreen(MainActivity.SCREEN_MAP);
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
    public void displaySequence(final ArrayList<SKCoordinate> nodes, boolean isLocal) {
        Log.d(TAG, "displaySequence: ");
        // set the nodes on the polyline
        if (nodes != null && nodes.size() != 0 && mapView != null) {
            mCurrentSequence = mPlayer.getSequence();
            int sequenceId = mCurrentSequence.sequenceId;
            mLocalSequences.clear();
            mapView.clearAllOverlays();
            mapView.deleteAllAnnotationsAndCustomPOIs();
//            if (!fromMap) {
//                activity.showActionBar(activity.getResources().getString(R.string.overview_label), true);
//            } else {
//                activity.hideActionBar();
//            }
            mPreviewNodes = (ArrayList<ImageCoordinate>) nodes.clone();
            final ArrayList<SKCoordinate> coords = new ArrayList<>();
            for (SKCoordinate img : nodes) {
                coords.add(img);
            }
            displayPolyline(coords, sequenceId, isLocal);
            zoomToPolyline(coords, (int) Utils.dpToPx(activity, 15), (int) Utils.dpToPx(activity, 15));
            mapView.deleteAnnotation(VIA_POINT_ICON_ID);
            mSelectedPositionAnnotation = new SKAnnotation(VIA_POINT_ICON_ID);
            mSelectedPositionAnnotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_MARKER);
            mSelectedPositionAnnotation.setAnnotationView(blueAnnotationView);
            mSelectedPositionAnnotation.setLocation(nodes.get(0));
            mapView.addAnnotation(mSelectedPositionAnnotation, SKAnimationSettings.ANIMATION_PULSE_CCP);
        } else {
            Log.d(TAG, "displaySequence: nodes size is 0");
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

        SKBoundingBox boundingBox = new SKBoundingBox(northLat, westLon, southLat, eastLon);
        mapView.fitBoundingBox(boundingBox, paddingX, paddingY);
    }

    /**
     * displayes a polyline for a list of coords
     * @param list of coordinates
     * @param id id of the sequence, local or online if available
     * @param local true if sequence is not uploaded yet
     */
    public void displayPolyline(List<?> list, int id, boolean local) {
//        Log.d(TAG, "displayPolyline and add annotation : id " + id + " isLocal : " + local);
        if (!mRecordingMode) {
            if (list != null && !list.isEmpty()) {
                Polyline polyline = new Polyline(id);
                polyline.setNodes((List<SKCoordinate>) list);
                polyline.isLocal = local;
                // set polyline color
                if (local) {
//                polyline.setColor(new float[]{0.447f, 0.447f, 0.447f, 1.0f}); grey
                    polyline.setColor(new float[]{227f / 255f, 142f / 255f, 104f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                    polyline.setOutlineColor(new float[]{227f / 255f, 142f / 255f, 104f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                } else {
//                polyline.setColor(new float[]{0.714f, 0.714f, 0.714f, 1.0f});  //grey
//                polyline.setColor(new float[]{3f/255f, 169f/255f, 244f/255f, 1.0f}); //accent color
                    polyline.setColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, 1.0f}); //washed out accent color
                    polyline.setOutlineColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, 1.0f}); //washed out accent color
                }
                polyline.setOutlineSize(3);
                polyline.setOutlineDottedPixelsSolid(50000);
                polyline.setOutlineDottedPixelsSkip(1);
                polyline.setLineSize(3);
                polyline.setIdentifier(id);
                mapView.addPolyline(polyline);
            }
        }
    }

    /**
     * removes a polyline for an id
     * @param id id of the sequence
     */
    public void removePolyline(int id) {
        mapView.deleteAnnotation(GREEN_PIN_ICON_ID + id);
        mapView.deleteAnnotation((RED_PIN_ICON_ID + id) * -1);
        mapView.clearOverlay(id);

    }

    /**
     * removes the currently viewed sequence and displayes all sequences
     */
    public void removeSequence() {
//        MainActivity.sLastSequence = -1;
//        MainActivity.sLastSequenceIndex = 0;
        int currentSeqId = -1;
        if (mCurrentSequence != null) {
            currentSeqId = mCurrentSequence.sequenceId;
        }
//        imageListView.setVisibility(View.INVISIBLE);
        // activity.hideActionBar();
//        centerOnPositionButton.hide(false);//show(true);
//        centerOnPositionButton.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_gps_fixed_white_24dp));
        mCurrentSequence = null;
        if (mPreviewNodes != null) {
            mPreviewNodes.clear();
        }
        needsToRefreshSequences = false;
        if (mLocalSequences != null) {
            mLocalSequences.clear();
        }
        if (mapView != null) {
            mapView.deleteAllAnnotationsAndCustomPOIs();
            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
        }
        System.gc();
        diplayLocalSequences();
    }

    /**
     * views the image from startIndex, moving the marker on the map
     * @param index startIndex of the image in the sequence list
     */
    private void viewFrame(int index) {
        if (mPreviewNodes == null || mPreviewNodes.size() <= index) {
            return;
        }
        mSelectedPositionAnnotation.setLocation(mPreviewNodes.get(index));
        mapView.updateAnnotation(mSelectedPositionAnnotation);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        if (imageListView != null) {
//            imageListView.refreshHeader();
//        }
    }

    @Override
    public void onDestinationReached() {

    }

    @Override
    public void onSignalNewAdviceWithInstruction(String s) {

    }

    @Override
    public void onSignalNewAdviceWithAudioFiles(String[] strings, boolean b) {

    }

    @Override
    public void onSpeedExceededWithAudioFiles(String[] strings, boolean b) {

    }

    @Override
    public void onSpeedExceededWithInstruction(String s, boolean b) {

    }

    @Override
    public void onUpdateNavigationState(SKNavigationState skNavigationState) {

    }

    @Override
    public void onReRoutingStarted() {

    }

    @Override
    public void onFreeDriveUpdated(String s, String s1, String s2, SKNavigationState.SKStreetType skStreetType, double v, double v1) {

        dottedPolyline.setLineSize(0);
        dottedPolyline.setOutlineSize(14);
        dottedPolyline.setOutlineDottedPixelsSkip(25);
        dottedPolyline.setOutlineDottedPixelsSolid(25);
        dottedPolyline.setColor(new float[]{0f / 255f, 122f / 255f, 255f / 255f, 1.0f}); //washed out accent color
        dottedPolyline.setOutlineColor(new float[]{0f / 255f, 122f / 255f, 255f / 255f, 1.0f});
        mapView.addPolyline(dottedPolyline);

        SKPosition position = SKPositionerManager.getInstance().getCurrentGPSPosition(true);
        dottedPolyline.getNodes().add(position.getCoordinate());


    }

    @Override
    public void onViaPointReached(int i) {

    }

    @Override
    public void onVisualAdviceChanged(boolean b, boolean b1, SKNavigationState skNavigationState) {

    }

    @Override
    public void onTunnelEvent(boolean b) {

    }

    @Override
    public void onFcdTripStarted(String s) {

    }

    public void setSource(PlaybackManager source) {
        mPlayer = source;
        mPlayer.addPlaybackListener(this);
    }

    public void setMapSmall(boolean small) {
        mIsSmall = small;
    }

    public void enterMode(int mode) {
        Log.d(TAG, "enterMode: " + mode);
        mCurrentMode = mode;
        switch (mode) {
            case MODE_IDLE:
                removeSequence();
                mRecordingMode = false;
                recordButton.setVisibility(View.VISIBLE);
                positionButton.setVisibility(View.VISIBLE);
                break;
            case MODE_RECORDING:
                mRecordingMode = true;
                mapView.bringToFront();
                recordButton.setVisibility(View.INVISIBLE);
                positionButton.setVisibility(View.INVISIBLE);
                break;
            case MODE_PREVIEW:
                recordButton.setVisibility(View.INVISIBLE);
                positionButton.setVisibility(View.INVISIBLE);
                break;
        }
    }

    public int getMode() {
        return mCurrentMode;
    }

    @Override
    public void onPlaying() {

    }

    @Override
    public void onPaused() {

    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onPrepared() {
        Log.d(TAG, "onPrepared: ");
        if (mapView != null) {
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    displaySequence(mPlayer.getTrack(), !mPlayer.isOnline());
                }
            });
        }
    }

    @Override
    public void onProgressChanged(int index) {
        Log.d(TAG, "onProgressChanged: " + index);
        viewFrame(index);
    }

    @Override
    public void onExit() {
        removeSequence();
    }

    public void bringToFront() {
        if (mapView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mapView.setZ(5);
                mapView.setTranslationZ(5);
            }
            mapView.bringToFront();
            mapView.setZOrderMediaOverlay(true);
        }
    }

    public void sendToBack() {
        if (mapView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mapView.setZ(0);
                mapView.setTranslationZ(0);
            }
            mapView.setZOrderMediaOverlay(false);
        }
    }

    /**
     * helper impl.
     */
    public static class Polyline extends SKPolyline {
        public boolean isLocal = false;

        public Polyline(int identifier) {
            super();
            setIdentifier(identifier);
            setNodes(new ArrayList<SKCoordinate>());
        }
    }


}
