package com.telenav.osv.tasks.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.data.user.datasource.UserDataSource
import com.telenav.osv.jarvis.login.usecase.JarvisLoginUseCase
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.tasks.usecases.TaskDetailsUseCase
import com.telenav.osv.tasks.utils.CurrencyUtil

@Suppress("UNCHECKED_CAST")
internal class TaskDetailsViewModelFactory(
        private val taskDetailsUseCase: TaskDetailsUseCase,
        private val userDataSource: UserDataSource,
        private val taskId: String?,
        private val currencyUtil: CurrencyUtil,
        private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TaskDetailsViewModel(
                taskDetailsUseCase,
                userDataSource,
                taskId,
                currencyUtil,
                genericJarvisApiErrorHandler) as T
    }
}