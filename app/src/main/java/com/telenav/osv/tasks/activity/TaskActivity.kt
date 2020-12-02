package com.telenav.osv.tasks.activity

import android.os.Bundle
import com.telenav.osv.R
import com.telenav.osv.common.model.base.KVBaseActivity
import com.telenav.osv.tasks.fragments.TaskDetailsFragment
import com.telenav.osv.tasks.fragments.TasksFragment
import com.telenav.osv.utils.ActivityUtils

const val KEY_TASK_ID = "TASK_ID"

class TaskActivity : KVBaseActivity(), TasksFragment.TasksFragmentListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.extras?.getString(KEY_TASK_ID)
        if (taskId.isNullOrEmpty()) {
            openTasksListScreen()
        } else {
            openTaskDetailScreen(taskId)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_task
    }

    private fun openTasksListScreen() {
        ActivityUtils.replaceFragment(supportFragmentManager,
                TasksFragment.newInstance(),
                R.id.fl_container,
                true,
                TasksFragment.TAG)
    }

    private fun openTaskDetailScreen(taskId: String) {
        ActivityUtils.replaceFragment(supportFragmentManager,
                TaskDetailsFragment.newInstance(taskId),
                R.id.fl_container,
                true,
                TaskDetailsFragment.TAG)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }


    override fun resolveLocationProblem() {}

    override fun onTaskItemClick(taskId: String) {
        openTaskDetailScreen(taskId)
    }
}