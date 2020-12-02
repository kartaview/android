package com.telenav.osv.report.network

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.telenav.osv.report.model.ClosedRoadResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface ReportIssueApi {
    @POST("closedRoad")
    fun reportClosedRoad(
            @Body loginRequest: ClosedRoadRequest
    ): Single<ClosedRoadResponse>
}

internal data class ClosedRoadRequest(
        @SerializedName("coordinates") @Expose val coordinates: List<Double>,
        @SerializedName("note") @Expose val note: String? = null,
        @SerializedName("type") @Expose val type: Int
)