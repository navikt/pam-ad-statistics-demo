package no.nav.arbeidsplassen.analytics

import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse
import com.google.api.services.analyticsreporting.v4.model.ReportRow
import no.nav.arbeidsplassen.analytics.ad.dto.AdStatisticsDto
import no.nav.arbeidsplassen.analytics.candidate.dto.CandidateStatisticsDto
import no.nav.arbeidsplassen.analytics.filter.dto.CandidateFilterStatisticsDto
import no.nav.arbeidsplassen.analytics.googleapi.GoogleAnalyticsQuery

abstract class DimensionEntity<T : StatisticsDto<T>>(private val googleAnalyticsQuery: GoogleAnalyticsQuery) {
    var rows = listOf<ReportRow>()
    var nextPageToken: String? = "init"
    abstract val metricExpressions: List<String>
    abstract val dimensionNames: List<String>
    abstract val filterExpression: String
    private var startDate = "1DaysAgo"
    private var endDate = "today"

    abstract fun toStatisticsDto(row: ReportRow): StatisticsDto<T>

    abstract fun getKey(row: ReportRow): List<String>

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
                    filterExpression = filterExpression,
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

class ReferralEntity(
    googleAnalyticsQuery: GoogleAnalyticsQuery
) : DimensionEntity<AdStatisticsDto>(googleAnalyticsQuery) {
    override val metricExpressions = listOf("ga:pageviews", "ga:avgTimeOnPage")
    override val dimensionNames = listOf("ga:pagePath", "ga:fullReferrer")
    override val filterExpression = "ga:pagePath=~^/stillinger/stilling"

    override fun toStatisticsDto(row: ReportRow): StatisticsDto<AdStatisticsDto> {
        return AdStatisticsDto(
            pageViews = row.getMetric().first().toInt(),
            averageTimeOnPage = listOf(row.getMetric().last().toDouble()),
            referrals = listOf(row.dimensions.last()),
            viewsPerReferral = listOf(row.getMetric().first().toInt())
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return listOf(row.dimensions.first().split("/").last())
    }
}

class DateEntity(
    googleAnalyticsQuery: GoogleAnalyticsQuery
) : DimensionEntity<AdStatisticsDto>(googleAnalyticsQuery) {
    override val metricExpressions = listOf("ga:pageviews")
    override val dimensionNames = listOf("ga:pagePath", "ga:date")
    override val filterExpression = "ga:pagePath=~^/stillinger/stilling"

    override fun toStatisticsDto(row: ReportRow): StatisticsDto<AdStatisticsDto> {
        return AdStatisticsDto(
            dates = listOf(row.dimensions.last()),
            viewsPerDate = listOf(row.getMetric().first().toInt())
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return listOf(row.dimensions.first().split("/").last())
    }
}

class CandidateEntity(
    googleAnalyticsQuery: GoogleAnalyticsQuery
) : DimensionEntity<CandidateStatisticsDto>(googleAnalyticsQuery) {
    override val metricExpressions = listOf("ga:uniquePageviews")
    override val dimensionNames = listOf("ga:pagePath")
    override val filterExpression =
        "ga:pagePath=~^/kandidater/cv\\?kandidatNr," +
            "ga:pagePath=~^/kandidater-next/cv\\?kandidatNr;" +
            "ga:pagePath!~^.*........-....-....-....-.............*$"

    override fun toStatisticsDto(row: ReportRow): StatisticsDto<CandidateStatisticsDto> {
        return CandidateStatisticsDto(
            pageViews = row.getMetric().first().toInt()
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return listOf(
            row.dimensions.first()
                .split("/").last()
                .split("?").last()
                .split("&").first()
                .split("=").last()
        )
    }
}

class CandidateFilterEntity(
    googleAnalyticsQuery: GoogleAnalyticsQuery
) : DimensionEntity<CandidateFilterStatisticsDto>(googleAnalyticsQuery) {
    override val metricExpressions = listOf("ga:uniquePageviews")
    override val dimensionNames = listOf("ga:pagePath")
    override val filterExpression =
        "ga:pagePath=~^/kandidater\\?," +
            "ga:pagePath=~^/kandidater-next\\?;" +
            "ga:pagePath!~^.*........-....-....-....-.............*$"

    override fun toStatisticsDto(row: ReportRow): StatisticsDto<CandidateFilterStatisticsDto> {
        return CandidateFilterStatisticsDto(
            pageViews = row.getMetric().first().toInt()
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return queryStringToKey(row.dimensions.first().split("/").last())
    }

    private fun queryStringToKey(queryString: String): List<String> {
        val pathAndQueries = queryString.split("?")
        return if(pathAndQueries.first() == "kandidater") {
            val queryNameAndValues = pathAndQueries.last().split("&").last().split("=")
            listOf(
                nameAndValueToString(queryNameAndValues.first(), queryNameAndValues.last().split("_").last())
            )
        } else {
            pathAndQueries.last().split("&").map { filter ->
                val queryNameAndValues = filter.split("=")
                queryNameAndValues.last().split("_").map { filterValue ->
                    nameAndValueToString(queryNameAndValues.first(), filterValue)
                }
            }.flatten()
        }
    }

    private fun nameAndValueToString(name: String, value: String): String {
        return "${name.toLowerCase()}=${value.toLowerCase()}"
    }
}

private fun ReportRow.getMetric() = metrics.first().getValues()

