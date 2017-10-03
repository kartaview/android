package com.telenav.osv.ui.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKAnnotationView;
import com.skobbler.ngx.map.SKBoundingBox;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapInternationalizationSettings;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.activity.SplashActivity;
import com.telenav.osv.command.BroadcastSegmentsCommand;
import com.telenav.osv.command.PhotoCommand;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.camera.RecordingEvent;
import com.telenav.osv.event.hardware.gps.LocationEvent;
import com.telenav.osv.event.hardware.gps.TrackChangedEvent;
import com.telenav.osv.event.network.matcher.BoundingBoxChangedEvent;
import com.telenav.osv.event.network.matcher.MatchedSegmentEvent;
import com.telenav.osv.event.network.matcher.SegmentsReceivedEvent;
import com.telenav.osv.event.network.upload.UploadFinishedEvent;
import com.telenav.osv.event.ui.PositionerEvent;
import com.telenav.osv.event.ui.PreviewSwitchEvent;
import com.telenav.osv.event.ui.SequencesChangedEvent;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.LocalSequence;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Sequence;
import com.telenav.osv.item.network.GeometryCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.GeometryRetriever;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.ui.Navigator;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.DimenUtils;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import java.util.ArrayList;
import javax.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Fragment holding the map or the placeholder
 * Created by Kalman on 11/9/15.
 */
public class MapFragment extends OSVFragment implements Displayable<Sequence>, SKMapSurfaceListener, PlaybackManager.PlaybackListener {

  public static final String TAG = "MapFragment";

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
   * the app prefs
   */
  @Inject
  Preferences appPrefs;

  @Inject
  GeometryRetriever mGeometryRetriever;

  private Sequence mCurrentSequence;

  /**
   * fragment's view
   */
  private View view;

  private boolean mRecording = false;

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
   * the list of images with coordinates used for previewing a sequence
   */
  private ArrayList<ImageCoordinate> mPreviewNodes;

  private OSVActivity activity;

  private ArrayList<ImageView> annotationViewList;

  private SKAnnotation mSelectedPositionAnnotation = new SKAnnotation(VIA_POINT_ICON_ID);

  private Polyline mTrackPolyline = new Polyline(TRACK_POLYLINE_ID);

  private FloatingActionButton recordButton;

  private FloatingActionButton positionButton;

  private SKBoundingBox boundingBoxUS;

  private boolean noPositionYet = true;

  private int mCurrentMode = MODE_IDLE;

  private Polyline mMatchedPolyline;

  private boolean mMapEnabled;

  private View chessBackground;

  private SegmentsReceivedEvent mLastSegmentsDisplayed;

  private Runnable mDisplayLocalSequencesRunnable = () -> {
    synchronized (LocalSequence.getStaticSequences()) {
      for (LocalSequence sequence : LocalSequence.getStaticSequences().values()) {
        if (sequence.getOriginalFrameCount() > 0) {
          try {
            displayPolyline(sequence.getPolyline());
          } catch (NumberFormatException e) {
            Log.d(TAG, "diplayLocalSequences: " + e.getLocalizedMessage());
          }
        }
      }
    }
  };

  private Runnable mDisplayTracksRunnable;

  private Handler mHandler = new Handler(Looper.getMainLooper());

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    activity = (OSVActivity) getActivity();
    mMapEnabled = appPrefs.isMapEnabled();
    view = inflater.inflate(R.layout.fragment_map, null);
    recordButton = view.findViewById(R.id.record_button);
    recordButton.setOnClickListener(v -> {
      int cameraPermitted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
      if (cameraPermitted == PackageManager.PERMISSION_GRANTED) {
        if (appPrefs.isMapEnabled() &&
            appPrefs.isMiniMapEnabled()) {
          recordButton.setVisibility(View.INVISIBLE);
          positionButton.setVisibility(View.INVISIBLE);
        }
      }
      activity.openScreen(Navigator.SCREEN_RECORDING);
    });
    positionButton = view.findViewById(R.id.position_button);
    recordButton.setVisibility(View.INVISIBLE);
    positionButton.setVisibility(View.INVISIBLE);
    if (mMapEnabled) {
      final Runnable addMapRunnable = () -> {
        View map;
        map = inflater.inflate(R.layout.partial_map, null);
        FrameLayout holder = view.findViewById(R.id.frameLayout);
        holder.addView(map);
        mapViewGroup = map.findViewById(R.id.view_group_map);
        mapViewGroup.setMapSurfaceListener(MapFragment.this);
        mapViewGroup.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
          if (v instanceof SKMapViewHolder) {
            onViewChanged((SKMapViewHolder) v, right, bottom);
          }
        });
        mapViewGroup.onResume();
      };
      if (appPrefs.isMapEnabled() && !SKMaps.getInstance().isSKMapsInitialized()) {
        Log.d(TAG, "onCreateView: needs to initialize skmaps");
        Utils.initializeLibrary(activity, appPrefs, b -> addMapRunnable.run());
      } else {
        addMapRunnable.run();
      }
      positionButton.setOnClickListener(v -> onPositionerClicked(null));
      boundingBoxUS = new SKBoundingBox(new SKCoordinate(49.384358, -124.848974), new SKCoordinate(24.396308, -66.885444));
    } else {
      View map;
      map = inflater.inflate(R.layout.partial_map_placeholder, null);
      FrameLayout holder = view.findViewById(R.id.frameLayout);
      holder.addView(map);
      recordButton.setVisibility(View.VISIBLE);
      positionButton.setVisibility(View.INVISIBLE);
    }
    Log.d(TAG, "onCreateView: ");
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    EventBus.register(this);
    if (mMapEnabled) {
      if (mapViewGroup != null) {
        mapViewGroup.onResume();
      }
      mTrackPolyline = new Polyline(10000156);
      mTrackPolyline.setLineSize(0);
      mTrackPolyline.setOutlineSize((int) Utils.dpToPx(activity, 4));
      mTrackPolyline.setOutlineDottedPixelsSkip((int) Utils.dpToPx(activity, 20));
      mTrackPolyline.setOutlineDottedPixelsSolid((int) Utils.dpToPx(activity, 20));
      mTrackPolyline.setColor(new float[] {0f / 255f, 122f / 255f, 255f / 255f, 1.0f});
      mTrackPolyline.setOutlineColor(new float[] {0f / 255f, 122f / 255f, 255f / 255f, 1.0f});
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
      mapViewGroup.setMapSurfaceListener(null);
    }
    super.onDestroyView();
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
        (screen == Navigator.SCREEN_RECORDING || screen == Navigator.SCREEN_RECORDING_HINTS);
    boolean maximized = (width >= point.x / 10 * 9) && (height >= point.y / 10 * 9);
    if (maximized) {
      enterMode(MODE_IDLE);
    } else if (screen == Navigator.SCREEN_RECORDING || screen == Navigator.SCREEN_RECORDING_HINTS) {
      enterMode(MODE_RECORDING_SCREEN);
    } else {
      enterMode(MODE_TRACK_PREVIEW);
    }
    mIsSmall = isSmall;
    Log.d(TAG, "onSurfaceChanged: mIsSmall = " + mIsSmall);
    setup(mapViewGroup);
  }

  @Subscribe
  public void onPositionerClicked(@Nullable PositionerEvent event) {
    if (mapView != null) {
      if (activity.checkPermissionsForGPS()) {
        if (!Utils.isGPSEnabled(activity)) {
          activity.resolveLocationProblem(false);
        } else {
          //EventBus.postSticky(new GpsCommand(true, true/* request single position*/)); todo for power optimisation
          if (activity.hasPosition()) {
            mapView.centerOnCurrentPosition(16, true, 1000);//zoomlevel, anim time
          } else {
            activity.showSnackBar(getString(R.string.warning_waiting_for_location_message), Snackbar.LENGTH_SHORT);
          }
        }
      }
    }
  }

  private void setup(SKMapViewHolder mapViewGroup) {
    if (mCurrentMode == MODE_IDLE) {
      if (mapViewGroup != null) {
        mapViewGroup.showScoutTextView();
        if (mapView == null) {
          mapView = mapViewGroup.getMapSurfaceView();
        }
      }
      if (recordButton != null && positionButton != null) {
        recordButton.setVisibility(View.VISIBLE);
        positionButton.setVisibility(View.VISIBLE);
      }
      applySettingsOnMapView(true);
      Location loc = appPrefs.getLastLocation();
      if (loc != null) {
        if (noPositionYet) {
          SKPositionerManager.getInstance().reportNewGPSPosition(new SKPosition(loc));
        }
      } else {
        if (mapView != null) {
          mapView.setZoom(1);
          mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
        }
      }
      if (mapView != null) {
        mapView.clearOverlay(TRACK_POLYLINE_ID);
        mapView.post(() -> {
          if (loc == null) {
            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
          } else {
            mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
          }
        });
      }
    } else {
      if (recordButton != null && positionButton != null) {
        recordButton.setVisibility(View.INVISIBLE);
        positionButton.setVisibility(View.INVISIBLE);
      }
      if (this.mapViewGroup != null) {
        this.mapViewGroup.hideScoutTextView();
      }
      if (mCurrentMode == MODE_RECORDING_SCREEN) {
        BackgroundThreadPool.cancelTask(mDisplayLocalSequencesRunnable);
        BackgroundThreadPool.cancelTask(mDisplayTracksRunnable);
        if (mapView != null) {
          mapView.setZoom(appPrefs.getRecordingMapZoom());
          applySettingsOnMapView(false);
          mapView.setZOrderMediaOverlay(true);
          mapView.post(() -> mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_ARROW_SMALL));
        }
      } else if (mCurrentMode == MODE_TRACK_PREVIEW && mCurrentSequence != null) {
        if (mapView != null) {
          applySettingsOnMapView(true);
          mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
        }
        displaySequence((ArrayList<SKCoordinate>) mCurrentSequence.getPolyline().getNodes(), !mCurrentSequence.isSafe(),
                        mCurrentSequence.getRequestedFrameIndex());
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
      mDisplayTracksRunnable = () -> {
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

  // map interaction callbacks ...
  @Override
  public void onActionPan() {
    //nothing
  }

  @Override
  public void onActionZoom() {
    if (mapView != null && mCurrentMode == MODE_RECORDING_SCREEN) {
      float zoom = mapView.getZoomLevel();
      appPrefs.postRecordingMapZoom(zoom);
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
    mapIntSetting.setPrimaryLanguage(SKMaps.SKLanguage.LANGUAGE_EN);
    mapIntSetting.setFallbackLanguage(SKMaps.SKLanguage.LANGUAGE_LOCAL);
    mapIntSetting
        .setFirstLabelOption(SKMapInternationalizationSettings.SKMapInternationalizationOption.MAP_INTERNATIONALIZATION_OPTION_INTL);
    mapIntSetting
        .setSecondLabelOption(SKMapInternationalizationSettings.SKMapInternationalizationOption.MAP_INTERNATIONALIZATION_OPTION_LOCAL);
    mapView.getMapSettings().setMapInternationalizationSettings(mapIntSetting);
    mapView.getMapSettings().setCityPoisShown(true);
    mapView.getMapSettings().setGeneratedPoisShown(false);
    mapView.getMapSettings().setImportantPoisShown(false);
    mapView.getMapSettings().setMapPoiIconsShown(false);
    mapView.getMapSettings().setHouseNumbersShown(false);
    mapView.setZOrderMediaOverlay(true);
    if (activity.getCurrentScreen() == Navigator.SCREEN_MAP) {
      mapView.postDelayed(() -> {
        Location loc = appPrefs.getLastLocation();
        if (loc != null) {
          SKPosition skpos = new SKPosition(loc);
          Log.d(TAG, "run: location = " + loc);
          SKPositionerManager.getInstance().reportNewGPSPosition(skpos);
          mapView.setPositionAsCurrent(new SKCoordinate(loc.getLatitude(), loc.getLongitude()), 20, true);
          mapView.centerOnCurrentPosition(16, true, 1000);//zoomlevel, anim time
        } else {
          mapView.setZoom(1);
          mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
        }
        mapView.postDelayed(() -> {
          diplayLocalSequences();
          if (activity.getCurrentScreen() == Navigator.SCREEN_MAP) {
            if (appPrefs.shouldShowTapOnMap()) {
              activity.showSnackBar(R.string.tip_map_screen, Snackbar.LENGTH_LONG,
                                    R.string.got_it_label, () -> appPrefs.setShouldShowTapOnMap(false));
            }
          }
        }, 1040);
      }, 1000);
    }
    resizeStopped();
  }

  @Override
  public void onSurfaceChanged(SKMapViewHolder mapViewHolder, int width, int height) {
    onViewChanged(mapViewHolder, width, height);
  }

  public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {
    //nothing
  }

  public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {
    //nothing
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

    if (activity.getCurrentScreen() == Navigator.SCREEN_MAP) {
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
            activity.openScreen(Navigator.SCREEN_NEARBY, collection);
          } else {
            activity.showSnackBar(getString(R.string.nearby_no_result_label), Snackbar.LENGTH_SHORT);
          }
        }
      }, String.valueOf(tappedCoords.getLatitude()), String.valueOf(tappedCoords.getLongitude()));
    }
  }

  public void onRotateMap() {
    //nothing
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

  public void onMapActionDown(SKScreenPoint skScreenPoint) {
    //nothing
  }

  @Override
  public void onMapActionUp(SKScreenPoint skScreenPoint) {
    if (mCurrentMode == MODE_RECORDING_SCREEN) {
      if (mIsSmall) {
        EventBus.post(new PreviewSwitchEvent(false));
        return;
      } else if (mRecording) {
        EventBus.post(new PhotoCommand());
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

  public void onCompassSelected() {
    //nothing
  }

  @Override
  public void onCurrentPositionSelected() {
    onMapInteraction();
  }

  @Override
  public void onObjectSelected(int i) {
    onMapInteraction();
  }

  public void onInternationalisationCalled(int i) {
    //nothing
  }

  public void onDebugInfo(double v, float v1, double v2) {
    //nothing
  }

  public void onBoundingBoxImageRendered(int i) {
    //nothing
  }

  public void onGLInitializationError(String s) {
    //nothing
  }

  public void onScreenshotReady(Bitmap bitmap) {
    //nothing
  }

  private boolean onMapInteraction() {
    cancelAction();
    if (mCurrentMode == MODE_RECORDING_SCREEN) {
      if (mIsSmall) {
        EventBus.post(new PreviewSwitchEvent(false));
        return true;
      } else if (mRecording) {
        EventBus.post(new PhotoCommand());
        return true;
      }
      return true;
    }
    return !mMapEnabled;
  }

  /**
   * Enables/disables heading mode
   *
   * @param enabled for enabled
   */
  private void setHeading(boolean enabled) {
    if (enabled) {
      mapView.getMapSettings().setFollowPositions(true);
      mapView.getMapSettings().setHeadingMode(SKMapSettings.SKHeadingMode.ROUTE);
      mapView.centerOnCurrentPosition(appPrefs.getRecordingMapZoom(), true, 1000);
    } else {
      mapView.getMapSettings().setFollowPositions(false);
      mapView.getMapSettings().setHeadingMode(SKMapSettings.SKHeadingMode.NONE);
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
  public void onTrackChanged(TrackChangedEvent event) {
    Log.d(TAG, "onTrackChanged: track size is " + event.track.size());
    synchronized (event.track) {
      refreshTrackPolyline(event.track);
    }
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
  public void onLocationChanged(LocationEvent event) {
    if (mMapEnabled) {
      if (mapView != null && mCurrentMode == MODE_IDLE) {
        mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
      }
      if (noPositionYet) {
        noPositionYet = false;
        final double latitude = event.location.getLatitude();
        final double longitude = event.location.getLongitude();
        if (!appPrefs.isFirstRun()) {
          setMetrics(latitude, longitude);
        }
        appPrefs.saveLastLocation(event.location);
        if (mapView != null) {
          mapView.post(() -> {
            if (mapView != null && mCurrentMode == MODE_IDLE) {
              if (latitude <= 0 && longitude <= 0) {
                mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_NONE);
              } else {
                mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
              }
            }
          });
        }
      }
      if (mapView != null && event.shouldCenter) {
        mapView.centerOnCurrentPosition(16, true, 1000);
      }
    }
  }

  private void setMetrics(double latitude, double longitude) {
    if (Utils.isInsideBoundingBox(latitude, longitude, boundingBoxUS.getTopLeft().getLatitude(), boundingBoxUS.getTopLeft().getLongitude(),
                                  boundingBoxUS.getBottomRight().getLatitude(), boundingBoxUS.getBottomRight().getLongitude())) {
      appPrefs.setUsingMetricUnits(false);
    } else {
      appPrefs.setUsingMetricUnits(true);
    }
  }

  /**
   * displays a specific sequence, other sequences get cleared
   *
   * @param nodes the coordinates and the images
   */
  @SuppressWarnings("unchecked")
  private void displaySequence(final ArrayList<SKCoordinate> nodes, boolean isLocal, int startIndex) {
    Log.d(TAG, "displaySequence: ");
    // set the nodes on the polyline
    if (nodes != null && nodes.size() != 0 && mapView != null && mCurrentSequence != null) {
      int sequenceId = mCurrentSequence.getId();
      mapView.clearAllOverlays();
      mLastSegmentsDisplayed = null;
      mapView.deleteAllAnnotationsAndCustomPOIs();
      mPreviewNodes = (ArrayList<ImageCoordinate>) nodes.clone();
      final ArrayList<SKCoordinate> coords = new ArrayList<>();
      coords.addAll(nodes);
      Polyline polyline = new Polyline(sequenceId);
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
    //int height = (int) (Utils.dpToPx(activity, activity.getResources().getConfiguration().screenHeightDp) * 0.64f)
    //mapView.fitBoundingBox(boundingBox, paddingX, paddingY, paddingY / 2 + height, paddingY / 2)
    ////todo uncomment for when, sdk method for fit bb is fixed and track preview card will have no background only map
    mapView.fitBoundingBox(boundingBox, paddingX, paddingY, paddingY / 2, paddingY / 2);
  }

  /**
   * displayes a polyline for a list of coords
   */
  private void displayPolyline(Polyline polyline) {
    if (mapView != null && polyline != null && polyline.getNodes().size() > 0) {
      // set polyline color
      int outlineSize = 3;
      if (polyline.isLocal) {
        polyline.setColor(new float[] {0f, 0f, 0f, 1.0f});  //black
        polyline.setOutlineColor(new float[] {0f, 0f, 0f, 1.0f});  //black
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
          polyline
              .setColor(new float[] {189f / 255f, 16f / 255f, 224f / 255f, Math.min(polyline.coverage, 10) * 0.09f + 0.1f}); //accent color
          polyline.setOutlineColor(
              new float[] {189f / 255f, 16f / 255f, 224f / 255f, Math.min(polyline.coverage, 10) * 0.09f + 0.1f}); //accent color
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
    if (mapView != null) {
      if (mCurrentSequence != null) {
        mapView.clearOverlay(mCurrentSequence.getId());
      }
      mapView.deleteAllAnnotationsAndCustomPOIs();
      mapView.setCurrentPositionIcon(SKMapSurfaceView.SKCurrentPositionIconArrowType.CCP_BLUE_DOT);
    }
    mCurrentSequence = null;
    System.gc();
    diplayLocalSequences();
  }

  /**
   * views the image from startIndex, moving the marker on the map
   *
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

  private void refreshTrackPolyline(ArrayList<SKCoordinate> track) {
    if (mRecording && mTrackPolyline != null && mapView != null) {
      mTrackPolyline.setNodes(track);
      mapView.addPolyline(mTrackPolyline);
    }
  }

  @Override
  public void setDisplayData(Sequence extra) {
    mCurrentSequence = extra;
  }

  private void enterMode(int mode) {
    Log.d(TAG, "enterMode: " + mode);
    mCurrentMode = mode;
    switch (mode) {
      case MODE_IDLE:
        removeSequence();
        mRecording = false;
        break;
      case MODE_RECORDING_SCREEN:
        if (mLastSegmentsDisplayed == null || !mLastSegmentsDisplayed.matcher) {
          EventBus.post(new BroadcastSegmentsCommand());
        }
        break;
      case MODE_TRACK_PREVIEW:
        break;
    }
  }

  public void onPlaying() {
    //nothing
  }

  public void onPaused() {
    //nothing
  }

  public void onStopped() {
    //nothing
  }

  @Override
  public void onPreparing() {
  }

  @Override
  public void onPrepared(boolean success) {
    Log.d(TAG, "onPrepared: ");
    if (mapView != null) {
      mapView.post(() -> {
        if (mCurrentSequence != null) {
          displaySequence((ArrayList<SKCoordinate>) mCurrentSequence.getPolyline().getNodes(), !mCurrentSequence.isSafe(),
                          mCurrentSequence.getRequestedFrameIndex());
        }
      });
    }
  }

  @Override
  public void onProgressChanged(final int index) {
    if (mapView != null) {
      mapView.post(() -> {
        Log.d(TAG, "onProgressChanged: " + index);
        viewFrame(index);
      });
    }
  }

  @Override
  public void onExit() {
    removeSequence();
    onPositionerClicked(null);
    mCurrentSequence = null;
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
  public void onRecordingStatusChanged(final RecordingEvent event) {
    mRecording = event.started;
    if (!event.started && mapView != null) {
      Log.d(TAG, "onRecordingStatusChanged: deleting polyline");
      mapView.clearOverlay(TRACK_POLYLINE_ID);
    }
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
      mapView.clearOverlay(event.deletedSequenceId);
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
          mMatchedPolyline.setColor(new float[] {189f / 255f, 16f / 255f, 224f / 255f, 1f}); //washed out accent color
          mMatchedPolyline.setOutlineColor(new float[] {189f / 255f, 16f / 255f, 224f / 255f, 1f}); //washed out accent color
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
        mMatchedPolyline.setColor(new float[] {0f / 255f, 125f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
        mMatchedPolyline
            .setOutlineColor(new float[] {0f / 255f, 125f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
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
        poly.setColor(new float[] {0f / 255f, 255f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
        poly.setOutlineColor(new float[] {0f / 255f, 255f / 255f, 0f / 255f, 1.0f});  //washed out red, inverse of washed out accent
        poly.setOutlineSize(3);
        poly.setOutlineDottedPixelsSolid(50000);
        poly.setOutlineDottedPixelsSkip(1);
        poly.setLineSize(5);
        mapView.addPolyline(poly);
        SKCoordinate ref = event.segment.getReference();
        SKAnnotation annot = new SKAnnotation(MATCHED_SEGMENT_POLYLINE_ID + 1);
        annot.setLocation(ref);
        annot.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_RED);
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
      mHandler.postDelayed(() -> EventBus.postSticky(event), 500);
      return;
    }
    Log.d(TAG, "onSegmentsReceived: from " + (event.matcher ? "matcher" : "map"));
    if (event.all != null) {
      for (Polyline poly : event.all) {
        displayPolyline(poly);
      }
      mLastSegmentsDisplayed = event;
    }
  }

  /**
   * helper impl.
   */

  @SuppressWarnings("unused")
  public void resizeStarted() {
    if (mapViewGroup != null) {
      mapViewGroup.setVisibility(View.GONE);
    }
    if (chessBackground != null) {
      chessBackground.setVisibility(View.VISIBLE);
    }
    if (recordButton != null && positionButton != null) {
      recordButton.setVisibility(View.INVISIBLE);
      positionButton.setVisibility(View.INVISIBLE);
    }
  }

  private void resizeStopped() {
    if (mapViewGroup != null) {
      mapViewGroup.setVisibility(View.VISIBLE);
    }
    view.postDelayed(() -> {
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
    }, 300);
  }

  public void cancelAction() {
    if (mGeometryRetriever != null) {
      mGeometryRetriever.cancelNearby();
    }
  }
}
