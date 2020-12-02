package com.telenav.osv.tasks.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.telenav.osv.R
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.KVApplication
import com.telenav.osv.common.Injection
import com.telenav.osv.common.dialog.KVDialog
import com.telenav.osv.common.model.base.KVBaseFragment
import com.telenav.osv.common.toolbar.KVToolbar
import com.telenav.osv.common.toolbar.ToolbarSettings
import com.telenav.osv.databinding.FragmentTaskDetailBinding
import com.telenav.osv.jarvis.login.utils.LoginUtils
import com.telenav.osv.tasks.activity.KEY_TASK_ID
import com.telenav.osv.tasks.adapter.OperationLogsAdapter
import com.telenav.osv.tasks.model.GenericErrorResponse
import com.telenav.osv.tasks.model.GridStatus
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.model.TaskError
import com.telenav.osv.tasks.utils.getTaskStatusBackground
import com.telenav.osv.tasks.utils.getTaskStatusColor
import com.telenav.osv.tasks.viewmodels.TaskDetailsViewModel
import com.telenav.osv.utils.KeyboardUtils
import com.telenav.osv.utils.UiUtils.setData
import com.telenav.osv.utils.recyclerview.DividerItemDecoration

/**
 * This fragment helps in displaying details of a Task.
 * Also it provides functionality to submit, give up or add notes for an assigned task or assign a task to a user
 */
class TaskDetailsFragment : KVBaseFragment() {

    private lateinit var operationLogsAdapter: OperationLogsAdapter
    private lateinit var fragmentTaskDetailBinding: FragmentTaskDetailBinding
    private lateinit var taskDetailsViewModel: TaskDetailsViewModel
    private lateinit var appPrefs: ApplicationPreferences
    private var taskId: String? = null
    private var submitTaskDialog: KVDialog? = null
    private var giveUpTaskDialog: KVDialog? = null
    private var submitTaskFailureDialog: KVDialog? = null
    private var giveUpTaskFailureDialog: KVDialog? = null
    private var submitNoteFailureDialog: KVDialog? = null
    private var pickUpTaskFailureDialog: KVDialog? = null
    private var pickUpLimitFailureDialog: KVDialog? = null
    private val distanceFormat = "%.2f"
    private var sessionExpireDialog: KVDialog? = null

    init {
        TAG = TaskDetailsFragment::class.java.simpleName
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.getString(KEY_TASK_ID).let { taskId = it }
        if (taskId.isNullOrEmpty()) {
            throw RuntimeException("Task id cannot be empty for Task Details Fragment")
        }
        activity?.let {
            appPrefs = (it.application as KVApplication).appPrefs
        }
        taskDetailsViewModel = ViewModelProviders.of(this, Injection.provideTaskDetailsViewModelFactory(
                Injection.provideTaskDetailsUseCase(Injection.provideTasksApi(true, appPrefs)),
                context,
                taskId,
                Injection.provideCurrencyUtil(),
                Injection.provideGenericJarvisApiErrorHandler(context, appPrefs)))
                .get(TaskDetailsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentTaskDetailBinding = FragmentTaskDetailBinding.inflate(inflater, container, false).apply {
            taskDetailsVm = taskDetailsViewModel
            lifecycleOwner = this@TaskDetailsFragment
        }
        return fragmentTaskDetailBinding.root
    }

    override fun handleBackPressed(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentTaskDetailBinding.ivBack.setOnClickListener { finish() }
        setOperationLogsRecyclerView(view)
        taskDetailsViewModel.fetchTaskDetails()
        val context = requireContext()
        taskDetailsViewModel.task.observe(viewLifecycleOwner, Observer { task ->
            updateUIForTask(context, task)
        })
        fragmentTaskDetailBinding.tvSubmit.setOnClickListener { showSubmitTaskDialog(context) }
        fragmentTaskDetailBinding.tvGiveUp.setOnClickListener { showGiveUpTaskDialog(context) }
        fragmentTaskDetailBinding.tvSubmitNote.setOnClickListener {
            KeyboardUtils.hideKeyboard(activity)
            taskDetailsViewModel.submitNoteForTask()
        }
        taskDetailsViewModel.isSubmitTaskSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                onSubmitTaskSuccess(context)
            } else {
                showSubmitTaskFailureDialog(context)
            }
        })
        taskDetailsViewModel.isGiveUpTaskSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                onGiveUpTaskSuccess(context)
            } else {
                showGiveUpTaskFailureDialog(context)
            }
        })
        taskDetailsViewModel.isSubmitNoteSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                onSubmitNoteSuccess(context)
            } else {
                showSubmitNoteFailureDialog(context)
            }
        })
        taskDetailsViewModel.isPickUpTaskSuccess.observe(viewLifecycleOwner, Observer { isSuccess ->
            if (isSuccess) {
                onPickUpTaskSuccess(context)
            } else {
                showPickUpTaskFailureDialog(context)
            }
        })
        taskDetailsViewModel.pickUpTaskError.observe(viewLifecycleOwner, Observer { pickUpTaskError ->
            handlePickUpTaskError(context, pickUpTaskError)
        })
        taskDetailsViewModel.shouldReLogin.observe(viewLifecycleOwner, Observer { shouldReLogin ->
            if (shouldReLogin) {
                showSessionExpiredDialog(context)
            }
        })
    }

    override fun getToolbarSettings(kvToolbar: KVToolbar?): ToolbarSettings? {
        return null
    }

    /**
     * This method sets up recycler view for operation logs
     */
    private fun setOperationLogsRecyclerView(view: View) {
        fragmentTaskDetailBinding.rvOperationLogs.let { recyclerView ->
            operationLogsAdapter = OperationLogsAdapter(Injection.provideCurrencyUtil())
            operationLogsAdapter.setHasStableIds(true)
            recyclerView.adapter = operationLogsAdapter
            recyclerView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
            view.context.getDrawable(R.drawable.operation_log_divider)?.let {
                recyclerView.addItemDecoration(DividerItemDecoration(it, true, showLastDivider = false))
            }
            recyclerView.isNestedScrollingEnabled = false
        }
    }

    /**
     * This method updates UI for a task
     */
    private fun updateUIForTask(context: Context, task: Task) {
        var targetText: String? = null
        task.ukm?.let { targetText = String.format(context.getString(R.string.task_target), String.format(distanceFormat, it)) }
        setData(targetText, fragmentTaskDetailBinding.tvTarget)
        updateUIForTaskStatus(context, GridStatus.getByStatus(task.status))
        operationLogsAdapter.setCurrency(task.currency)
        task.operationLogs?.let { operationLogsAdapter.updateOperationLogs(it) }
    }

    /**
     * This method updates UI for a task based on it's Grid Status
     */
    private fun updateUIForTaskStatus(context: Context, gridStatus: GridStatus?) {
        if (gridStatus == null) {
            return
        }
        val taskColor = ContextCompat.getColor(context, getTaskStatusColor(gridStatus))
        fragmentTaskDetailBinding.clToolbar.setBackgroundColor(taskColor)
        fragmentTaskDetailBinding.tvTitle.background = context.getDrawable(getTaskStatusBackground(gridStatus))
        fragmentTaskDetailBinding.tvAmount.setTextColor(taskColor)
        setStatusTitleAndMessage(context, gridStatus)
    }

    /**
     * This method updates status and title message for a task based on it's Grid Status
     */
    private fun setStatusTitleAndMessage(context: Context, gridStatus: GridStatus) {
        var statusTitle: String? = null
        var statusMessage: String? = null
        when {
            GridStatus.MAP_OPS_QC == gridStatus -> {
                statusTitle = context.getString(R.string.grid_status_map_ops_qc_title)
                statusMessage = context.getString(R.string.grid_status_map_ops_qc_message)
            }
            GridStatus.DONE == gridStatus -> {
                statusTitle = context.getString(R.string.grid_status_done_title)
                statusMessage = context.getString(R.string.grid_status_done_message)
            }
            GridStatus.PAID == gridStatus -> {
                statusTitle = context.getString(R.string.grid_status_paid_title)
                statusMessage = context.getString(R.string.grid_status_paid_message)
            }
        }
        setData(statusTitle, fragmentTaskDetailBinding.tvStatus)
        setData(statusMessage, fragmentTaskDetailBinding.tvStatusMessage)
    }

    /**
     * This method displays alert dialog before submitting a task
     */
    private fun showSubmitTaskDialog(context: Context) {
        if (submitTaskDialog == null) {
            submitTaskDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.submit_task_dialog_title)
                    .setInfoResId(R.string.submit_task_dialog_message)
                    .setPositiveButton(R.string.submit_task_positive_button_text) {
                        taskDetailsViewModel.submitTaskForReview()
                        submitTaskDialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_label) {
                        submitTaskDialog?.dismiss()
                    }
                    .setNegativeButtonTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        submitTaskDialog?.show()
    }

    /**
     * This method displays toast message on successful submission of task
     */
    private fun onSubmitTaskSuccess(context: Context) {
        Toast.makeText(context, String.format(context.getString(
                R.string.submit_task_success_toast_message), taskId), Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * This method displays alert dialog on failure of submitting task
     */
    private fun showSubmitTaskFailureDialog(context: Context) {
        if (submitTaskFailureDialog == null) {
            submitTaskFailureDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.oops)
                    .setInfoResId(R.string.submit_task_failure_dialog_message)
                    .setPositiveButton(R.string.dialog_try_again) {
                        taskDetailsViewModel.submitTaskForReview()
                        submitTaskFailureDialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_label) {
                        submitTaskFailureDialog?.dismiss()
                    }
                    .setNegativeButtonTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        submitTaskFailureDialog?.show()
    }

    /**
     * This method displays alert dialog before giving up a task
     */
    private fun showGiveUpTaskDialog(context: Context) {
        if (giveUpTaskDialog == null) {
            giveUpTaskDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.give_up_task_dialog_title)
                    .setInfoResId(R.string.give_up_task_dialog_message)
                    .setPositiveButton(R.string.give_up_task_positive_button_text) {
                        taskDetailsViewModel.giveUpTask()
                        giveUpTaskDialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_label) {
                        giveUpTaskDialog?.dismiss()
                    }
                    .setNegativeButtonTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        giveUpTaskDialog?.show()
    }

    /**
     * This method displays toast message on successfully giving up a task
     */
    private fun onGiveUpTaskSuccess(context: Context) {
        Toast.makeText(context, String.format(context.getString(
                R.string.give_up_task_success_toast_message), taskId), Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * This method displays alert dialog on failure of giving up a task
     */
    private fun showGiveUpTaskFailureDialog(context: Context) {
        if (giveUpTaskFailureDialog == null) {
            giveUpTaskFailureDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.oops)
                    .setInfoResId(R.string.give_up_task_failure_dialog_message)
                    .setPositiveButton(R.string.dialog_try_again) {
                        taskDetailsViewModel.giveUpTask()
                        giveUpTaskFailureDialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_label) {
                        giveUpTaskFailureDialog?.dismiss()
                    }
                    .setNegativeButtonTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        giveUpTaskFailureDialog?.show()
    }

    /**
     * This method displays toast message on successful submission of note
     */
    private fun onSubmitNoteSuccess(context: Context) {
        Toast.makeText(context, String.format(context.getString(
                R.string.submit_note_success_toast_message), taskId), Toast.LENGTH_SHORT).show()
    }

    /**
     * This method displays alert dialog on failure of submitting note
     */
    private fun showSubmitNoteFailureDialog(context: Context) {
        if (submitNoteFailureDialog == null) {
            submitNoteFailureDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.oops)
                    .setInfoResId(R.string.submit_note_failure_dialog_message)
                    .setPositiveButton(R.string.dialog_try_again) {
                        taskDetailsViewModel.submitNoteForTask()
                        submitNoteFailureDialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_label) {
                        submitNoteFailureDialog?.dismiss()
                    }
                    .setNegativeButtonTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        submitNoteFailureDialog?.show()
    }

    /**
     * This method displays toast message on successfully assigning a task
     */
    private fun onPickUpTaskSuccess(context: Context) {
        Toast.makeText(context, String.format(context.getString(
                R.string.pick_up_task_success_toast_message), taskId), Toast.LENGTH_SHORT).show()
        finish()
    }

    /**
     * This method displays alert dialog on failure of assigning a task
     */
    private fun showPickUpTaskFailureDialog(context: Context) {
        if (pickUpTaskFailureDialog == null) {
            pickUpTaskFailureDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.oops)
                    .setInfoResId(R.string.pick_up_task_failure_dialog_message)
                    .setPositiveButton(R.string.dialog_try_again) {
                        taskDetailsViewModel.pickUpTask()
                        pickUpTaskFailureDialog?.dismiss()
                    }
                    .setNegativeButton(R.string.cancel_label) {
                        pickUpTaskFailureDialog?.dismiss()
                    }
                    .setNegativeButtonTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        pickUpTaskFailureDialog?.show()
    }

    private fun handlePickUpTaskError(context: Context, pickUpTaskError: GenericErrorResponse) {
        val errorCode = pickUpTaskError.result.errorCode
        if (!errorCode.isNullOrEmpty()) {
            when (TaskError.getByError(errorCode)) {
                TaskError.OUT_OF_PICKUP_LIMIT -> showPickUpLimitFailureDialog(context)
                TaskError.UNKNOWN -> showPickUpTaskFailureDialog(context)
            }
        } else {
            showPickUpTaskFailureDialog(context)
        }
    }

    /**
     * This method displays alert dialog on failure of assigning a task when task limit is reached
     */
    private fun showPickUpLimitFailureDialog(context: Context) {
        if (pickUpLimitFailureDialog == null) {
            pickUpLimitFailureDialog = KVDialog.Builder(context)
                    .setTitleResId(R.string.pick_up_limit_reached_dialog_title)
                    .setInfoResId(R.string.pick_up_limit_reached_dialog_message)
                    .setPositiveButton(R.string.okay_label) {
                        pickUpLimitFailureDialog?.dismiss()
                    }
                    .setTitleTextColor(R.color.color_EB3030)
                    .setIconLayoutVisibility(false)
                    .build()
        }
        pickUpLimitFailureDialog?.show()
    }

    /**
     * This method displays alert dialog for expired session
     */
    private fun showSessionExpiredDialog(context: Context) {
        if (sessionExpireDialog == null) {
            sessionExpireDialog = LoginUtils.getSessionExpiredDialog(context)
        }
        sessionExpireDialog?.show()
    }

    private fun finish() {
        activity?.onBackPressed()
    }

    companion object {
        lateinit var TAG: String

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment TaskDetailFragment.
         */
        @JvmStatic
        fun newInstance(taskId: String) = TaskDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_TASK_ID, taskId)
            }
        }
    }
}