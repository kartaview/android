package com.telenav.osv.tasks.usecases

import com.telenav.osv.tasks.model.CampaignType
import com.telenav.osv.tasks.model.Task
import com.telenav.osv.tasks.network.TasksApi
import io.reactivex.Single

/**
 * Interface representing the use case for fetching tasks. This is to be used for getting both the tasks related to the logged in user via [fetchAssignedTasks] or the ones in a bounding box via [fetchTasks]
 */
interface FetchAssignedTasksUseCase {

    fun fetchAssignedTasks(): Single<List<Task>>

    fun fetchTasks(neLat: Double, neLon: Double, swLat: Double, swLon: Double): Single<List<Task>>
}

/**
 * Implementation for the [FetchAssignedTasksUseCase] which will use internally the given [tasksApi] representing the Jarvis API.
 */
class FetchAssignedTasksUseCaseImpl(private val tasksApi: TasksApi) : FetchAssignedTasksUseCase {

    override fun fetchAssignedTasks(): Single<List<Task>> {
        //the campaign type is not exposes since due to current requirements jarvis will only support crowd sourced campaigns, expose the value if required
        return tasksApi.fetchAssignedTasks(CampaignType.CAMPAIGN_TYPE_CROWD_SOURCED.value)
                .map { assignedTasksResponse ->
                    assignedTasksResponse.result.data
                }
    }

    override fun fetchTasks(neLat: Double, neLon: Double, swLat: Double, swLon: Double): Single<List<Task>> {
        //the campaign type is not exposes since due to current requirements jarvis will only support crowd sourced campaigns, expose the value if required
        return tasksApi.fetchTasks(neLat, neLon, swLat, swLon, CampaignType.CAMPAIGN_TYPE_CROWD_SOURCED.value, CAMPAIGN_IS_ACTIVE)
                .map { assignedTasksResponse ->
                    assignedTasksResponse.result.data
                }
    }

    private companion object {
        const val CAMPAIGN_IS_ACTIVE = 1
    }
}