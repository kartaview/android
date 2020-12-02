package com.telenav.osv.map.render.mapbox.grid.loader

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.network.GenericJarvisApiErrorHandlerListener
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.usecases.FetchAssignedTasksUseCase
import com.telenav.osv.utils.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception

/**
 * Handles the load logic for the grids. This will apply a 3 bounding box system where the current bounding box (visible screen size) is checked against a minimum threshold bounding box and a maximum threshold bounding box.
 * If any of the corners intersect the maximum threshold bbox the grids load trigger will be performed.
 *
 * The MTB (Maximum Threshold Bbox) will contain the bbox used for grid load, always,  the current and minimal bboxes are only used for triggering load logic.
 */
class GridsLoader(private val fetchAssignedTasksUseCase: FetchAssignedTasksUseCase, private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler) {

    init {
        TAG = GridsLoader::class.java.simpleName
        clear()
    }

    private lateinit var disposables: CompositeDisposable

    private var minimumThresholdBBox: LatLngBounds? = null

    private var maximumThresholdBBox: LatLngBounds? = null

    private var coefficientX = 0.0

    private var coefficientY = 0.0

    /**
     * Loads the grids if the bellow logic is passed. This will either trigger the [successCallback] or the [errorCallback] based on the api response.
     *
     * The [visibleRegion] will be checked against the calculated [minimumThresholdBBox] and [maximumThresholdBBox], based on this the trigger for load will be when:
     * * [maximumThresholdBBox] is empty
     * * [visibleRegion] is outside [minimumThresholdBBox] meaning is entirely in the [maximumThresholdBBox]
     * * [visibleRegion] points intersect any [maximumThresholdBBox] points
     * @param visibleRegion the visible region representing the bounding box of the screen
     * @param currentUserLocation the current user location which will be used to calculate [minimumThresholdBBox] and [maximumThresholdBBox]
     * @param successCallback the callback used for a success grid load request
     * @param errorCallback the callback used for error grid load request
     * @param forceLoad the status which if set it will trigger a load without any checks
     */
    fun loadIfNecessary(visibleRegion: LatLngBounds, currentUserLocation: LatLng, successCallback: (List<Task>) -> Unit, errorCallback: (Throwable) -> Unit, forceLoad: Boolean = false) {
        if (maximumThresholdBBox == null || forceLoad) {
            Log.d(TAG, "loadIfNecessary. Status: Load data. Force load: $forceLoad. Maximum threshold: $maximumThresholdBBox.")
            reloadBoundingBox(visibleRegion, currentUserLocation)
            loadGrids(successCallback, errorCallback)
            return
        }

        if (minimumThresholdBBox != null) {
            val minIntersection = visibleRegion.intersect(minimumThresholdBBox!!)
            if (minIntersection == null || minIntersection != visibleRegion) {
                Log.d(TAG, "loadIfNecessary. Status: Loading grids. Message: Visible region intersect max region.")
                reloadBoundingBox(visibleRegion, currentUserLocation)
                loadGrids(successCallback, errorCallback)
                return
            }
        }

        Log.d(TAG, "loadIfNecessary. Status: Ignoring grids request. Message: Visible region does not intersect with max region.")
    }

    /**
     * Clears the resources from the grids setting them at the initial state.
     */
    fun clear() {
        disposables = CompositeDisposable()
        minimumThresholdBBox = null
        maximumThresholdBBox = null
        coefficientX = 0.0
        coefficientY = 0.0
    }

    private fun calculateCoefficient(bboxCoordinate: Double, currentUserCoordinate: Double): Double {
        var coefficient = bboxCoordinate - currentUserCoordinate
        if (coefficient < 0.0) {
            coefficient *= SIGN_CHANGE_VALUE
        }
        return coefficient
    }

    private fun loadGrids(successCallback: (List<Task>) -> Unit, errorCallback: (Throwable) -> Unit) {
        maximumThresholdBBox?.let { bounds ->
            disposables.add(fetchAssignedTasksUseCase
                    .fetchTasks(bounds.northEast.latitude, bounds.northEast.longitude, bounds.southWest.latitude, bounds.southWest.longitude)
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                successCallback(it)
                            },
                            {
                                onGridsLoadHandleError(it, successCallback, errorCallback)
                            }))
        }
    }

    private fun onGridsLoadHandleError(it: Throwable, successCallback: (List<Task>) -> Unit, errorCallback: (Throwable) -> Unit) {
        genericJarvisApiErrorHandler.onError(
                it,
                object : GenericJarvisApiErrorHandlerListener {
                    override fun onRefreshTokenSuccess() {
                        loadGrids(successCallback, errorCallback)
                    }

                    override fun onError() {
                        successCallback(listOf())
                    }

                    override fun reLogin() {
                        Log.d(TAG, "onGridsLoadHandleError. Status: error. Message: ${it.message}.")
                        errorCallback(it)
                    }
                },
                disposables)
    }

    private fun reloadBoundingBox(visibleRegion: LatLngBounds, currentUserLocation: LatLng) {
        coefficientX = calculateCoefficient(visibleRegion.lonEast, currentUserLocation.longitude)
        coefficientY = calculateCoefficient(visibleRegion.latNorth, currentUserLocation.latitude)
        minimumThresholdBBox = loadThreshold(THRESHOLD_MULTIPLIER_MIN, currentUserLocation)
        maximumThresholdBBox = loadThreshold(THRESHOLD_MULTIPLIER_MAX, currentUserLocation)
        Log.d(TAG, "reloadBoundingBox. Status: reload data. CoefficientX: $coefficientX. CoefficientY: $coefficientY. Minimum Bbox: $minimumThresholdBBox. Maximum BBOX: $maximumThresholdBBox")
    }

    private fun loadThreshold(thresholdMultiplier: Int, currentUserLocation: LatLng): LatLngBounds? {
        val newLatitudeValue = coefficientY * thresholdMultiplier
        val newLongitudeValue = coefficientX * thresholdMultiplier
        var newLatLngBounds: LatLngBounds? = null
        try {
            newLatLngBounds = LatLngBounds.from(currentUserLocation.latitude + newLatitudeValue, currentUserLocation.longitude + newLongitudeValue, currentUserLocation.latitude - newLatitudeValue, currentUserLocation.longitude - newLongitudeValue)
        } catch (e: Exception) {
            Log.d(TAG, "loadThreshold. Status: error. Message: ${e.message}")
        }
        return newLatLngBounds
    }

    private companion object {
        private const val THRESHOLD_MULTIPLIER_MIN = 3
        private const val THRESHOLD_MULTIPLIER_MAX = 5
        private const val SIGN_CHANGE_VALUE = -1
        private lateinit var TAG: String
    }
}