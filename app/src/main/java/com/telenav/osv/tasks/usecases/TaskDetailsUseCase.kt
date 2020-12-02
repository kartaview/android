package com.telenav.osv.tasks.usecases

import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.network.TasksApi
import io.reactivex.Single

interface TaskDetailsUseCase {
    fun fetchTaskDetails(taskId: String): Single<Task>
    fun submitTaskForReview(taskId: String): Single<Task>
    fun giveUpTask(taskId: String): Single<Task>
    fun submitNoteForTask(taskId: String, note: String): Single<Task>
    fun pickUpTask(taskId: String): Single<Task>
}

class TaskDetailsUseCaseImpl(private val tasksApi: TasksApi): TaskDetailsUseCase {
    override fun fetchTaskDetails(taskId: String): Single<Task> {
        return tasksApi.fetchTaskDetails(taskId)
                .map { gridDetailsResponse ->
                    gridDetailsResponse.result.data
                }
    }

    override fun submitTaskForReview(taskId: String): Single<Task> {
        return tasksApi.submitTaskForReview(taskId)
                .map { gridDetailsResponse ->
                    gridDetailsResponse.result.data
                }
    }

    override fun giveUpTask(taskId: String): Single<Task> {
        return tasksApi.giveUpTask(taskId)
                .map { gridDetailsResponse ->
                    gridDetailsResponse.result.data
                }
    }

    override fun submitNoteForTask(taskId: String, note: String): Single<Task> {
        return tasksApi.submitNoteForTask(taskId, note)
                .map { gridDetailsResponse ->
                    gridDetailsResponse.result.data
                }
    }

    override fun pickUpTask(taskId: String): Single<Task> {
        return tasksApi.pickUpTask(taskId)
                .map { gridDetailsResponse ->
                    gridDetailsResponse.result.data
                }
    }

}