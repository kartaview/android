package com.telenav.osv.map.viewmodel

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.common.Injection
import com.telenav.osv.common.model.KVLatLng
import com.telenav.osv.data.location.datasource.LocationLocalDataSource
import com.telenav.osv.data.user.datasource.UserDataSource
import com.telenav.osv.event.EventBus
import com.telenav.osv.event.SdkEnabledEvent
import com.telenav.osv.event.network.matcher.KVBoundingBox
import com.telenav.osv.event.ui.UserTypeChangedEvent
import com.telenav.osv.item.network.TrackCollection
import com.telenav.osv.listener.network.NetworkResponseDataListener
import com.telenav.osv.manager.network.GeometryRetriever
import com.telenav.osv.manager.playback.OnlinePlaybackManager
import com.telenav.osv.manager.playback.PlaybackManager
import com.telenav.osv.manager.playback.SafePlaybackManager
import com.telenav.osv.manager.playback.VideoPlayerManager
import com.telenav.osv.map.MapFragment
import com.telenav.osv.map.model.*
import com.telenav.osv.map.model.MapModes
import com.telenav.osv.map.render.mapbox.grid.loader.GridsLoader
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.RecordingViewModel
import com.telenav.osv.utils.Log
import com.telenav.osv.utils.StringUtils
import com.telenav.osv.utils.Utils
import com.telenav.osv.utils.getMapMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * The view model related to Map functionality. Holds the logic to switch the map between modes by using the [switchMode] which will change the state of the map.
 */
class MapViewModel(private val locationLocalDataSource: LocationLocalDataSource,
                   private val userDataSource: UserDataSource,
                   private val gridsLoader: GridsLoader,
                   private val geometryRetriever: GeometryRetriever,
                   private val appPrefs: ApplicationPreferences,
                   private val recordingViewModel: RecordingViewModel) : ViewModel(), PlaybackManager.PlaybackListener {

    private var mutableRecordingEnable: MutableLiveData<Boolean> = MutableLiveData()
    private var mutableLocationEnable: MutableLiveData<Boolean> = MutableLiveData()
    private var mutableMapRender: MutableLiveData<MapRenderMode> = MutableLiveData()
    private var mutableMapUpdate: MutableLiveData<MapUpdateBase> = MutableLiveData()
    private var mutableNearby: MutableLiveData<TrackCollection?> = MutableLiveData()
    private var mutableEnableProgress: MutableLiveData<Boolean> = MutableLiveData()
    private var mutableEnableMyTasks: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableShouldReLogin: MutableLiveData<Boolean> = MutableLiveData()
    private var disposables: CompositeDisposable = CompositeDisposable()
    private var playbackManager: PlaybackManager? = null
    private var boundingBoxUS = KVBoundingBox(KVLatLng(49.034, -125.041), KVLatLng(24.519, -68.701))
    private var jarvisUserId: Int? = null

    init {
        TAG = MapViewModel::class.java.simpleName
    }

    /**
     * Observable for changing the state of driving.
     */
    val recordingEnable: LiveData<Boolean> = mutableRecordingEnable

    /**
     * Observable for changing the state of the rendering of points.
     */
    val locationEnable: LiveData<Boolean> = mutableLocationEnable

    /**
     * Observable for updating the the values of the renders on the map.
     */
    val mapRenderUpdate: LiveData<MapUpdateBase> = mutableMapUpdate

    /**
     * Observable for changing the state of the map rendering.
     */
    val mapRender: LiveData<MapRenderMode> = mutableMapRender

    /**
     * Observable for changing the state of the rendering of points.
     */
    val nearbySequences: LiveData<TrackCollection?> = mutableNearby

    /**
     * Observable for changing the state of the rendering of points.
     */
    val enableProgress: LiveData<Boolean> = mutableEnableProgress

    /**
     * Observable for changing the state of the grid request.
     */
    val enableMyTasks: LiveData<Boolean> = mutableEnableMyTasks

    /**
     * Observable for changing the state of re-login dialog
     */
    val shouldReLogin: LiveData<Boolean> = mutableShouldReLogin

    /**
     * The current render mode.
     */
    private var currentRenderMode = MapRenderMode.DEFAULT

    /**
     * The status representing if the zoom is between threshold values.
     */
    private var isCurrentZoomBetweenGivenValues: Boolean = false

    private var mapBoxJarvisOkHttpClient: OkHttpClient? = null

    /**
     * Function which switches the UI for different map modes. Available modes are based on [MapModes] enum, based on that:
     * * [MapModes.IDLE] - this will enable all the controls on the map (recording button, center CCP) and request for the current persisted local sequences by using the internal [displayAllLocalSequences]
     * * [MapModes.PREVIEW_MAP] - this will disable all the controls on the map, set the player in order to have access to the played local sequence and request for the current local sequence used in it by the internal method [displayPlayerSequence]
     * * [MapModes.DISABLED] - this will disable the center CCP control on the map and disable the map. The recording control is still available since you can record without a map displayed.
     * * [MapModes.PREVIEW_MAP_DISABLED] - this will disabled all the controls and the map.
     * * [MapModes.RECORDING] - this will enable the map similar with [MapModes.IDLE] but with focus/control elements off
     * * [MapModes.GRID]  - this will enable map similiar with [MapModes.IDLE] with added grids to display the available tasks for the user.
     * @param mapMode the map mode for which the UI will be switched and specific functionality will be called for that switch
     * @param playbackManager this is required for [MapModes.PREVIEW_MAP] mode otherwise it should not be set.
     */
    fun switchMode(mapMode: MapModes, playbackManager: PlaybackManager? = null) {
        Log.d(TAG, "switch map Mode: ${mapMode.mode}")
        when (mapMode) {
            MapModes.IDLE -> {
                currentRenderMode = MapRenderMode.DEFAULT
                mutableMapRender.postValue(currentRenderMode)
                enableMapButtons(true)
                mutableEnableMyTasks.postValue(isJarvisUser())
                displayAllLocalSequences(MapUpdateDefault())
            }
            MapModes.PREVIEW_MAP -> {
                currentRenderMode = MapRenderMode.PREVIEW
                mutableMapRender.postValue(currentRenderMode)
                mutableEnableMyTasks.postValue(false)
                enableMapButtons(false)
                disposables.clear()
                this.playbackManager = playbackManager
                playbackManager?.addPlaybackListener(this)
                if (playbackManager is SafePlaybackManager || playbackManager is VideoPlayerManager) {
                    displayPlayerSequence()
                }
            }
            MapModes.PREVIEW_MAP_DISABLED -> {
                currentRenderMode = MapRenderMode.DISABLED
                mutableMapRender.postValue(currentRenderMode)
                enableMapButtons(false)
                mutableEnableMyTasks.postValue(false)
                disposables.clear()
            }
            MapModes.DISABLED -> {
                currentRenderMode = MapRenderMode.DISABLED
                mutableMapRender.postValue(currentRenderMode)
                enableLocationButton(false)
                enableRecordingButton(true)
                mutableEnableMyTasks.postValue(isJarvisUser())
                disposables.clear()
            }
            MapModes.RECORDING -> {
                currentRenderMode = MapRenderMode.RECORDING
                mutableMapRender.postValue(currentRenderMode)
                enableMapButtons(false)
                mutableEnableMyTasks.postValue(false)
                displayAllLocalSequences(MapUpdateRecording())
            }
            MapModes.RECORDING_MAP_DISABLED -> {
                currentRenderMode = MapRenderMode.DISABLED
                mutableMapRender.postValue(currentRenderMode)
                enableMapButtons(false)
                mutableEnableMyTasks.postValue(false)
                disposables.clear()
            }
            MapModes.GRID -> {
                currentRenderMode = MapRenderMode.DEFAULT_WITH_GRID
                mutableMapRender.postValue(currentRenderMode)
                enableMapButtons(true)
                mutableEnableMyTasks.postValue(isJarvisUser())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
        geometryRetriever.destroy()
        gridsLoader.clear()
    }

    /**
     * Method for subscribe/unsubscribe to EventBus
     */
    fun enableEventBus(enable: Boolean) {
        if (enable) {
            EventBus.register(this)
        } else {
            EventBus.unregister(this)
        }
    }

    /**
     * This will check if there is a jarvis user logged in and if the current zoom is between [MIN_ZOOM_LEVEL] and [MAX_ZOOM_LEVEL]. If true the request for grids will be made which will be based on the current given bounding box.
     *
     * The grids request will be performed by [GridsLoader] class.
     *
     */
    fun onGridsLoadIfAvailable(currentZoom: Double, boundingBox: LatLngBounds, currentLocation: LatLng, forceLoad: Boolean) {
        val isJarvisUser = isJarvisUser()
        //logic for when to active the grids based on the current zoom level
        if (isJarvisUser) {
            if (currentZoom < MIN_ZOOM_LEVEL_THRESHOLD) {
                Log.d(TAG, "onGridsLoadIfAvailable. Status: Zoom smaller than min threshold. Message: Ignoring request. Current zoom: $currentZoom. Min threshold: $MIN_ZOOM_LEVEL_THRESHOLD")
            } else {
                val isCurrentZoomBetweenGivenValues = currentZoom >= MIN_ZOOM_LEVEL && currentZoom < MAX_ZOOM_LEVEL
                var bypassForceLoad = forceLoad
                if (!bypassForceLoad) {
                    bypassForceLoad = this.isCurrentZoomBetweenGivenValues != isCurrentZoomBetweenGivenValues
                }
                this.isCurrentZoomBetweenGivenValues = isCurrentZoomBetweenGivenValues
                gridsLoader.loadIfNecessary(
                        boundingBox,
                        currentLocation,
                        {
                            Log.d(TAG, "onGridsLoadIfAvailable. Status: success. Grids szie: ${it.size}. Include labels: $isCurrentZoomBetweenGivenValues. Current Zoom: $currentZoom.")
                            displayAllLocalSequences(MapUpdateGrid(it, jarvisUserId!!, isCurrentZoomBetweenGivenValues))
                        },
                        {
                            Log.d(TAG, "onGridsLoadHandleError. Status: error. Message: ${it.message}.")
                            displayAllLocalSequences(MapUpdateGrid())
                            mutableShouldReLogin.value = true
                        },
                        bypassForceLoad)
            }
        } else {
            Log.d(TAG, "onGridsLoadIfAvailable. Status: Jarvis user not found. Message: Ignoring request.")
        }
    }

    fun setupMapResource(mapBoxJarvisOkHttpClient: OkHttpClient) {
        disposables.add(userDataSource.user
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
                            if (it.jarvisUserId != 0) {
                                Log.d(TAG, "setupMapResources. Status: setting OkHttpClient for Jarvis authorization.")
                                this.jarvisUserId = it.jarvisUserId
                                HttpRequestUtil.setOkHttpClient(mapBoxJarvisOkHttpClient)
                            } else {
                                Log.d(TAG, "setupMapResources. Status: removing OkHttpClient for Jarvis authorization.")
                                HttpRequestUtil.setOkHttpClient(null)
                            }
                        },
                        {
                            Timber.d("error while getting the user type. Error: ${it.message}.")
                        }
                ))
    }

    /**
     * Logic which handles the nearby sequence logic by making a network request to the [geometryRetriever] which will either return the nearby sequences or empty.
     */
    fun onNearbySequencesClick(point: LatLng): Boolean {
        mutableEnableProgress.postValue(true)
        geometryRetriever.nearby(object : NetworkResponseDataListener<TrackCollection> {

            override fun requestFailed(status: Int, details: TrackCollection) {
                Log.d(TAG, "onMapClick. Nearby error: $details")
                mutableEnableProgress.postValue(false)
            }

            override fun requestFinished(status: Int, collection: TrackCollection) {
                Log.d(TAG, "onMapClick. Nearby success. Collection size: ${collection.trackList.size}")
                mutableEnableProgress.postValue(false)
                if (collection.trackList.size > 0) {
                    mutableNearby.postValue(collection)
                } else {
                    mutableNearby.postValue(null)
                }
            }
        }, point.latitude.toString(), point.longitude.toString())
        return true
    }

    override fun onPaused() {

    }

    override fun onStopped() {

    }

    override fun onPrepared() {
        if (playbackManager != null && playbackManager is OnlinePlaybackManager && playbackManager!!.track != null) {
            displayPlayerSequence()
        }
    }

    override fun onProgressChanged(index: Int) {
        //ToDo: Remove this abomination once Player is rewritten FROM SCRATCH
        val sequenceLocations = playbackManager!!.track!!.map { LatLng(it) }.toList()
        var ccp: LatLng? = null
        if (index < sequenceLocations.size) {
            ccp = sequenceLocations[index]
        }
        mutableMapUpdate.postValue(MapUpdatePreview(sequenceLocations, ccp))
    }

    override fun onExit() {
        Log.d(TAG, "onExit. Removed playback manager listener.")
        playbackManager = null
    }

    override fun onPlaying() {

    }

    private fun displayPlayerSequence() {
        val sequenceLocations = playbackManager!!.track!!.map { LatLng(it) }.toList()
        mutableMapUpdate.postValue(MapUpdatePreview(sequenceLocations, if (sequenceLocations.isEmpty()) null else sequenceLocations[0]))
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    fun onSdkEnabled(event: SdkEnabledEvent) {
        Log.d(TAG, "Evenbus onSdkEnabled.")
        switchMode(if (event.enabled) MapModes.IDLE else MapModes.DISABLED, null)
        EventBus.clear(SdkEnabledEvent::class.java)
    }

    //ToDo: remove this abomination once the EventBus is removed from the project, I am sorry for the workaround fix and the technical depth added
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onUserTypeChanged(event: UserTypeChangedEvent) {
        //for now this is the only way to check if a user has been logged out
        if (event.type == PreferenceTypes.USER_TYPE_UNKNOWN) {
            Log.d(MapFragment.TAG, "onUserTypeChanged. Status: unknown user type. Message: Clearing grids.")
            jarvisUserId = null
            HttpRequestUtil.setOkHttpClient(null)
            switchMode(appPrefs.getMapMode(), null)
            displayAllLocalSequences(MapUpdateDefault())
            mutableEnableMyTasks.postValue(false)
        } else {
            disposables.add(userDataSource.user
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                if (it.jarvisUserId != 0) {
                                    mapBoxJarvisOkHttpClient = Injection.provideMapBoxOkHttpClient(appPrefs)
                                    this.jarvisUserId = it.jarvisUserId
                                    HttpRequestUtil.setOkHttpClient(mapBoxJarvisOkHttpClient)
                                    Log.d(TAG, "onUserTypeChanged. Status: Jarvis user loaded. Message: Clearing previous coverages.")
                                    if (currentRenderMode != MapRenderMode.RECORDING) {
                                        switchMode(mapMode = MapModes.GRID)
                                    }
                                }
                            },
                            {
                                Log.d(TAG, "onUserTypeChanged. Status: Could not get user. Error: ${it.message}.")
                            }
                    ))
        }
    }

    /**
     * @return true if the jarvis id is set, false otherwise
     */
    private fun isJarvisUser(): Boolean {
        return jarvisUserId != null && jarvisUserId != 0
    }

    /**
     * Enables/disables the recording button.
     * @param enable `true` if the button should be visible, `false` otherwise.
     */
    private fun enableRecordingButton(enable: Boolean) {
        Log.d(TAG, "enableRecordingButton: $enable")
        mutableRecordingEnable.postValue(enable)
    }

    /**
     * This will enable/disable the metric/imperial system based on the current location if it is inside a specific bounding box.
     */
    fun initMetricBasedOnCcp(location: Location): Boolean {
        return (Utils.isInsideBoundingBox(
                location.latitude,
                location.longitude,
                boundingBoxUS.topLeft.lat,
                boundingBoxUS.topLeft.lon,
                boundingBoxUS.bottomRight.lat,
                boundingBoxUS.bottomRight.lon))
    }

    /**
     * Enables/disables the location button.
     * @param enable `true` if the button should be visible, `false` otherwise.
     */
    private fun enableLocationButton(enable: Boolean) {
        Log.d(TAG, "enableLocationButton: $enable")
        mutableLocationEnable.postValue(enable)
    }

    private fun displayAllLocalSequences(mapRenderBaseSequences: MapUpdateBaseSequences) {
        disposables.add(
                locationLocalDataSource
                        .locations
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { locations ->
                                    val sequences = mutableListOf<List<LatLng>>()
                                    if (locations.isEmpty()) {
                                        Log.d(TAG, "displayAllLocalSequences. Status: abort. Message: Empty sequences.")
                                        mapRenderBaseSequences.sequences = mutableListOf()
                                        mutableMapUpdate.postValue(mapRenderBaseSequences)
                                        return@subscribe
                                    }
                                    val sequencesGrouped = locations.filter { location ->
                                        location.sequenceId != (recordingViewModel.currentSequenceIdIfSet
                                                ?: StringUtils.EMPTY_STRING)
                                    }.groupBy { it.sequenceId }
                                    Log.d(TAG, "displayAllLocalSequences. Locations size: ${sequencesGrouped.size}")
                                    for ((_, value) in sequencesGrouped) {
                                        sequences.add(value.map { LatLng(it.location) })
                                    }
                                    mapRenderBaseSequences.sequences = sequences
                                    mutableMapUpdate.postValue(mapRenderBaseSequences)
                                },
                                //onError
                                { throwable ->
                                    Log.d(TAG, String.format("displayLocalSequencesRunnable. Status: error. Message: %s.", throwable.localizedMessage))
                                }))
    }

    /**
     * Enables/disables the map buttons, i.e. record and position.
     *
     *  The lint suppress is due to restriction of the api. While the recommendation is to use hide/show methods, that means those methods will use internally animators sets
     * which have issues when called on fragments which do not have callbacks of lifecycle when the view is changed/resized.
     * @param enable `true` will display the buttons, otherwise hide them.
     */
    @SuppressLint("RestrictedApi")
    fun enableMapButtons(enable: Boolean) {
        Log.d(TAG, "enableMapButtons: $enable")
        enableRecordingButton(enable)
        mutableLocationEnable.postValue(enable)
    }

    private companion object {
        private lateinit var TAG: String

        private const val MIN_ZOOM_LEVEL: Double = 13.2

        private const val MAX_ZOOM_LEVEL: Double = 16.2

        private const val MIN_ZOOM_LEVEL_THRESHOLD: Double = 9.1
    }
}