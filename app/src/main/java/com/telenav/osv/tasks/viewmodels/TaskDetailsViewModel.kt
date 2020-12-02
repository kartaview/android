package com.telenav.osv.tasks.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.telenav.osv.data.user.datasource.UserDataSource
import com.telenav.osv.data.user.model.User
import com.telenav.osv.network.GenericJarvisApiErrorHandler
import com.telenav.osv.network.GenericJarvisApiErrorHandlerListener
import com.telenav.osv.tasks.model.GenericErrorResponse
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.usecases.TaskDetailsUseCase
import com.telenav.osv.tasks.utils.CurrencyUtil
import com.telenav.osv.utils.LogUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.HttpURLConnection

internal class TaskDetailsViewModel(
        private val taskDetailsUseCase: TaskDetailsUseCase,
        private val userDataSource: UserDataSource,
        private val taskId: String?,
        private val currencyUtil: CurrencyUtil,
        private val genericJarvisApiErrorHandler: GenericJarvisApiErrorHandler
) : ViewModel() {

    private val TAG = TaskDetailsViewModel::class.java.simpleName
    private val disposables: CompositeDisposable = CompositeDisposable()
    private val mutableNoteText: MutableLiveData<String> = MutableLiveData()
    private val mutableIsSubmitNoteVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsLoaderVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableTask: MutableLiveData<Task> = MutableLiveData()
    private val mutableIsTaskDataVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsTaskAssigned: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableTaskAmount: MutableLiveData<String> = MutableLiveData()
    private val mutableIsErrorLayoutVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsHistoryVisible: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsTaskInProgress: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsSubmitTaskSuccess: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsGiveUpTaskSuccess: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsSubmitNoteSuccess: MutableLiveData<Boolean> = MutableLiveData()
    private val mutableIsPickUpTaskSuccess: MutableLiveData<Boolean> = MutableLiveData()
    private val mutablePickUpTaskError: MutableLiveData<GenericErrorResponse> = MutableLiveData()
    private val mutableShouldReLogin: MutableLiveData<Boolean> = MutableLiveData()
    val noteText: LiveData<String> = mutableNoteText
    val isSubmitNoteVisible: LiveData<Boolean> = mutableIsSubmitNoteVisible
    val isLoaderVisible: LiveData<Boolean> = mutableIsLoaderVisible
    val task: LiveData<Task> = mutableTask
    val isTaskDataVisible: LiveData<Boolean> = mutableIsTaskDataVisible
    val isTaskAssigned: LiveData<Boolean> = mutableIsTaskAssigned
    val taskAmount: LiveData<String> = mutableTaskAmount
    val isErrorLayoutVisible: LiveData<Boolean> = mutableIsErrorLayoutVisible
    val isHistoryVisible: LiveData<Boolean> = mutableIsHistoryVisible
    val isTaskInProgress: LiveData<Boolean> = mutableIsTaskInProgress
    val isSubmitTaskSuccess: LiveData<Boolean> = mutableIsSubmitTaskSuccess
    val isGiveUpTaskSuccess: LiveData<Boolean> = mutableIsGiveUpTaskSuccess
    val isSubmitNoteSuccess: LiveData<Boolean> = mutableIsSubmitNoteSuccess
    val isPickUpTaskSuccess: LiveData<Boolean> = mutableIsPickUpTaskSuccess
    val pickUpTaskError: LiveData<GenericErrorResponse> = mutablePickUpTaskError
    val shouldReLogin: LiveData<Boolean> = mutableShouldReLogin

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun onNoteTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        val updatedText = charSequence.toString()
        mutableNoteText.value = updatedText
        mutableIsSubmitNoteVisible.value = updatedText.trim().isNotEmpty()
    }

    fun fetchTaskDetails() {
        if (taskId.isNullOrEmpty()) return
        disposables.add(taskDetailsUseCase.fetchTaskDetails(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        { task ->
                            disposables.add(userDataSource.user
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            { user ->
                                                onFetchTaskDetailsSuccess(task, user)
                                            },
                                            {
                                                onReLogin()
                                            },
                                            {
                                                onReLogin()
                                            }
                                    ))
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            fetchTaskDetails()
                                        }

                                        override fun onError() {
                                            onFetchTaskDetailsError()
                                        }

                                        override fun reLogin() {
                                            onReLogin()
                                        }

                                    },
                                    disposables)
                        }
                ))
    }

    private fun onFetchTaskDetailsSuccess(task: Task, user: User) {
        mutableIsLoaderVisible.value = false
        mutableTask.value = task
        mutableTaskAmount.value = currencyUtil.getAmountWithCurrencySymbol(task.currency, task.amount)
        mutableIsTaskDataVisible.value = true
        mutableIsErrorLayoutVisible.value = false
        mutableIsHistoryVisible.value = !task.operationLogs.isNullOrEmpty()
        mutableIsTaskInProgress.value = GridStatus.RECORDING == GridStatus.getByStatus(task.status)
        mutableIsTaskAssigned.value = user.jarvisUserId == task.assignedUserId
    }

    private fun onFetchTaskDetailsError() {
        mutableIsLoaderVisible.value = false
        mutableIsTaskDataVisible.value = false
        mutableIsErrorLayoutVisible.value = true
    }

    private fun onReLogin() {
        mutableIsLoaderVisible.value = false
        mutableShouldReLogin.value = true
    }

    fun submitTaskForReview() {
        if (taskId.isNullOrEmpty()) return
        disposables.add(taskDetailsUseCase.submitTaskForReview(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        {
                            mutableIsLoaderVisible.value = false
                            mutableIsSubmitTaskSuccess.value = true
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            submitTaskForReview()
                                        }

                                        override fun onError() {
                                            onSubmitTaskForReviewError()
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

    private fun onSubmitTaskForReviewError() {
        mutableIsLoaderVisible.value = false
        mutableIsSubmitTaskSuccess.value = false
    }

    fun giveUpTask() {
        if (taskId.isNullOrEmpty()) return
        disposables.add(taskDetailsUseCase.giveUpTask(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        {
                            mutableIsLoaderVisible.value = false
                            mutableIsGiveUpTaskSuccess.value = true
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            giveUpTask()
                                        }

                                        override fun onError() {
                                            onGiveUpTaskError()
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

    private fun onGiveUpTaskError() {
        mutableIsLoaderVisible.value = false
        mutableIsGiveUpTaskSuccess.value = false
    }

    fun submitNoteForTask() {
        val note = noteText.value?.trim()
        if (taskId.isNullOrEmpty() || note.isNullOrEmpty()) return
        disposables.add(taskDetailsUseCase.submitNoteForTask(taskId, note)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        {
                            mutableIsLoaderVisible.value = false
                            mutableTask.value = it
                            mutableIsHistoryVisible.value = !it.operationLogs.isNullOrEmpty()
                            mutableNoteText.value = ""
                            mutableIsSubmitNoteSuccess.value = true
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            submitNoteForTask()
                                        }

                                        override fun onError() {
                                            onSubmitNoteForTaskError()
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

    private fun onSubmitNoteForTaskError() {
        mutableIsLoaderVisible.value = false
        mutableIsSubmitNoteSuccess.value = false
    }

    fun pickUpTask() {
        if (taskId.isNullOrEmpty()) return
        disposables.add(taskDetailsUseCase.pickUpTask(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { mutableIsLoaderVisible.value = true }
                .subscribe(
                        {
                            mutableIsLoaderVisible.value = false
                            mutableIsPickUpTaskSuccess.value = true
                        },
                        { throwable ->
                            genericJarvisApiErrorHandler.onError(
                                    throwable,
                                    object : GenericJarvisApiErrorHandlerListener {
                                        override fun onRefreshTokenSuccess() {
                                            pickUpTask()
                                        }

                                        override fun onError() {
                                            onPickUpTaskError(throwable)
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

    private fun onPickUpTaskError(throwable: Throwable) {
        mutableIsLoaderVisible.value = false
        var pickUpTaskError: GenericErrorResponse? = null
        when (throwable) {
            is HttpException -> {
                when (throwable.code()) {
                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        val response =  throwable.response()?.errorBody()?.string()
                        if (!response.isNullOrEmpty()) {
                            try {
                                pickUpTaskError = Gson().fromJson(response, GenericErrorResponse::class.java)
                            } catch (e: JsonSyntaxException) {
                                LogUtils.logDebug(TAG, e.localizedMessage)
                            }
                        }
                    }
                }
            }
        }
        if (pickUpTaskError != null) {
            mutablePickUpTaskError.value = pickUpTaskError
        } else {
            mutableIsPickUpTaskSuccess.value = false
        }
    }

}