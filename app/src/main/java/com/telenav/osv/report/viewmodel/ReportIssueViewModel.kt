package com.telenav.osv.report.viewmodel

import android.location.Location
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.osv.R
import com.telenav.osv.location.LocationService
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.network.GenericJarvisApiErrorHandlerListener
import com.telenav.osv.report.model.ClosedRoadType
import com.telenav.osv.report.network.ClosedRoadRequest
import com.telenav.osv.report.usecase.ReportIssueUseCase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

internal class ReportIssueViewModel(
        private val reportIssueUseCase: ReportIssueUseCase,
        private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler,
        private val locationService: LocationService
): ViewModel() {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private val mutableNoteText: MutableLiveData<String> = MutableLiveData()
    private val mutableIsLoaderVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsSubmitLayoutVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableClosedRoadType: MutableLiveData<ClosedRoadType> = MutableLiveData()
    private val mutableIsSubmitIssueSuccess: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableShouldReLogin: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsGetLocationError: MutableLiveData<Boolean> = MutableLiveData()
    val noteText: LiveData<String> = mutableNoteText
    val isLoaderVisible: LiveData<Boolean> = mutableIsLoaderVisible
    val isSubmitLayoutVisible: LiveData<Boolean> = mutableIsSubmitLayoutVisible
    private val closedRoadType: LiveData<ClosedRoadType> = mutableClosedRoadType
    val isSubmitIssueSuccess: LiveData<Boolean> = mutableIsSubmitIssueSuccess
    val shouldReLogin: LiveData<Boolean> = mutableShouldReLogin
    val isGetLocationError: LiveData<Boolean> = mutableIsGetLocationError

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun onNoteTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        val updatedText = charSequence.toString()
        mutableNoteText.value = updatedText
    }

    fun navigateBack() {
        mutableNoteText.value = ""
        setReportLayoutVisibility(false)
    }

    fun reportIssue(view: View) {
        setReportLayoutVisibility(true)
        when (view.id) {
            R.id.cl_road_closed -> mutableClosedRoadType.value = ClosedRoadType.CLOSED
            R.id.cl_inaccessible -> mutableClosedRoadType.value = ClosedRoadType.NARROW
            R.id.cl_note -> mutableClosedRoadType.value = ClosedRoadType.OTHER
        }
    }

    fun submitIssue() {
        disposables.add(locationService.lastKnownLocation
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        { location ->
                            onGetLocationSuccess(location)
                        },
                        {
                            onGetLocationError()
                        }))
    }

    private fun onGetLocationSuccess(location: Location) {
        val note = noteText.value?.trim()
        val type = closedRoadType.value!!.type
        val coordinates = listOf(location.longitude, location.latitude)
        val closedRoadRequest = if (note.isNullOrEmpty()) {
            ClosedRoadRequest(coordinates = coordinates, type = type)
        } else {
            ClosedRoadRequest(coordinates, note, type)
        }
        disposables.add(reportIssueUseCase.reportClosedRoad(closedRoadRequest)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            mutableIsLoaderVisible.value = false
                            mutableIsSubmitIssueSuccess.value = true
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            submitIssue()
                                        }

                                        override fun onError() {
                                            onSubmitIssueError()
                                        }

                                        override fun reLogin() {
                                            onReLogin()
                                        }

                                    },
                                    disposables
                            )
                        }
                ))
    }

    private fun onGetLocationError() {
        mutableIsLoaderVisible.value = false
        mutableIsGetLocationError.value = true
    }

    private fun onSubmitIssueError() {
        mutableIsLoaderVisible.value = false
        mutableIsSubmitIssueSuccess.value = false
    }

    private fun onReLogin() {
        mutableIsLoaderVisible.value = false
        mutableShouldReLogin.value = true
    }

    private fun setReportLayoutVisibility(visible: Boolean) {
        mutableIsSubmitLayoutVisible.value = visible
    }
}