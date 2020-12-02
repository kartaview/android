package com.telenav.osv.tasks.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.network.GenericJarvisApiErrorHandlerListener
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.usecases.FetchAssignedTasksUseCase
import com.telenav.osv.tasks.utils.CurrencyUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

internal class TasksViewModel(
        private val fetchAssignedTasksUseCase: FetchAssignedTasksUseCase,
        private val currencyUtil: CurrencyUtil,
        private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler
) : ViewModel() {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private val mutableAssignedTasks: MutableLiveData<List<Task>> = MutableLiveData()
    private val mutableIsEmptyLayoutVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsErrorLayoutVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsDataLayoutVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsLoaderVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutablePaidAmount: MutableLiveData<String> = MutableLiveData()
    private val mutablePendingAmount: MutableLiveData<String> = MutableLiveData()
    private val mutableShouldReLogin: MutableLiveData<Boolean> = MutableLiveData()
    val assignedTasks: LiveData<List<Task>> = mutableAssignedTasks
    val isEmptyLayoutVisible: LiveData<Boolean> = mutableIsEmptyLayoutVisible
    val isErrorLayoutVisible: LiveData<Boolean> = mutableIsErrorLayoutVisible
    val isDataLayoutVisible: LiveData<Boolean> = mutableIsDataLayoutVisible
    val isLoaderVisible: LiveData<Boolean> = mutableIsLoaderVisible
    val paidAmount: LiveData<String> = mutablePaidAmount
    val pendingAmount: LiveData<String> = mutablePendingAmount
    val shouldReLogin: LiveData<Boolean> = mutableShouldReLogin

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun fetchAssignedTasks() {
        disposables.add(fetchAssignedTasksUseCase.fetchAssignedTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        { taskList ->
                            mutableIsLoaderVisible.value = false
                            updatePaidAndPendingAmount(taskList)
                            mutableAssignedTasks.value = taskList.sortedBy { it.status }
                            val isEmpty = taskList.isEmpty()
                            mutableIsEmptyLayoutVisible.value = isEmpty
                            mutableIsErrorLayoutVisible.value = false
                            mutableIsDataLayoutVisible.value = !isEmpty
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            fetchAssignedTasks()
                                        }

                                        override fun onError() {
                                            onFetchAssignedTasksError()
                                        }

                                        override fun reLogin() {
                                            mutableIsLoaderVisible.value = false
                                            mutableShouldReLogin.value = true
                                        }
                                    },
                                    disposables)
                        }
                )
        )
    }

    private fun onFetchAssignedTasksError() {
        mutableIsLoaderVisible.value = false
        mutableAssignedTasks.value = emptyList()
        mutableIsEmptyLayoutVisible.value = false
        mutableIsErrorLayoutVisible.value = true
        mutableIsDataLayoutVisible.value = false
    }

    private fun updatePaidAndPendingAmount(taskList: List<Task>?) {
        if (taskList == null || taskList.isEmpty()) {
            return
        }

        val currencyPaidAmountMapping = LinkedHashMap<String, Double>()
        val currencyPendingAmountMapping = LinkedHashMap<String, Double>()
        for (task in taskList) {
            val gridStatus = GridStatus.getByStatus(task.status)
            if (gridStatus == GridStatus.PAID) {
                val currency = task.currency
                if (currencyPaidAmountMapping.containsKey(currency)) {
                    currencyPaidAmountMapping[currency] = currencyPaidAmountMapping[currency]!! + task.amount
                } else {
                    currencyPaidAmountMapping[currency] = task.amount
                }
            } else if (gridStatus == GridStatus.DONE) {
                val currency = task.currency
                if (currencyPendingAmountMapping.containsKey(currency)) {
                    currencyPendingAmountMapping[currency] = currencyPendingAmountMapping[currency]!! + task.amount
                } else {
                    currencyPendingAmountMapping[currency] = task.amount
                }
            }
        }

        val defaultCurrency = taskList[0].currency
        val paidAmount = currencyPaidAmountMapping[defaultCurrency] ?: 0.0
        mutablePaidAmount.value = currencyUtil.getAmountWithCurrencySymbol(defaultCurrency, paidAmount)
        val pendingAmount = currencyPendingAmountMapping[defaultCurrency] ?: 0.0
        mutablePendingAmount.value = currencyUtil.getAmountWithCurrencySymbol(defaultCurrency, pendingAmount)
    }
}