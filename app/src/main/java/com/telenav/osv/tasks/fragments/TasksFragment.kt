package com.telenav.osv.tasks.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
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
import com.telenav.osv.databinding.FragmentTasksBinding
import com.telenav.osv.jarvis.login.utils.LoginUtils
import com.telenav.osv.tasks.adapter.TasksAdapter
import com.telenav.osv.tasks.viewmodels.TasksViewModel
import com.telenav.osv.utils.LogUtils
import com.telenav.osv.utils.NetworkUtils
import com.telenav.osv.utils.recyclerview.DividerItemDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

/**
 * This fragment helps in displaying assigned tasks for user.
 * Also it displays total amount paid and pending for a user.
 */
class TasksFragment : KVBaseFragment() {

    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var fragmentTasksBinding: FragmentTasksBinding
    private lateinit var appPrefs: ApplicationPreferences
    private lateinit var tasksFragmentListener: TasksFragmentListener
    private lateinit var tasksViewModel: TasksViewModel
    private var sessionExpireDialog: KVDialog? = null
    private val disposables: CompositeDisposable = CompositeDisposable()

    init {
        TAG = TasksFragment::class.java.simpleName
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TasksFragmentListener) {
            tasksFragmentListener = context
            activity?.let { appPrefs = (it.application as KVApplication).appPrefs }
            tasksViewModel = ViewModelProviders.of(this, Injection.provideTasksViewModelFactory(
                    Injection.provideFetchAssignedTasksUseCase(
                            Injection.provideTasksApi(true, appPrefs)),
                    Injection.provideCurrencyUtil(),
                    Injection.provideGenericJarvisApiErrorHandler(context, appPrefs)))
                    .get(TasksViewModel::class.java)
        } else {
            throw RuntimeException("$context must implement TasksFragmentListener.")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        fragmentTasksBinding = FragmentTasksBinding.inflate(inflater, container, false).apply {
            tasksVm = tasksViewModel
            lifecycleOwner = this@TasksFragment
        }
        return fragmentTasksBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTasksRecyclerView(view)
        tasksViewModel.fetchAssignedTasks()
        tasksViewModel.assignedTasks.observe(viewLifecycleOwner, Observer { assignedTasks ->
            tasksAdapter.updateData(assignedTasks)
        })
        tasksViewModel.isLoaderVisible.observe(viewLifecycleOwner, Observer { isLoaderVisible ->
            tasksAdapter.setIsClickable(!isLoaderVisible)
        })
        tasksViewModel.shouldReLogin.observe(viewLifecycleOwner, Observer { shouldReLogin ->
            if (shouldReLogin) {
                context?.let { showSessionExpiredDialog(it) }
            }
        })
        fragmentTasksBinding.tvExploreNearby.setOnClickListener { activity?.onBackPressed() }
    }

    override fun getToolbarSettings(kvToolbar: KVToolbar?): ToolbarSettings? {
        return ToolbarSettings.Builder()
                .setTitle(getString(R.string.tasks_toolbar_title))
                .setNavigationIcon(R.drawable.vector_back_arrow)
                .build()
    }

    private fun setTasksRecyclerView(view: View) {
        fragmentTasksBinding.rvTasks.let {
            tasksAdapter = TasksAdapter(Injection.provideCurrencyUtil())
            tasksAdapter.setOnTaskItemClick { taskId ->
                tasksFragmentListener.onTaskItemClick(taskId)
            }
            tasksAdapter.setHasStableIds(true)
            it.adapter = tasksAdapter
            it.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL, false)
            it.addItemDecoration(DividerItemDecoration(view.context, null, true, false))
        }
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

    override fun onStart() {
        super.onStart()
        val context = activity?.applicationContext
        if (context != null) {
            disposables.add(NetworkUtils.isInternetAvailableStream(context)
                    .observable()
                    .distinctUntilChanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { isInternetAvailable ->
                                val tvInternetConnectivityMessage = fragmentTasksBinding.tvInternetConnectivityMessage
                                if (isInternetAvailable as Boolean) {
                                    tvInternetConnectivityMessage.visibility = View.GONE
                                } else {
                                    tvInternetConnectivityMessage.text = getString(R.string.no_internet_connection_label)
                                    tvInternetConnectivityMessage.background = AppCompatResources.getDrawable(context, R.color.default_red)
                                    tvInternetConnectivityMessage.visibility = View.VISIBLE
                                }
                            },
                            { throwable ->
                                LogUtils.logDebug(TAG, String.format("Status: error on internet. Message: %s.", throwable.message))
                            }))
        }

    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun handleBackPressed(): Boolean {
        return false
    }

    companion object {
        lateinit var TAG: String

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment TasksFragment.
         */
        @JvmStatic
        fun newInstance() = TasksFragment()
    }

    interface TasksFragmentListener {
        fun onTaskItemClick(taskId: String)
    }
}