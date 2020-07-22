package no.nav.arbeidsplassen.analytics.ad.dto

import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse
import com.google.api.services.analyticsreporting.v4.model.ReportRow

data class AdDto(
    var pageViews: Int = 0,
    //leaving the actual calculation to frontend
    var averageTimeOnPage: List<Double> = listOf(),
    var referrals: List<String> = listOf(),
    var viewsPerReferral: List<Int> = listOf(),
    var dates: List<String> = listOf(),
    var viewsPerDate: List<Int> = listOf()
    /*
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)

//kanskje ha annet sted
abstract class DimensionEntity {
    var rows: List<ReportRow>
    var nextPageToken: String?

    constructor(reportsResponse: GetReportsResponse) {
        rows = reportsResponse.getReport().data.rows
        nextPageToken = reportsResponse.getReport().nextPageToken
    }

    abstract fun toAdDto(row: ReportRow): AdDto

    fun setGetReportsResponse(reportsResponse: GetReportsResponse) {
        rows = reportsResponse.getReport().data.rows
        nextPageToken = reportsResponse.getReport().nextPageToken
    }
}

class ReferralEntity(reportsResponse: GetReportsResponse) : DimensionEntity(reportsResponse) {

    //ikke sikker på return eller dto-variabel
    override fun toAdDto(row: ReportRow): AdDto {
        return AdDto(
            pageViews = row.getMetric().first().toInt(),
            averageTimeOnPage = listOf(row.getMetric().last().toDouble()),
            referrals = listOf(row.dimensions.last()),
            viewsPerReferral = listOf(row.getMetric().first().toInt())
        )
    }
}

class DateEntity(reportsResponse: GetReportsResponse) : DimensionEntity(reportsResponse) {

    //ikke sikker på return eller dto-variabel
    override fun toAdDto(row: ReportRow): AdDto {
        return AdDto(
            dates = listOf(row.dimensions.last()),
            viewsPerDate = listOf(row.getMetric().first().toInt())
        )
    }
}

//this is implying we only send one request
private fun GetReportsResponse.getReport() = reports.first()

private fun ReportRow.getMetric() = metrics.first().getValues()