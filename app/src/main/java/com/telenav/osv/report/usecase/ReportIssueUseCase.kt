package com.telenav.osv.report.usecase

import com.telenav.osv.report.model.ClosedRoadResponse
import com.telenav.osv.report.network.ClosedRoadRequest
import com.telenav.osv.report.network.ReportIssueApi
import io.reactivex.Single

internal interface ReportIssueUseCase {
    fun reportClosedRoad(closedRoadRequest: ClosedRoadRequest): Single<ClosedRoadResponse>
}

internal class ReportIssueUseCaseImpl(
        private val reportIssueApi: ReportIssueApi
): ReportIssueUseCase {
    override fun reportClosedRoad(closedRoadRequest: ClosedRoadRequest): Single<ClosedRoadResponse> {
        return reportIssueApi.reportClosedRoad(closedRoadRequest)
    }

}

