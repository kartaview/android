package com.telenav.osv.tasks.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telenav.osv.data.user.datasource.UserDataSource
import com.telenav.osv.jarvis.login.usecase.JarvisLoginUseCase
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.tasks.usecases.FetchAssignedTasksUseCase
import com.telenav.osv.tasks.utils.CurrencyUtil

@Suppress("UNCHECKED_CAST")
internal class TasksViewModelFactory(
        private val fetchAssignedTasksUseCase: FetchAssignedTasksUseCase,
        private val currencyUtil: CurrencyUtil,
        private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TasksViewModel(
                fetchAssignedTasksUseCase,
                currencyUtil,
                genericJarvisApiErrorHandler) as T
    }
}