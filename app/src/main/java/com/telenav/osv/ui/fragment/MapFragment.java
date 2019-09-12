package com.telenav.osv.ui.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.SKMapsInitializationListener;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKAnnotationView;
import com.skobbler.ngx.map.SKBoundingBox;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapInternationalizationSettings;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.map.maplistener.SKAnnotationListener;
import com.skobbler.ngx.map.maplistener.SKCurrentPositionSelectedListener;
import com.skobbler.ngx.map.maplistener.SKInternetConnectionListener;
import com.skobbler.ngx.map.maplistener.SKMapActionListener;
import com.skobbler.ngx.map.maplistener.SKMapRegionChangedListener;
import com.skobbler.ngx.map.maplistener.SKMapSurfaceCreatedListener;
import com.skobbler.ngx.map.maplistener.SKMapTapListener;
import com.skobbler.ngx.map.maplistener.SKObjectSelectedListener;
import com.skobbler.ngx.map.maplistener.SKPOIListener;
import com.skobbler.ngx.map.maplistener.SKPanListener;
import com.skobbler.ngx.map.maplistener.SKZoomListener;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.skobbler.ngx.util.SKLanguage;
import com.telenav.osv.R;
import com.telenav.osv.activity.LocationPermissionsListener;
import com.telenav.osv.activity.MainActivity;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.BroadcastSegmentsCommand;
import com.telenav.osv.common.Injection;
import com.telenav.osv.common.filter.FilterFactory;
import com.telenav.osv.data.location.datasource.LocationLocalDataSource;
import com.telenav.osv.data.location.model.OSVLocation;
import com.telenav.osv.data.sequence.model.Sequence;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.SdkEnabledEvent;
import com.telenav.osv.event.network.matcher.BoundingBoxChangedEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.SegmentsReceivedEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.network.GeometryCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.location.LocationService;
import com.telenav.osv.location.filter.LocationFilterType;
import com.telenav.osv.manager.network.GeometryRetriever;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.ScreenComposer;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Fragment holding the map or the placeholder
 * Created by Kalman on 11/9/15.
 */
public class MapFragment extends DisplayFragment implements SKCurrentPositionSelectedListener, SKObjectSelectedListener, SKMapRegionChangedListener, SKAnnotationListener,
        SKZoomListener, SKInternetConnectionListener, SKMapTapListener, SKMapActionListener, SKPanListener, SKPOIListener,
        SKMapSurfaceCreatedListener, PlaybackManager.PlaybackListener, LocationPermissionsListener {

    public final static String TAG = "MapFragment";

    public static final int LOCAL_ID_INCREMENT_VALUE = 1;

    /**
     * id for selected image marker annotation
     */
    private static final byte VIA_POINT_ICON_ID = 4;

    private static final int MODE_IDLE = 0;

    private static final int MODE_TRACK_PREVIEW = 1;

    private static final int MODE_RECORDING_SCREEN = 2;

    private static final int TRACK_POLYLINE_ID = 10000156;

    private static final int MATCHED_SEGMENT_POLYLINE_ID = 10000157;

    /**
     * fragment's view
     */
    private View view;

    private boolean mIsSmall = false;

    /**
     * the view that holds the map view
     */
    private SKMapViewHolder mapViewGroup;

    /**
     * Surface view for displaying the map
     */
    private SKMapSurfaceView mapView;

    /**
     * the app prefs
     */
    private ApplicationPreferences appPrefs;

    /**
     * the list of images with coordinates used for previewing a sequence
     */
    private ArrayList<ImageCoordinate> mPreviewNodes;

    private OSVActivity activity;

    private ArrayList<ImageView> annotationViewList;

    private SKAnnotation mSelectedPositionAnnotation = new SKAnnotation(VIA_POINT_ICON_ID);

    private FloatingActionButton recordButton;

    private FloatingActionButton positionButton;

    private SKBoundingBox boundingBoxUS;

    private boolean noPositionYet = true;

    private PlaybackManager mPlayer;

    private int mCurrentMode = MODE_IDLE;

    private Polyline mMatchedPolyline;

    private boolean mMapEnabled;

    private View chessBackground;

    private GeometryRetriever mGeometryRetriever;

    private SegmentsReceivedEvent mLastSegmentsDisplayed;

    private LocationLocalDataSource locationLocalDataSource;

    private Map<String, Integer> polylineIdSequenceIdMap = new HashMap<>();

    private Polyline recordingPolyline;

    private Runnable mDisplayLocalSequencesRunnable = () ->
            locationLocalDataSource
                    .getLocations()
                    .toObservable()
                    .flatMap(Observable::fromIterable)
                    .groupBy(OSVLocation::getSequenceId)
                    .flatMapSingle(groupedLocation -> groupedLocation.collect(
                            // (Callable<Map<String, List<OSVLocation>>>)
                            () -> Collections.singletonMap(groupedLocation.getKey(), new ArrayList<OSVLocation>()),

                            // (BiConsumer<Map<String, List<OSVLocation>>)
                            (map, osvLocation) -> map.get(groupedLocation.getKey()).add(osvLocation)
                    ))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(
                            //onNext
                            locationsMap -> {
                                Log.d(TAG, "displayLocalSequencesRunnable. Status: success. Message: Sequences loaded successfully.");
                                if (locationsMap.isEmpty()) {
                                    Log.d(TAG, "displayLocalSequencesRunnable. Status: abort. Message: Empty sequences.");
                                    return;
                                }

                                for (Map.Entry<String, ArrayList<OSVLocation>> entry : locationsMap.entrySet()) {
                                    int localId = polylineIdSequenceIdMap.size();
                                    polylineIdSequenceIdMap.put(entry.getKey(), localId);
                                    Polyline polyline = new Polyline(localId);
                                    polyline.setNodes(Utils.toSKCoordinatesFromOSVLocations(entry.getValue()));
                                    polyline.isLocal = true;
                                    displayPolyline(polyline);
                                }
                            },
                            //onError
                            throwable -> Log.d(TAG, String.format("displayLocalSequencesRunnable. Status: error. Message: %s.", throwable.getLocalizedMessage())));

    private Runnable mDisplayTracksRunnable;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private LocationService locationService;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (OSVActivity) getActivity();
        appPrefs = ((OSVApplication) activity.getApplication()).getAppPrefs();
        Context context = getContext().getApplicationContext();
        this.locationLocalDataSource = Injection.provideLocationLocalDataSource(context);
        this.locationService = Injection.provideLocationService(context);
        mMapEnabled = appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED);
        mGeometryRetriever = new GeometryRetriever(activity);
        view = inflater.inflate(R.layout.fragment_map, null);
        recordButton = view.findViewById(R.id.record_button);
        recordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).goToRecordingScreen();
                }
            }
        });
        positionButton = view.findViewById(R.id.position_button);
        recordButton.hide();
        positionButton.hide();
        if (mMapEnabled) {
            final Runnable addMapRunnable = () -> {
                View map;
                map = inflater.inflate(R.layout.partial_map, null);
                FrameLayout holder = view.findViewById(R.id.frameLayout);
                holder.addView(map);
                mapViewGroup = map.findViewById(R.id.view_group_map);
                addMapListeners(MapFragment.this);
                mapViewGroup.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {

                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight,
                                               int oldBottom) {
                        if (v instanceof SKMapViewHolder) {
                            onViewChanged((SKMapViewHolder) v, right, bottom);
                        }
                    }
                });
                mapViewGroup.onResume();
            };
            if (appPrefs.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED) && !SKMaps.getInstance().isSKMapsInitialized()) {
                Log.d(TAG, "onCreateView: needs to initialize skmaps");
                Utils.initializeLibrary(activity, new SKMapsInitializationListener() {

                    @Override
                    public void onLibraryInitialized(boolean b) {
                        addMapRunnable.run();
                    }

                    @Override
                    public void onLibraryAlreadyInitialized() {

                    }
                });
            } else {
                addMapRunnable.run();
            }
            positionButton.setOnClickListener(v -> centerOnCurrentPosition());
            boundingBoxUS = new SKBoundingBox(new SKCoordinate(49.384358, -124.848974), new SKCoordinate(24.396308, -66.885444));
        } else {
            View map;
            map = inflater.inflate(R.layout.partial_map_placeholder, null);
            FrameLayout holder = view.findViewById(R.id.frameLayout);
            holder.addView(map);
            recordButton.show();
            positionButton.hide();
        }
        Log.d(TAG, "onCreateView: ");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart. Status: callback.");
        super.onStart();
        EventBus.register(this);
        if (mMapEnabled) {
            if (mapViewGroup != null) {
                mapViewGroup.onResume();
            }
            annotationViewList = new ArrayList<>();
            SKAnnotationView redAnnotationView = new SKAnnotationView();
            ImageView iv = new ImageView(activity);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            iv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_track_point_end));
            redAnnotationView.setView(iv);
            annotationViewList.add(iv);
            SKAnnotationView greenAnnotationView = new SKAnnotationView();
            ImageView iv2 = new ImageView(activity);
            iv2.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            iv2.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_track_point_start));
            greenAnnotationView.setView(iv2);
            annotationViewList.add(iv2);
            SKAnnotationView blueAnnotationView = new SKAnnotationView();
            ImageView iv3 = new ImageView(activity);
            iv3.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            iv3.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_track_point_position));
            blueAnnotationView.setView(iv3);
            annotationViewList.add(iv3);
            mSelectedPositionAnnotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_BLUE);
            mSelectedPositionAnnotation.setAnnotationView(blueAnnotationView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (activity.checkPermissionsForGPS()) {
            onLocationPermissionGranted();
        }
        if (mapViewGroup != null && mMapEnabled) {
            if (mapView == null) {
                view.postDelayed(() -> mapViewGroup.onResume(), 300);
            } else {
                mapViewGroup.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
    }

    @Override
    public void onStop() {
        if (mMapEnabled) {
            if (mapViewGroup != null) {
                mapViewGroup.onPause();
            }
            if (annotationViewList != null) {
                annotationViewList.clear();
                annotationViewList = null;
            }
        }
        EventBus.unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if (mGeometryRetriever != null) {
            mGeometryRetriever.destroy();
            mGeometryRetriever = null;
        }
        if (mapViewGroup != null) {
            addMapListeners(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    // map interaction callbacks ...
    @Override
    public void onActionPan() {
    }

    @Override
    public void onActionZoom() {
        if (mapView != null && mCurrentMode == MODE_RECORDING_SCREEN) {
            float zoom = mapView.getZoomLevel();
            appPrefs.saveFloatPreference(PreferenceTypes.K_RECORDING_MAP_ZOOM, zoom, true);
            Log.d(TAG, "onActionZoom: " + zoom);
        }
    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder mapHolder) {
        Log.d(TAG, "onSurfaceCreated: concurrency ");
        chessBackground = view.findViewById(R.id.chess_board_background);
        chessBackground.setVisibility(View.GONE);
        mapView = mapHolder.getMapSurfaceView();
        SKMapInternationalizationSettings mapIntSetting = new SKMapInternationalizationSettings();
        mapIntSetting.setPrimaryLanguage(SKLanguage.LANGUAGE_EN);
        mapIntSetting.setFallbackLanguage(SKLanguage.LANGUAGE_LOCAL);
        mapIntSetting
                .setFirstLabelOption(SKMapInternationalizationSettings.SKMapInternationalizationOption.MAP_INTERNATIONALIZATION_OPTION_INTL);
        mapIntSetting
                .setSecondLabelOption(SKMapInternationalizationSettings.SKMapInternationalizationOption.MAP_INTERNATIONALIZATION_OPTION_LOCAL);
        mapView.getMapSettings().setCityPoisShown(true);
        mapView.getMapSettings().setGeneratedPoisShown(false);
        mapView.getMapSettings().setImportantPoisShown(false);
        mapView.getMapSettings().setMapPoiIconsShown(false);
        mapView.getMapSettings().setHouseNumbersShown(false);
        mapHolder.setAttributionsLayout(getContext(), SKMapViewHolder.SKAttributionPosition.BOTTOM_LEFT);
        mapHolder.setInternationalisationListener(i -> {
            //empty since is a limitation from the sdk which requires this listener to be set in order to set map settings.
        });
        mapView.getMapSettings().setMapInternationalizationSettings(mapIntSetting);
        mapView.setZOrderMediaOverlay(true);
        if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MAP) {
            mapView.postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (!activity.checkPermissionsForGPS()) {
                        return;
                    }
                    if (!Utils.isGPSEnabled(getContext())) {
                        activity.resolveLocationProblem(false);
                    }
                    locationService.getLastKnownLocation()
                            .subscribe(new MaybeObserver<Location>() {
                                @Override
                                public void onSubscribe(Disposable d) {
                                    compositeDisposable.add(d);
                                }

                                @Override
                                public void onSuccess(Location location) {
                                    appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) location.getLatitude());
                                    appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) location.getLongitude());
                                    SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(location.getLatitude(), location.getLongitude()));
                                    mapView.setPositionAsCurrent(new SKCoordinate(location.getLatitude(), location.getLongitude()), 20, true);
                                    mapView.centerOnCurrentPosition(16, true, 1000);//zoomlevel, anim time
                                }

                                @Override
                                public void onError(Throwable e) {
                                    centerOnDefaultLocation();
                                }

                                @Override
                                public void onComplete() {
                                    centerOnDefaultLocation();
                                }

                                private void centerOnDefaultLocation() {
                                    final double lat = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT);
                                    final double lon = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON);
                                    if (lat != 0 && lon != 0) {
                                        Log.d(TAG, "run: lat lon = " + lat + ", " + lon);
                                        SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(lat, lon));
                                        mapView.setPositionAsCurrent(new SKCoordinate(lat, lon), 20, true);
                                        mapView.centerOnCurrentPosition(16, true, 1000);//zoomlevel, anim time
                                    } else if (lat == 0 && lon == 0) {
                                        mapView.setZoom(1);
                                        mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
                                    }
                                }
                            });
                    mapView.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            diplayLocalSequences();
                            if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MAP) {
                                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_HINT_TAP_ON_MAP, false)) {
                                    activity.showSnackBar(R.string.tip_map_screen, Snackbar.LENGTH_LONG, R.string.got_it_label, new Runnable() {

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
        }
        displayControls();
        mapHolder.showAttributions();
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
            Log.d(TAG, "onMapRegionChangeEnded: ");
            refreshDisplayedSequences();
        }
    }

    @Override
    public void onDoubleTap(SKScreenPoint point) {
        onMapInteraction();
    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {
        if (onMapInteraction()) {
            return;
        }
        if (mapView == null) {
            return;
        }

        if (activity.getCurrentScreen() == ScreenComposer.SCREEN_MAP) {
            activity.enableProgressBar(true);
            SKCoordinate tappedCoords = mapView.pointToCoordinate(skScreenPoint);
            mGeometryRetriever.nearby(new NetworkResponseDataListener<TrackCollection>() {

                @Override
                public void requestFailed(int status, TrackCollection details) {
                    Log.d(TAG, "nearby error: " + details);
                    activity.enableProgressBar(false);
                }

                @Override
                public void requestFinished(int status, TrackCollection collection) {
                    activity.enableProgressBar(false);
                    if (collection.getTrackList().size() > 0) {
                        activity.openScreen(ScreenComposer.SCREEN_NEARBY, collection);
                    } else {
                        activity.showSnackBar(getString(R.string.nearby_no_result_label), Snackbar.LENGTH_SHORT);
                    }
                }
            }, "" + tappedCoords.getLatitude(), "" + tappedCoords.getLongitude());
        }
    }

    @Override
    public void onRotateMap() {
    }

    @Override
    public void onLongPress(SKScreenPoint point) {
        onMapInteraction();
    }

    @Override
    public void onInternetConnectionNeeded() {
        if (activity != null) {
            activity.showSnackBar(R.string.map_no_internet_connection, Snackbar.LENGTH_LONG);
        }
    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {
    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {
        if (mCurrentMode == MODE_RECORDING_SCREEN) {
            if (mIsSmall) {
                EventBus.post(new PreviewSwitchEvent(false));
                return;
            }
            return;
        } else {
            if (mCurrentMode == MODE_IDLE && mapView != null) {
                Log.d(TAG, "onMapActionUp: ");
                refreshDisplayedSequences();
            }
        }
        Log.d(TAG, "onMapActionUp: reached end");
    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {
        onMapInteraction();
    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {
        onMapInteraction();
    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {
        onMapInteraction();
    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI customPoi) {
        onMapInteraction();
    }

    @Override
    public void onCurrentPositionSelected() {
        onMapInteraction();
    }

    @Override
    public void onObjectSelected(int i) {
        onMapInteraction();
    }

    @Override
    public void setSource(Object extra) {
        mPlayer = (PlaybackManager) extra;
        mPlayer.addPlaybackListener(this);
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
            mapView.post(() -> {
                if (mPlayer != null) {
                    displaySequence(mPlayer.getTrack(), mPlayer.getSequence().getType() == Sequence.SequenceTypes.LOCAL, getFrameStartIndex());
                }
            });
        }
    }

    @Override
    public void onProgressChanged(final int index) {
        if (mapView != null) {
            mapView.post(new Runnable() {

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
        mPlayer = null;
    }

    public void cancelAction() {
        if (mGeometryRetriever != null) {
            mGeometryRetriever.cancelNearby();
        }
    }

    @Override
    public void onLocationPermissionGranted() {
        Log.d(TAG, "observeOnLocationUpdates");
        compositeDisposable.add(locationService.getLocationUpdates()
                .filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES))
                .subscribe(this::onLocationChanged));
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        if (mMapEnabled) {
            if (mapView != null && mCurrentMode == MODE_IDLE) {
                mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
                SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(location.getLatitude(), location.getLongitude()));
            }
            if (noPositionYet) {
                noPositionYet = false;
                final double latitude = location.getLatitude();
                final double longitude = location.getLongitude();
                if (!appPrefs.getBooleanPreference(PreferenceTypes.K_RUN_COUNTER)) {
                    appPrefs.saveBooleanPreference(PreferenceTypes.K_RUN_COUNTER, true);
                    setMetrics(latitude, longitude);
                }
                appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LAT, (float) latitude);
                appPrefs.saveFloatPreference(PreferenceTypes.K_POS_LON, (float) longitude);
                setSignDetectionRegion(latitude, longitude);
                if (mapView != null) {
                    mapView.post(new Runnable() {

                        @Override
                        public void run() {
                            if (mapView != null && mCurrentMode == MODE_IDLE) {
                                if (latitude == 0 && longitude == 0) {
                                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
                                } else {
                                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
                                    mapView.centerOnCurrentPosition(16, true, 1000);
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onSdkEnabled(final SdkEnabledEvent event) {
        BackgroundThreadPool.post(() -> {
            mMapEnabled = event.enabled;
            final LayoutInflater inflater = activity.getLayoutInflater();
            if (mMapEnabled) {
                if (!SKMaps.getInstance().isSKMapsInitialized()) {
                    Utils.initializeLibrary(getActivity(), null);
                }
                boundingBoxUS = new SKBoundingBox(new SKCoordinate(49.384358, -124.848974), new SKCoordinate(24.396308, -66.885444));
                activity.runOnUiThread(() -> {
                    View map = inflater.inflate(R.layout.partial_map, null);
                    final FrameLayout holder = view.findViewById(R.id.frameLayout);
                    holder.removeAllViews();
                    holder.addView(map);
                    mapViewGroup = map.findViewById(R.id.view_group_map);
                    addMapListeners(MapFragment.this);
                    mapViewGroup.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        if (v instanceof SKMapViewHolder) {
                            onViewChanged((SKMapViewHolder) v, right, bottom);
                        }
                    });
                    enableMapButtons(true);
                    positionButton.setOnClickListener(v -> centerOnCurrentPosition());
                });
            } else {
                SKMaps.getInstance().destroySKMaps();
                activity.runOnUiThread(() -> {
                    View map = inflater.inflate(R.layout.partial_map_placeholder, null);
                    FrameLayout holder = view.findViewById(R.id.frameLayout);
                    holder.removeAllViews();
                    holder.addView(map);
                    recordButton.show();
                    positionButton.hide();
                    if (mapViewGroup != null) {
                        addMapListeners(null);
                    }
                });
            }
        });

        EventBus.clear(SdkEnabledEvent.class);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onBoundingBoxesChanged(BoundingBoxChangedEvent event) {
        if (mapView != null) {
            mapView.addPolyline(event.lastBB);
            mapView.addPolyline(event.requestedBB);
            mapView.addPolyline(event.smallBB);
        }
    }

    @Subscribe(sticky = true)
    public void onRefreshNeeded(SequencesChangedEvent event) {
        if (!event.online && mapView != null) {
            removePolyline(event.deletedSequenceId);
            EventBus.clear(SequencesChangedEvent.class);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onUploadFinished(UploadFinishedEvent event) {
        if (mapView != null) {
            mapView.clearAllOverlays();
            mLastSegmentsDisplayed = null;
            diplayLocalSequences();
            refreshDisplayedSequences();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onMatched(MatchedSegmentEvent event) {
        Log.d(TAG, "onMatched: " + (event.segment != null ? event.segment.getPolyline().getIdentifier() : "null"));
        if (Utils.DEBUG) {
            Polyline polyline = event.segment == null ? null : event.segment.getPolyline();
            if (mMatchedPolyline != null) {
                if (mapView != null) {
                    mMatchedPolyline.isLocal = false;
                    // set segment color
                    //                segment.setColor(new float[]{0.447f, 0.447f, 0.447f, 1.0f}); grey
                    mMatchedPolyline.setColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, 1f}); //washed out accent color
                    mMatchedPolyline.setOutlineColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, 1f}); //washed out accent color
                    mMatchedPolyline.setOutlineSize(3);
                    mMatchedPolyline.setOutlineDottedPixelsSolid(50000);
                    mMatchedPolyline.setOutlineDottedPixelsSkip(1);
                    mMatchedPolyline.setLineSize(3);
                    mapView.addPolyline(mMatchedPolyline);
                }
            }
            mMatchedPolyline = polyline;
            if (mapView != null && mMatchedPolyline != null) {
                mMatchedPolyline.isLocal = false;
                // set segment color
                //                segment.setColor(new float[]{0.447f, 0.447f, 0.447f, 1.0f}); grey
                mMatchedPolyline.setColor(new float[]{0f / 255f, 125f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                mMatchedPolyline
                        .setOutlineColor(new float[]{0f / 255f, 125f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                mMatchedPolyline.setOutlineSize(3);
                mMatchedPolyline.setOutlineDottedPixelsSolid(50000);
                mMatchedPolyline.setOutlineDottedPixelsSkip(1);
                mMatchedPolyline.setLineSize(5);
                mapView.addPolyline(mMatchedPolyline);
            }
            if (mapView != null && event.segment != null) {
                Polyline poly = new Polyline(MATCHED_SEGMENT_POLYLINE_ID);
                poly.getNodes().add(event.segment.getStart());
                poly.getNodes().add(event.segment.getEnd());
                poly.isLocal = false;
                // set segment color
                //                segment.setColor(new float[]{0.447f, 0.447f, 0.447f, 1.0f}); grey
                poly.setColor(new float[]{0f / 255f, 255f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                poly.setOutlineColor(new float[]{0f / 255f, 255f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
                poly.setOutlineSize(3);
                poly.setOutlineDottedPixelsSolid(50000);
                poly.setOutlineDottedPixelsSkip(1);
                poly.setLineSize(5);
                mapView.addPolyline(poly);
                SKCoordinate ref = event.segment.getReference();
                SKAnnotation annot = new SKAnnotation(MATCHED_SEGMENT_POLYLINE_ID + 1);
                annot.setLocation(ref);
                annot.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_RED);
                //                annot.setAnnotationView(blueAnnotationView);
                mapView.addAnnotation(annot, SKAnimationSettings.ANIMATION_NONE);
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onSegmentsReceived(final SegmentsReceivedEvent event) {
        if (mCurrentMode == MODE_TRACK_PREVIEW) {
            return;
        }
        if (mapView == null) {
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    EventBus.postSticky(event);
                }
            }, 500);
            return;
        }
        Log.d(TAG, "onSegmentsReceived: from " + (event.matcher ? "matcher" : "map"));
        if (event.polylines != null) {
            ListIterator litr = event.polylines.listIterator();

            while (litr.hasNext()) {
                Polyline polyline = (Polyline) litr.next();
                displayPolyline(polyline);
            }
            mLastSegmentsDisplayed = event;
        }
    }

    /**
     * Enables/disables the map buttons, i.e. record and position.
     * <p> The lint suppress is due to restriction of the api. While the recommendation is to use hide/show methods, that means those methods will use internally animators sets
     * which have issues when called on fragments which do not have callbacks of lifecycle when the view is changed/resized.
     * @param enable {@code true} will display the buttons, otherwise hide them.
     */
    @SuppressLint("RestrictedApi")
    public void enableMapButtons(boolean enable) {
        if (recordButton != null && positionButton != null) {
            if (enable) {
                recordButton.setVisibility(View.VISIBLE);
                positionButton.setVisibility(View.VISIBLE);
            } else {
                recordButton.setVisibility(View.GONE);
                positionButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Enables/disables the recording button.
     * @param enable {@code true} if the button should be visible, {@code false} otherwise.
     */
    public void enableRecordingButton(boolean enable) {
        if (recordButton == null) {
            return;
        }
        if (enable) {
            recordButton.show();
        } else {
            recordButton.hide();
        }
    }

    /**
     * Center the map on current position if GPS is enable,
     * otherwise request to the user to turn on the GPS.
     */
    private void centerOnCurrentPosition() {
        if (!Utils.isGPSEnabled(getContext())) {
            activity.resolveLocationProblem(false);
        } else if (mapView != null) {
            mapView.centerOnCurrentPosition(16, true, 1000);
        }
    }

    private void addPolyline(String sequenceId) {
        if (polylineIdSequenceIdMap != null && !polylineIdSequenceIdMap.containsKey(sequenceId)) {
            int polyLineId = polylineIdSequenceIdMap.size();
            polylineIdSequenceIdMap.put(sequenceId, polyLineId);
            mapView.addPolyline(getRecordingPolyline(polyLineId));
        }
    }

    private Polyline getRecordingPolyline(int identifier) {
        Polyline recordingPolyline = new Polyline(identifier);
        recordingPolyline.setLineSize(0);
        recordingPolyline.setOutlineSize((int) Utils.dpToPx(activity, 4));
        recordingPolyline.setOutlineDottedPixelsSkip((int) Utils.dpToPx(activity, 20));
        recordingPolyline.setOutlineDottedPixelsSolid((int) Utils.dpToPx(activity, 20));
        recordingPolyline.setColor(new float[]{0f / 255f, 122f / 255f, 255f / 255f, 1.0f});
        recordingPolyline.setOutlineColor(new float[]{0f / 255f, 122f / 255f, 255f / 255f, 1.0f});

        this.recordingPolyline = recordingPolyline;
        return this.recordingPolyline;
    }

    private void removePolyline(String sequenceId) {
        if (polylineIdSequenceIdMap != null && polylineIdSequenceIdMap.containsKey(sequenceId)) {
            mapView.clearOverlay(polylineIdSequenceIdMap.get(sequenceId));
            polylineIdSequenceIdMap.remove(sequenceId);
        }
    }

    /**
     * TODo: remove mPlayer sequence direct access.
     * @return {@code int} which represents the frame index in case the compression base in jpeg otherwise it will be 0.
     */
    private int getFrameStartIndex() {
        SequenceDetailsCompressionBase compressionBase = mPlayer.getSequence().getCompressionDetails();
        return compressionBase instanceof SequenceDetailsCompressionJpeg ? ((SequenceDetailsCompressionJpeg) compressionBase).getFrameIndex() : 0;
    }

    private void onViewChanged(SKMapViewHolder mapViewGroup, int width, int height) {
        int orientation = activity.getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        Point point = new Point();
        DimenUtils.getContentSize(activity, portrait, point);
        Log.d(TAG, "onViewChanged: map is " + width + "x" + height + ", while screen is " + point.x + "x" + point.y);
        if (width == 0 && height == 0) {
            return;
        }
        int screen = activity.getCurrentScreen();
        boolean isSmall = (width < point.x / 2) && (height < point.y / 2) &&
                (screen == ScreenComposer.SCREEN_RECORDING);
        boolean maximized = (width >= point.x / 10 * 9) && (height >= point.y / 10 * 9);
        if (maximized) {
            enterMode(MODE_IDLE, mapViewGroup);
        } else if (screen == ScreenComposer.SCREEN_RECORDING) {
            enterMode(MODE_RECORDING_SCREEN, mapViewGroup);
        } else {
            enterMode(MODE_TRACK_PREVIEW, mapViewGroup);
        }
        mIsSmall = isSmall;
        Log.d(TAG, "setPreviewSurface: mIsSmall = " + mIsSmall);
        if (mapView != null && recordingPolyline != null) {
            mapView.addPolyline(recordingPolyline);
        }
    }

    private void handlePreviewMapMode() {
        if (mapView != null) {
            applySettingsOnMapView(true);
            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
        }
        if (mPlayer != null) {
            displaySequence(mPlayer.getTrack(), mPlayer.getSequence().getType() == Sequence.SequenceTypes.LOCAL, getFrameStartIndex());
        }
    }

    private void handleRecordingMapMode() {
        BackgroundThreadPool.cancelTask(mDisplayLocalSequencesRunnable);
        BackgroundThreadPool.cancelTask(mDisplayTracksRunnable);
        if (mapView != null) {
            mapView.setZoom(appPrefs.getFloatPreference(PreferenceTypes.K_RECORDING_MAP_ZOOM, 16));
            applySettingsOnMapView(false);
            mapView.setZOrderMediaOverlay(true);
            mapView.post(new Runnable() {

                @Override
                public void run() {
                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_ARROW_SMALL);
                }
            });
        }
    }

    private void handleIdleMapMode(SKMapViewHolder mapViewGroup) {
        if (mapViewGroup != null) {
            if (mapView == null) {
                mapView = mapViewGroup.getMapSurfaceView();
            }
        }

        applySettingsOnMapView(true);
        final double lat = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LAT);
        final double lon = (double) appPrefs.getFloatPreference(PreferenceTypes.K_POS_LON);
        if (lat == 0 && lon == 0) {
            if (mapView != null) {
                mapView.setZoom(1);
                mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
            }
        } else {
            if (noPositionYet) {
                SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(lat, lon));
            }
        }
        if (mapView != null) {
            mapView.clearOverlay(TRACK_POLYLINE_ID);
            mapView.post(() -> {
                if (lat == 0 && lon == 0) {
                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
                } else {
                    mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
                }
            });
        }
    }

    /**
     * reads and displayes all locally cached sequences from the files system
     */
    private void diplayLocalSequences() {
        if (mMapEnabled) {
            BackgroundThreadPool.post(mDisplayLocalSequencesRunnable);
        }
    }

    /**
     * displays local and online sequences
     */
    private void refreshDisplayedSequences() {
        if (activity == null || !mMapEnabled) {
            return;
        }
        final float zoomForRequest = mapView.getMapSettings().isMapZoomingEnabled() ? mapView.getZoomLevel() : 19;
        final SKCoordinateRegion skCoordinateRegion = mapView.getCurrentMapRegion();
        SKBoundingBox bbnormal = mapView.getBoundingBoxForRegion(skCoordinateRegion);
        SKBoundingBox lastBoundingBox = mLastSegmentsDisplayed == null ? null : mLastSegmentsDisplayed.boundingBox;
        if (assessBBDifference(lastBoundingBox, bbnormal) ||
                (mLastSegmentsDisplayed != null && zoomForRequest - mLastSegmentsDisplayed.zoom > 1)) {
            final float zoom = skCoordinateRegion.getZoomLevel();
            SKCoordinate coord = skCoordinateRegion.getCenter();
            if (zoom < 5) {
                mapView.clearAllOverlays();
                mLastSegmentsDisplayed = null;
                return;
            }
            Log.d(TAG, "refreshDisplayedSequences: loading tracks for zoom level " + zoom + " , coordinate " + coord);
            skCoordinateRegion.setZoomLevel(zoom - 2);
            final SKBoundingBox boundingBox = mapView.getBoundingBoxForRegion(skCoordinateRegion);
            //intermediary bb
            skCoordinateRegion.setZoomLevel(zoom - 1f);
            BackgroundThreadPool.cancelTask(mDisplayLocalSequencesRunnable);
            BackgroundThreadPool.cancelTask(mDisplayTracksRunnable);
            mDisplayTracksRunnable = new Runnable() {

                @Override
                public void run() {
                    if (mapView != null) {
                        Log.d(TAG, "refreshDisplayedSequences: sending request");
                        mGeometryRetriever.listSegments(new NetworkResponseDataListener<GeometryCollection>() {

                                                            @Override
                                                            public void requestFailed(int status, GeometryCollection details) {
                                                                Log.d(TAG, "requestFinished: " + details);
                                                            }

                                                            @Override
                                                            public void requestFinished(int status, GeometryCollection collectionData) {
                                                                if (mCurrentMode == MODE_IDLE) {
                                                                    EventBus.postSticky(
                                                                            new SegmentsReceivedEvent(collectionData.getSegmentList(), new Object(), false,
                                                                                    boundingBox, zoomForRequest));
                                                                }
                                                            }
                                                        }, boundingBox.getTopLeft().getLatitude() + "," + boundingBox.getTopLeft().getLongitude(),
                                boundingBox.getBottomRight().getLatitude() + "," + boundingBox.getBottomRight().getLongitude(),
                                zoomForRequest);
                    }
                }
            };
            BackgroundThreadPool.post(mDisplayTracksRunnable);
        } else {
            Log.d(TAG, "refreshDisplayedSequences: not changed enough");
        }
    }

    /**
     * Customize the map view
     */
    private void applySettingsOnMapView(boolean interactive) {
        if (mapView != null) {
            mapView.getMapSettings().setMapRotationEnabled(interactive);
            mapView.getMapSettings().setMapZoomingEnabled(true);
            mapView.getMapSettings().setMapPanningEnabled(interactive);
            mapView.getMapSettings().setZoomLimits(interactive ? 1 : 14f, 19f);
            mapView.getMapSettings().setZoomWithAnchorEnabled(!interactive);
            mapView.getMapSettings().setInertiaRotatingEnabled(interactive);
            mapView.getMapSettings().setInertiaZoomingEnabled(interactive);
            mapView.getMapSettings().setInertiaPanningEnabled(interactive);
            mapView.getMapSettings().setFollowPositions(!interactive);
            setHeading(!interactive);
            updateMapStyle();
        }
    }

    /**
     * Add the map listeners for the current fragment.
     * @param mapFragment the instance to either the map fragment or null for cleanup.
     */
    private void addMapListeners(@Nullable MapFragment mapFragment) {
        mapViewGroup.setMapSurfaceCreatedListener(mapFragment);
        mapViewGroup.setPoiListener(mapFragment);
        mapViewGroup.setPanListener(mapFragment);
        mapViewGroup.setInternetConnectionListener(mapFragment);
        mapViewGroup.setMapActionListener(mapFragment);
        mapViewGroup.setMapTapListener(mapFragment);
        mapViewGroup.setMapAnnotationListener(mapFragment);
        mapViewGroup.setZoomListener(mapFragment);
        mapViewGroup.setCurrentPositionSelectedListener(mapFragment);
        mapViewGroup.setObjectSelectedListener(mapFragment);
        mapViewGroup.setMapRegionChangedListener(mapFragment);
    }


    /**
     * updates the current map style to the new style given as parameter
     */
    private void updateMapStyle() {
        SKMapViewStyle style = new SKMapViewStyle(SplashActivity.mapResourcesDirPath + "grayscalestyle" + "/", "grayscalestyle.json");
        SKMapSettings skMapSettings = mapView.getMapSettings();
        String currentMapStyle = skMapSettings.getMapStyle().getStyleFileName();
        String nextMapStyle = style.getStyleFileName();

        if (!currentMapStyle.equals(nextMapStyle)) {
            skMapSettings.setMapStyle(style);
        }
    }

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
        return bb.getTopLeft().getLatitude() >= lat && lat >= bb.getBottomRight().getLatitude() && bb.getTopLeft().getLongitude() <= lon &&
                lon <= bb.getBottomRight().getLongitude();
    }

    private boolean onMapInteraction() {
        cancelAction();
        if (mCurrentMode == MODE_RECORDING_SCREEN) {
            if (mIsSmall) {
                EventBus.post(new PreviewSwitchEvent(false));
                return true;
            }
            return true;
        }
        return !mMapEnabled;
    }

    /**
     * Enables/disables heading mode
     * @param enabled for enabled
     */
    private void setHeading(boolean enabled) {
        if (enabled) {
            mapView.getMapSettings().setFollowPositions(true);
            mapView.getMapSettings().setHeadingMode(SKMapSettings.SKHeadingMode.ROUTE);
            mapView.centerOnCurrentPosition(appPrefs.getFloatPreference(PreferenceTypes.K_RECORDING_MAP_ZOOM, 16), true, 1000);
        } else {
            mapView.getMapSettings().setFollowPositions(false);
            mapView.getMapSettings().setHeadingMode(SKMapSettings.SKHeadingMode.NONE);
        }
    }

    private void setMetrics(double latitude, double longitude) {
        if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeft().getLatitude(), boundingBoxUS.getTopLeft().getLongitude(),
                boundingBoxUS.getBottomRight().getLatitude(), boundingBoxUS.getBottomRight().getLongitude())) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, false);
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, true);
        }
    }

    private void setSignDetectionRegion(double latitude, double longitude) {
        //TODOdo reinitialize sign detection
        if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeft().getLatitude(), boundingBoxUS.getTopLeft().getLongitude(),
                boundingBoxUS.getBottomRight().getLatitude(), boundingBoxUS.getBottomRight().getLongitude())) {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_REGION_US, true);
        } else {
            appPrefs.saveBooleanPreference(PreferenceTypes.K_REGION_US, false);
        }
    }

    /**
     * displays a specific sequence, other sequences get cleared
     * @param nodes the coordinates and the images
     */
    @SuppressWarnings("unchecked")
    private void displaySequence(final ArrayList<SKCoordinate> nodes, boolean isLocal, int startIndex) {
        Log.d(TAG, "displaySequence: ");
        // set the nodes on the polyline
        if (nodes != null && nodes.size() != 0 && mapView != null && mPlayer != null) {
            mapView.clearAllOverlays();
            mLastSegmentsDisplayed = null;
            mapView.deleteAllAnnotationsAndCustomPOIs();
            mPreviewNodes = (ArrayList<ImageCoordinate>) nodes.clone();
            final ArrayList<SKCoordinate> coords = new ArrayList<>(nodes);
            int id = polylineIdSequenceIdMap.size();
            polylineIdSequenceIdMap.put(mPlayer.getSequence().getID(), id);
            Polyline polyline = new Polyline(id);
            polyline.isLocal = isLocal;
            polyline.coverage = 15;
            polyline.setNodes(coords);
            displayPolyline(polyline);
            zoomToPolyline(coords, (int) Utils.dpToPx(activity, 15), (int) Utils.dpToPx(activity, 15));
            mapView.deleteAnnotation(VIA_POINT_ICON_ID);
            mSelectedPositionAnnotation.setLocation(nodes.get(Math.min(startIndex, nodes.size() - 1)));
            mapView.addAnnotation(mSelectedPositionAnnotation, SKAnimationSettings.ANIMATION_PULSE_CCP);
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
            if (northLat == 0) {
                northLat = lat;
            }
            if (westLon == 0) {
                westLon = lon;
            }
            if (southLat == 0) {
                southLat = lat;
            }
            if (eastLon == 0) {
                eastLon = lon;
            }
            westLon = Math.min(lon, westLon);
            southLat = Math.min(lat, southLat);
            northLat = Math.max(northLat, lat);
            eastLon = Math.max(eastLon, lon);
        }

        SKBoundingBox boundingBox = new SKBoundingBox(new SKCoordinate(northLat, westLon), new SKCoordinate(southLat, eastLon));
        mapView.fitBoundingBox(boundingBox, paddingX, paddingY, paddingY / 2, paddingY / 2);
    }

    /**
     * displayes a polyline for a list of coords
     */
    private void displayPolyline(Polyline polyline) {
        if (mapView != null && polyline != null && polyline.getNodes().size() > 0) {
            //            Log.d(TAG, "displayPolyline: " + polyline.getIdentifier() + ", size " + polyline.getNodes().size());
            // set polyline color
            int outlineSize = 3;
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
                    //                    outlineSize = new Random().nextInt(5);
                    polyline.setColor(color);
                    polyline.setOutlineColor(color);
                } else {
                    polyline
                            .setColor(new float[]{189f / 255f, 16f / 255f, 224f / 255f, Math.min(polyline.coverage, 10) * 0.09f + 0.1f}); //accent color
                    polyline.setOutlineColor(
                            new float[]{189f / 255f, 16f / 255f, 224f / 255f, Math.min(polyline.coverage, 10) * 0.09f + 0.1f}); //accent color
                }
            }
            polyline.setOutlineSize(outlineSize);
            polyline.setOutlineDottedPixelsSolid(50000);
            polyline.setOutlineDottedPixelsSkip(1);
            polyline.setLineSize(outlineSize);
            mapView.addPolyline(polyline);
        }
    }

    /**
     * removes the currently viewed sequence and displayes all sequences
     */
    private void removeSequence() {
        if (mPreviewNodes != null) {
            mPreviewNodes.clear();
        }
        if (mapView != null && mPlayer != null) {
            Sequence sequence = mPlayer.getSequence();
            if (sequence != null &&
                    //Sometimes the sequence is still downloading, therefore, the data is not available in the polyline map.
                    //For this particular case, we need to check first if the data is available or not,
                    //in order to perform the following operations.
                    polylineIdSequenceIdMap.get(sequence.getID()) != null) {
                int polyLineId = polylineIdSequenceIdMap.get(sequence.getID());
                mapView.clearOverlay(polyLineId);
                polylineIdSequenceIdMap.remove(sequence.getID());
            }
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
            Log.w(TAG, "viewFrame: sorry returning");
            return;
        }
        mSelectedPositionAnnotation.setLocation(mPreviewNodes.get(index));
        mapView.updateAnnotation(mSelectedPositionAnnotation);
    }

    private void enterMode(int mode, SKMapViewHolder mapViewGroup) {
        Log.d(TAG, "enterMode: " + mode);
        mCurrentMode = mode;
        switch (mode) {
            case MODE_IDLE:
                enableMapButtons(true);
                removeSequence();
                handleIdleMapMode(mapViewGroup);
                Log.d(TAG, "enterMode. Status: idle. Message: Processing idle map mode.");
                break;
            case MODE_RECORDING_SCREEN:
                enableMapButtons(false);
                if (mLastSegmentsDisplayed == null || !mLastSegmentsDisplayed.matcher) {
                    EventBus.post(new BroadcastSegmentsCommand());
                }
                handleRecordingMapMode();
                Log.d(TAG, "enterMode. Status: recording. Message: Processing recording map mode.");
                break;
            case MODE_TRACK_PREVIEW:
                enableMapButtons(false);
                if (mPlayer != null) {
                    handlePreviewMapMode();
                } else {
                    Log.d(TAG, "setup: player is null");
                }
                Log.d(TAG, "enterMode. Status: preview. Message: Processing preview map mode.");
                break;
        }
    }

    private void displayControls() {
        if (mapViewGroup != null) {
            mapViewGroup.setVisibility(View.VISIBLE);
        }
        if (chessBackground != null) {
            chessBackground.setVisibility(View.GONE);
        }
        switch (mCurrentMode) {
            case MODE_IDLE:
                enableMapButtons(true);
                break;
            case MODE_RECORDING_SCREEN:
            case MODE_TRACK_PREVIEW:
                enableMapButtons(false);
                break;
        }
    }
}
