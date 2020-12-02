package com.telenav.osv.report.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telenav.osv.location.LocationService
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.report.usecase.ReportIssueUseCase

internal class ReportIssueViewModelFactory(
        private val reportIssueUseCase: ReportIssueUseCase,
        private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler,
        private val locationService: LocationService
): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ReportIssueViewModel(
                reportIssueUseCase,
                genericJarvisApiErrorHandler,
                locationService) as T
    }
}