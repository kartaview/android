package com.telenav.osv.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.data.location.datasource.LocationLocalDataSource
import com.telenav.osv.data.user.datasource.UserDataSource
import com.telenav.osv.manager.network.GeometryRetriever
import com.telenav.osv.map.render.mapbox.grid.loader.GridsLoader
import com.telenav.osv.ui.fragment.camera.controls.viewmodel.RecordingViewModel

class MapViewModelFactory(private val locationLocalDataSource: LocationLocalDataSource,
                          private val userDataSource: UserDataSource,
                          private val gridsLoader: GridsLoader,
                          private val geometryRetriever: GeometryRetriever,
                          private val applicationPreferences: ApplicationPreferences,
                          private val recordingViewModel: RecordingViewModel) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(locationLocalDataSource, userDataSource, gridsLoader, geometryRetriever, applicationPreferences, recordingViewModel) as T
    }
}