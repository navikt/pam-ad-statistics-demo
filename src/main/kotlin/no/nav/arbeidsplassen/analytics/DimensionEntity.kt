package no.nav.arbeidsplassen.analytics

import com.google.api.services.analyticsreporting.v4.model.ReportRow
import no.nav.arbeidsplassen.analytics.ad.dto.AdStatisticsDto
import no.nav.arbeidsplassen.analytics.candidate.dto.CandidateStatisticsDto
import no.nav.arbeidsplassen.analytics.filter.dto.CandidateFilterStatisticsDto
import java.net.URLDecoder

abstract class DimensionEntity<T : StatisticsDto<T>> {
    var rows = listOf<ReportRow>()
    abstract val metricExpressions: List<String>
    abstract val dimensionNames: List<String>
    abstract val filterExpression: String
    var startDate = "1DaysAgo"
    var endDate = "today"

    abstract fun toStatisticsDto(row: ReportRow): T

    abstract fun getKey(row: ReportRow): List<String>

    fun setDateRange(startDate: String, endDate: String) {
        this.startDate = startDate
        this.endDate = endDate
    }

    fun googleAnalyticsReportsToStatisticsDtoMap(listOfGoogleAnalyticsReportsRows: List<ReportRow>): Map<String, T> {
        return listOfGoogleAnalyticsReportsRows.map { row ->
            getKey(row).map { key -> key to toStatisticsDto(row) }
        }.flatten()
            .groupBy({ dtoMapEntry -> dtoMapEntry.first }, { dtoMapEntry -> dtoMapEntry.second })
            .mapValues { (_, values) ->
                values.reduce { acc, statisticsDto -> acc.mergeWith(statisticsDto) }
            }
    }
}

class ReferralEntity : DimensionEntity<AdStatisticsDto>() {
    override val metricExpressions = listOf("ga:pageviews", "ga:avgTimeOnPage")
    override val dimensionNames = listOf("ga:pagePath", "ga:fullReferrer")
    override val filterExpression = "ga:pagePath=~^/stillinger/stilling/"

    override fun toStatisticsDto(row: ReportRow): AdStatisticsDto {
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

class DateEntity : DimensionEntity<AdStatisticsDto>() {
    override val metricExpressions = listOf("ga:pageviews")
    override val dimensionNames = listOf("ga:pagePath", "ga:date")
    override val filterExpression = "ga:pagePath=~^/stillinger/stilling/"

    override fun toStatisticsDto(row: ReportRow): AdStatisticsDto {
        return AdStatisticsDto(
            dates = listOf(row.dimensions.last()),
            viewsPerDate = listOf(row.getMetric().first().toInt())
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return listOf(row.dimensions.first().split("/").last())
    }
}

class CandidateEntity : DimensionEntity<CandidateStatisticsDto>() {
    override val metricExpressions = listOf("ga:uniquePageviews")
    override val dimensionNames = listOf("ga:pagePath")
    override val filterExpression =
        "ga:pagePath=~^/kandidater/cv\\?kandidatNr," +
            "ga:pagePath=~^/kandidater-next/cv\\?kandidatNr;" +
            "ga:pagePath!~^.*........-....-....-....-.............*$"

    override fun toStatisticsDto(row: ReportRow): CandidateStatisticsDto {
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

class CandidateShortlistEntity : DimensionEntity<CandidateStatisticsDto>() {
    override val metricExpressions = listOf("ga:uniquePageviews")
    override val dimensionNames = listOf("ga:pagePath")
    override val filterExpression =
        "ga:pagePath=~^/kandidater/lister/detaljer/.*/cv/"

    override fun toStatisticsDto(row: ReportRow): CandidateStatisticsDto {
        return CandidateStatisticsDto(
            pageViewsFromShortlist = row.getMetric().first().toInt()
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return listOf(
            row.dimensions.first()
                .split("/").last()
        )
    }
}

class CandidateFilterEntity : DimensionEntity<CandidateFilterStatisticsDto>() {
    override val metricExpressions = listOf("ga:uniquePageviews")
    override val dimensionNames = listOf("ga:pagePath")
    override val filterExpression =
        "ga:pagePath=~^/kandidater\\?," +
            "ga:pagePath=~^/kandidater-next\\?;" +
            //trenger ikke denne tror jeg men spiller det safe
            "ga:pagePath!~^.*........-....-....-....-.............*$"

    override fun toStatisticsDto(row: ReportRow): CandidateFilterStatisticsDto {
        return CandidateFilterStatisticsDto(
            pageViews = row.getMetric().first().toInt()
        )
    }

    override fun getKey(row: ReportRow): List<String> {
        return queryStringToKey(row.dimensions.first())
    }

    private fun queryStringToKey(queryString: String): List<String> {
        val pathAndQueries = queryString.split("?")
        return if (pathAndQueries.first() == "/kandidater") {
            val queryNameAndValues = pathAndQueries.last().split("&").last().split("=")
            listOf(
                nameAndValueToString(queryNameAndValues.first(), queryNameAndValues.last().split("_").last())
            )
        } else {
            pathAndQueries.last().split("&").flatMap { filter ->
                val queryNameAndValues = filter.split("=")
                queryNameAndValues.last().split("_").map { filterValue ->
                    nameAndValueToString(queryNameAndValues.first(), filterValue)
                }
            }
        }
    }

    private fun nameAndValueToString(name: String, value: String): String {
        return URLDecoder.decode(name.toLowerCase(), "UTF-8") +
            "=" +
            URLDecoder.decode(value.toLowerCase(), "UTF-8")
    }
}

//Metrics is a list of metrics for each requested daterange, since we only use one daterange I just take the first one
private fun ReportRow.getMetric() = metrics.first().getValues()


