package com.telenav.osv.recorder.gpsTrail

import android.location.Location
import com.telenav.osv.common.filter.FilterFactory
import com.telenav.osv.location.LocationService
import com.telenav.osv.location.filter.LocationFilterType
import com.telenav.osv.utils.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Helper class which handle all logic for starting/stopping gps trail updates.
 *
 * This will also provide listener based callback for everyone who will listen to the trail updates.
 */
class GpsTrailHelper(private val locationService: LocationService) {

    init {
        TAG = GpsTrailHelper::class.java.simpleName
    }

    private val compositeDisposable = CompositeDisposable()

    private var gpsTrail: MutableList<Location> = arrayListOf()

    private var gpsTrailListeners = CopyOnWriteArraySet<ListenerRecordingGpsTrail>()

    /**
     * Starts the gps trail process.
     */
    fun start(recordingListenerGpsTrail: ListenerRecordingGpsTrail?) {
        Log.d(TAG, "start. Status: Starting gps trail updates.")
        recordingListenerGpsTrail?.let {
            setGpsTrailListener(recordingListenerGpsTrail)
        }
        gpsTrail.clear()
        initLocationUpdates()
    }

    fun stop(recordingListenerGpsTrail: ListenerRecordingGpsTrail?) {
        Log.d(TAG, "stop. Status: Stopping gps trail updates.")
        recordingListenerGpsTrail?.let {
            removeListenerGpsTrail(recordingListenerGpsTrail)
        }
        gpsTrail.clear()
    }

    fun setGpsTrailListener(recordingListenerGpsTrail: ListenerRecordingGpsTrail) {
        Log.d(TAG, "setGpsTrailListener. Listener: $recordingListenerGpsTrail")
        gpsTrailListeners.add(recordingListenerGpsTrail)
    }

    fun removeListenerGpsTrail(recordingListenerGpsTrail: ListenerRecordingGpsTrail) {
        Log.d(TAG, "removeListenerGpsTrail. Listener: $recordingListenerGpsTrail")
        gpsTrailListeners.remove(recordingListenerGpsTrail)
    }

    private fun initLocationUpdates() {
        compositeDisposable.add(locationService.locationUpdates
                .filter(FilterFactory.getLocationFilter(LocationFilterType.FILTER_ZERO_VALUES))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    gpsTrail.add(it)
                    notifyListeners()
                })
    }

    private fun notifyListeners() {
        gpsTrailListeners.forEach { recordingListener ->
            recordingListener.onGpsTrailChanged(gpsTrail)
        }
    }

    private companion object {
        private lateinit var TAG: String
    }
}