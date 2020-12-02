package com.telenav.osv.tasks.network

import com.telenav.osv.tasks.model.AssignedTasksResponse
import com.telenav.osv.tasks.model.GridDetailsResponse
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.Path

interface TasksApi {

    @GET("grids/me")
    fun fetchAssignedTasks(@Query("campaignType") campaignType: Int): Single<AssignedTasksResponse>

    @GET("grids/{taskId}")
    fun fetchTaskDetails(@Path("taskId") taskId: String): Single<GridDetailsResponse>

    @GET("grids")
    fun fetchTasks(@Query("neLat") neLat: Double, @Query("neLng") neLng: Double, @Query("swLat") swLat: Double, @Query("swLng") swLng: Double, @Query("campaignType") campaignType: Int, @Query("isActive") isActive: Int): Single<AssignedTasksResponse>

    @PUT("grids/{taskId}/review")
    fun submitTaskForReview(@Path("taskId") taskId: String): Single<GridDetailsResponse>

    @PUT("grids/{taskId}/giveup")
    fun giveUpTask(@Path("taskId") taskId: String): Single<GridDetailsResponse>

    @FormUrlEncoded
    @POST("grids/{taskId}/note")
    fun submitNoteForTask(
            @Path("taskId") taskId: String,
            @Field("note") note: String
    ): Single<GridDetailsResponse>

    @PUT("grids/{taskId}/pickup")
    fun pickUpTask(@Path("taskId") taskId: String): Single<GridDetailsResponse>
}