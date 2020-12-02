package com.telenav.osv.map

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraMoveStartedListener.REASON_API_GESTURE
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
import com.mapbox.mapboxsdk.maps.Style
import com.telenav.osv.R
import com.telenav.osv.activity.KVActivityTempBase
import com.telenav.osv.activity.LocationPermissionsListener
import com.telenav.osv.activity.MainActivity
import com.telenav.osv.activity.OSVActivity
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.KVApplication
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.common.Injection
import com.telenav.osv.common.dialog.KVDialog
import com.telenav.osv.databinding.FragmentMapBinding
import com.telenav.osv.jarvis.login.utils.LoginUtils
import com.telenav.osv.location.LocationService
import com.telenav.osv.manager.playback.PlaybackManager
import com.telenav.osv.map.model.*
import com.telenav.osv.map.model.MapModes
import com.telenav.osv.map.render.MapRender
import com.telenav.osv.map.render.template.MapRenderTemplateIdentifier
import com.telenav.osv.map.viewmodel.MapViewModel
import com.telenav.osv.recorder.gpsTrail.ListenerRecordingGpsTrail
import com.telenav.osv.tasks.activity.KEY_TASK_ID
import com.telenav.osv.tasks.activity.TaskActivity
import com.telenav.osv.ui.ScreenComposer
import com.telenav.osv.ui.fragment.DisplayFragment
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.RecordingViewModel
import com.telenav.osv.utils.Log
import com.telenav.osv.utils.LogUtils
import com.telenav.osv.utils.PermissionUtils
import com.telenav.osv.utils.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_map.*

/**
 * Fragment which represent the map feature. Holds logic in order to render a usable map by using the internal [MapboxMap] and helper [MapRender].
 */
class MapFragment(private var mapMode: MapModes = MapModes.IDLE, private var playbackManager: PlaybackManager? = null) : DisplayFragment(), LocationPermissionsListener, ListenerRecordingGpsTrail {

    init {
        TAG = MapFragment::class.java.simpleName
    }

    private var mapRender: MapRender? = null
    private lateinit var appPrefs: ApplicationPreferences
    private var mapboxMap: MapboxMap? = null
    private lateinit var fragmentMapBinding: FragmentMapBinding
    private lateinit var locationService: LocationService
    private lateinit var recordingViewModel: RecordingViewModel
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var sessionExpireDialog: KVDialog? = null

    private val mapClickListener: MapboxMap.OnMapClickListener = MapboxMap.OnMapClickListener {
        if (LoginUtils.isLoginTypePartner(appPrefs)) {
            val selectedGridId = mapRender?.mapGridClick(it)
            if (selectedGridId != null) {
                val taskDetailsIntent = Intent(context, TaskActivity::class.java)
                taskDetailsIntent.putExtra(KEY_TASK_ID, selectedGridId)
                context?.startActivity(taskDetailsIntent)
                return@OnMapClickListener true
            } else return@OnMapClickListener mapViewModel.onNearbySequencesClick(it)
        } else {
            return@OnMapClickListener mapViewModel.onNearbySequencesClick(it)
        }
    }

    private val onCameraMoveStartedListener: MapboxMap.OnCameraMoveStartedListener = MapboxMap.OnCameraMoveStartedListener {
        if (it == REASON_API_GESTURE || it == REASON_DEVELOPER_ANIMATION) {
            onMapMove()
        }
    }

    private val mapViewModel: MapViewModel by lazy {
        ViewModelProviders.of(this,
                Injection.provideMapViewFactory(Injection.provideLocationLocalDataSource(context!!),
                        Injection.provideUserRepository(context!!),
                        Injection.provideGridsLoader(
                                Injection.provideFetchAssignedTasksUseCase(
                                        Injection.provideTasksApi(true, Injection.provideApplicationPreferences(context!!))),
                                Injection.provideGenericJarvisApiErrorHandler(context!!, Injection.provideApplicationPreferences(context!!))),
                        Injection.provideGeometryRetriever(context!!, Injection.provideNetworkFactoryUrl(Injection.provideApplicationPreferences(context!!))),
                        Injection.provideApplicationPreferences(context!!),
                        recordingViewModel)).get(MapViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        this.recordingViewModel = ViewModelProviders.of(activity!!).get(RecordingViewModel::class.java)
        activity?.let {
            appPrefs = (it.application as KVApplication).appPrefs
            locationService = Injection.provideLocationService(it.applicationContext)
            initLocation()
        }
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate. Status: restore saved instance state.")
            val savedMapMode = MapModes.getByType(savedInstanceState.getInt(KEY_MAP_MODE))
            if (savedMapMode != null) {
                this.mapMode = savedMapMode
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentMapBinding = FragmentMapBinding.inflate(inflater, container, false).apply {
            clickListenerCamera = View.OnClickListener {
                activity?.let {
                    if (it is MainActivity) {
                        it.goToRecordingScreen()
                    }
                }
            }
            clickListenerCenter = View.OnClickListener {
                getLastKnowLocationAsync { location -> mapRender?.centerOnCurrentLocation(location) }
            }
            lifecycleOwner = this@MapFragment
            viewModel = mapViewModel
        }
        return fragmentMapBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentMapBinding.tvTasks.setOnClickListener {
            context?.startActivity(Intent(context, TaskActivity::class.java))
        }
        observerOnLoginDialog()
        observeOnNearbySequences()
        observeOnEnableProgress()
        observeOnMapRender()
        observeOnMapUpdate()
        observeOnRecordingStateChanged()
        setMarginForRecordingIfRequired()
        mapView?.onCreate(savedInstanceState)
    }

    override fun onStart() {
        mapViewModel.enableEventBus(true)
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        if (recordingViewModel.isRecording) {
            recordingViewModel.setListenerRecordingGpsTrail(this)
        }
        switchMapMode(mapMode, playbackManager)
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
        recordingViewModel.removeListenerRecordingGpsTrail(this)
    }

    override fun setSource(extra: Any?) {

    }

    override fun onStop() {
        mapViewModel.enableEventBus(false)
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }

    override fun onDestroyView() {
        this.mapboxMap?.removeOnCameraMoveStartedListener(onCameraMoveStartedListener)
        mapRender?.clearMap()
        mapView?.onDestroy()
        Log.d(TAG, "onDestroyView. Map loaded: ${mapView != null}")
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            this.putInt(KEY_MAP_MODE, mapMode.mode)
        }
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLocationPermissionGranted() {
        LogUtils.logDebug(TAG, "onLocationPermissionGranted. Initialising map.")
        getLastKnowLocationAsync { location -> mapRender?.centerOnCurrentLocation(location) }
        initLocation()
    }

    override fun onLocationPermissionDenied() {
        context?.let {
            Toast.makeText(context, R.string.enable_location_label, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGpsTrailChanged(gpsTrail: List<Location>) {
        onMapMove()
        mapRender?.updateRecording(gpsTrail)
    }

    /**
     * Switches the map mode for the map. This method only exposes the viewModel logic.
     * //ToDo: to be removed, recommended inject the view model directly for direct control of the fragment.
     */
    fun switchMapMode(mapMode: MapModes, playbackManager: PlaybackManager? = null) {
        activity?.let {
            this.mapMode = mapMode
            //preserve reference to the playback manager for the case when the fragment is not added yet
            this.playbackManager = playbackManager
            mapViewModel.setupMapResource(Injection.provideMapBoxOkHttpClient(appPrefs))
            if (isAdded) {
                Log.d(TAG, "Switching map mode: ${mapMode.mode}.")
                mapViewModel.switchMode(mapMode, playbackManager)
            }
        }
    }

    private fun setMarginForRecordingIfRequired() {
        if (mapMode.mode == MapModes.RECORDING.mode) {
            context?.let {
                val set = ConstraintSet()
                set.clone(fragmentMapBinding.root as ConstraintLayout)
                set.setMargin(R.id.mapView, ConstraintSet.TOP, 0)
                set.applyTo(fragmentMapBinding.root as ConstraintLayout)
            }
        }
    }

    private fun observeOnRecordingStateChanged() {
        recordingViewModel.let {
            Log.d(TAG, "observeOnRecordingStateChanged. Recording status: ${it.isRecording}")
            it.recordingObservable?.observe(this, Observer { recordingStatus ->
                Log.d(TAG, "recordingObservable. Recording status: $recordingStatus")
                if (recordingStatus) {
                    recordingViewModel.setListenerRecordingGpsTrail(this)
                } else {
                    mapRender?.clearGpsTrail()
                    recordingViewModel.removeListenerRecordingGpsTrail(this)
                    mapViewModel.switchMode(mapMode, null)
                }
            })
        }
    }

    private fun observeOnNearbySequences() {
        mapViewModel.nearbySequences.observe(this, Observer {
            val activity = activity as OSVActivity
            if (it != null) {
                activity.openScreen(ScreenComposer.SCREEN_NEARBY, it)
            } else {
                activity.showSnackBar(getString(R.string.nearby_no_result_label), Snackbar.LENGTH_SHORT)
            }
        })
    }

    /**
     * This method displays alert dialog for expired session
     */
    private fun showSessionExpiredDialog(context: Context) {
        if (sessionExpireDialog == null) {
            sessionExpireDialog = LoginUtils.getSessionExpiredDialog(context)
        }
        sessionExpireDialog?.show()
    }

    private fun observeOnEnableProgress() {
        mapViewModel.enableProgress.observe(this, Observer {
            (activity as OSVActivity).enableProgressBar(it)
        })
    }

    private fun observerOnLoginDialog() {
        mapViewModel.shouldReLogin.observe(this, Observer { shouldReLogin ->
            if (shouldReLogin) {
                context?.let { showSessionExpiredDialog(it) }
            }
        })
    }

    private fun observeOnMapRender() {
        mapViewModel.mapRender.observe(this, Observer {
            Log.d(TAG, "observeOnMapRender. Map status change: $it")
            if (it.value == MapRenderMode.DISABLED.value) {
                mapRender?.clearMap()
                val mapView = fragmentMapBinding.root.findViewById(R.id.mapView) as View
                mapView.visibility = View.GONE
            } else {
                loadMap {
                    when (it.value) {
                        MapRenderMode.DEFAULT.value -> {
                            this.mapboxMap?.uiSettings?.setAllGesturesEnabled(true)
                            this.mapboxMap?.addOnCameraMoveStartedListener(onCameraMoveStartedListener)
                            this.mapboxMap?.addOnMapClickListener(mapClickListener)
                            mapRender?.render(MapRenderTemplateIdentifier.DEFAULT)
                            getLastKnowLocationAsync { location -> mapRender?.centerOnCurrentLocation(location) }
                        }
                        MapRenderMode.DEFAULT_WITH_GRID.value -> {
                            this.mapboxMap?.uiSettings?.setAllGesturesEnabled(true)
                            this.mapboxMap?.addOnCameraMoveStartedListener(onCameraMoveStartedListener)
                            mapRender?.render(MapRenderTemplateIdentifier.GRID)
                            this.mapboxMap?.addOnMapClickListener(mapClickListener)
                            getLastKnowLocationAsync { location ->
                                run {
                                    mapRender?.centerOnCurrentLocation(location)
                                    onMapMove(true)
                                }
                            }
                        }
                        MapRenderMode.PREVIEW.value -> {
                            this.mapboxMap?.uiSettings?.setAllGesturesEnabled(false)
                            this.mapboxMap?.removeOnMapClickListener(mapClickListener)
                            this.mapboxMap?.removeOnCameraMoveStartedListener(onCameraMoveStartedListener)
                            mapRender?.render(MapRenderTemplateIdentifier.PREVIEW)
                        }
                        MapRenderMode.RECORDING.value -> {
                            Log.d(TAG, "loadMap function. Status: map render recording function. Message: Preparing for recording.")
                            this.mapboxMap?.addOnCameraMoveStartedListener(onCameraMoveStartedListener)
                            this.mapboxMap?.uiSettings?.setAllGesturesEnabled(false)
                            this.mapboxMap?.uiSettings?.isZoomGesturesEnabled = true
                            this.mapboxMap?.removeOnMapClickListener(mapClickListener)
                            mapRender?.render(MapRenderTemplateIdentifier.RECORDING)
                        }
                        else -> {
                            //nothing since it is not required to add disabled
                        }
                    }
                }
            }
        })
    }

    private fun observeOnMapUpdate() {
        mapViewModel.mapRenderUpdate.observe(this, Observer {
            Log.d(TAG, "observeOnMapUpdate. Map update change: $it")
            when (it) {
                is MapUpdateDefault -> {
                    getLastKnowLocationAsync { location -> mapRender?.updateDefault(it.sequences, location) }
                }
                is MapUpdateGrid -> {
                    if (it.tasks.isNotEmpty()) {
                        mapRender?.refreshCoverage(it.tasks[0].createdAt)
                    }
                    mapRender?.updateGrid(it.tasks, it.jarvisUserId, it.includeLabels, it.sequences)
                }
                is MapUpdatePreview -> {
                    mapRender?.updatePreview(it.localSequence, it.symbolLocation)
                }
                is MapUpdateRecording -> {
                    getLastKnowLocationAsync { location ->
                        Log.d(TAG, "loadMap function. Status: centerOnCurrentPosition callback. Message: Preparing for render map in recording mode. Location: $location. Sequence size: ${it.sequences.size}")
                        run {
                            mapRender?.updateRecording(location, it.sequences)
                            onMapMove()
                        }
                    }
                }
            }
        })
    }

    private fun loadMap(function: () -> Unit) {
        mapView?.visibility = View.VISIBLE
        mapView?.getMapAsync { mapBoxMap ->
            if (this.mapboxMap == null) {
                Log.d(TAG, "loadMap. Status: initialising map.")
                this.mapboxMap = mapBoxMap
                this.mapboxMap?.setStyle(Style.LIGHT)
            }
            context?.let { context ->
                Log.d(TAG, "loadMap. Status: initialising map render.")
                if (mapRender == null) {
                    mapRender = MapRender(context, mapboxMap!!, Injection.provideNetworkFactoryUrl(appPrefs), Injection.provideCurrencyUtil())
                }
                function()
            }
        }
    }

    @SuppressLint("CheckResult")
    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun getLastKnowLocationAsync(callback: (LatLng) -> Unit) {
        activity?.let {
            val kvActivityTempBase = it as KVActivityTempBase
            val locationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
            if (PermissionUtils.isPermissionGranted(it.applicationContext, locationPermission)) {
                locationService
                        .lastKnownLocation
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ location ->
                            callback(LatLng(location.latitude, location.longitude))
                            LogUtils.logDebug(TAG, "getLastKnowLocationAsync. Status: success.")
                        }, { error: Throwable? ->
                            LogUtils.logDebug(TAG, "getLastKnowLocationAsync. Error: ${error?.message}")
                            if (!Utils.isGPSEnabled(it.applicationContext)) {
                                kvActivityTempBase.resolveLocationProblem()
                            }
                        })
            } else {
                PermissionUtils.checkPermissionsForGPS(it as Activity)
            }
        }
    }

    private fun initLocation() {
        activity?.let {
            if (PermissionUtils.isPermissionGranted(it.applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (Utils.isGPSEnabled(it.applicationContext)) {
                    locationService
                            .lastKnownLocation
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ location ->
                                appPrefs.saveBooleanPreference(PreferenceTypes.K_DISTANCE_UNIT_METRIC, !mapViewModel.initMetricBasedOnCcp(location))
                            }, { error: Throwable? ->
                                LogUtils.logDebug(TAG, "centerOnCurrentPosition. Error: ${error?.message}")
                            })
                }
            }
        }
    }

    private fun onMapMove(forceLoad: Boolean = false) {
        val loadGrid = mapMode.mode == MapModes.GRID.mode || mapMode.mode == MapModes.IDLE.mode || mapMode.mode == MapModes.RECORDING.mode
        Log.d(TAG, "onMapMove. Status: performing grid load if required. Required: $loadGrid. Map mode: ${mapMode.mode}. Force loads: $forceLoad")
        if (loadGrid) {
            mapboxMap?.let { mapboxMap ->
                getLastKnowLocationAsync { mapViewModel.onGridsLoadIfAvailable(mapboxMap.cameraPosition.zoom, mapboxMap.projection.visibleRegion.latLngBounds, it, forceLoad) }
            }
        }
    }

    companion object {

        lateinit var TAG: String

        private const val KEY_MAP_MODE = "key_map_mode"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment MapFragment.
         */
        @JvmStatic
        fun newInstance(mapMode: MapModes = MapModes.IDLE, playbackManager: PlaybackManager? = null) =
                MapFragment(mapMode, playbackManager)
    }
}