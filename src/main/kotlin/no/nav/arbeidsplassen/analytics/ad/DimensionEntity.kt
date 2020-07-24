package no.nav.arbeidsplassen.analytics.ad

import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse
import com.google.api.services.analyticsreporting.v4.model.ReportRow
import no.nav.arbeidsplassen.analytics.ad.dto.AdDto

abstract class DimensionEntity(private val googleAnalyticsQuery: GoogleAnalyticsQuery) {
    var rows = listOf<ReportRow>()
    private var nextPageToken: String? = ""
    abstract val metricExpressions: List<String>
    abstract val dimensionNames: List<String>
    private var startDate = "1DaysAgo"
    private var endDate = "today"

    abstract fun toAdDto(row: ReportRow): AdDto

    fun setDateRange(startDate: String, endDate: String) {
        this.startDate = startDate
        this.endDate = endDate
    }

    fun nextPage(): Boolean {
        return nextPageToken?.let {
            val reportsResponse =
                googleAnalyticsQuery.getReportsResponse(
                    metricExpressions = metricExpressions,
                    dimensionNames = dimensionNames,
                    pageToken = nextPageToken,
                    startDate = startDate,
                    endDate = endDate
                )

            rows = reportsResponse.getReport().data.rows
            nextPageToken = reportsResponse.getReport().nextPageToken
            true
        } ?: false
    }

    //this is implying we only send one request
    private fun GetReportsResponse.getReport() = reports.first()


}

class ReferralEntity(googleAnalyticsQuery: GoogleAnalyticsQuery) : DimensionEntity(googleAnalyticsQuery) {
    override val metricExpressions = listOf("ga:pageviews", "ga:avgTimeOnPage")
    override val dimensionNames = listOf("ga:pagePath", "ga:fullReferrer")

    override fun toAdDto(row: ReportRow): AdDto {
        return AdDto(
            pageViews = row.getMetric().first().toInt(),
            averageTimeOnPage = listOf(row.getMetric().last().toDouble()),
            referrals = listOf(row.dimensions.last()),
            viewsPerReferral = listOf(row.getMetric().first().toInt())
        )
    }
}

class DateEntity(googleAnalyticsQuery: GoogleAnalyticsQuery) : DimensionEntity(googleAnalyticsQuery) {
    override val metricExpressions = listOf("ga:pageviews")
    override val dimensionNames = listOf("ga:pagePath", "ga:date")

    override fun toAdDto(row: ReportRow): AdDto {
        return AdDto(
            dates = listOf(row.dimensions.last()),
            viewsPerDate = listOf(row.getMetric().first().toInt())
        )
    }
}

private fun ReportRow.getMetric() = metrics.first().getValues()

