package no.nav.arbeidsplassen.analytics.ad

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes
import com.google.api.services.analyticsreporting.v4.model.DateRange
import com.google.api.services.analyticsreporting.v4.model.Dimension
import com.google.api.services.analyticsreporting.v4.model.GetReportsRequest
import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse
import com.google.api.services.analyticsreporting.v4.model.Metric
import com.google.api.services.analyticsreporting.v4.model.ReportRequest
import com.google.api.services.analyticsreporting.v4.model.ReportRow
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import no.nav.arbeidsplassen.analytics.ad.dto.AdDto

abstract class DimensionEntity {
    private var analyticsReporting = initializeAnalyticsReporting()
    var rows = listOf<ReportRow>()
    private var nextPageToken: String? = ""
    abstract val metricExpressions: List<String>
    abstract val dimensionNames: List<String>

    abstract fun toAdDto(row: ReportRow): AdDto

    fun nextPage(): Boolean {
        return nextPageToken?.let {
            val reportsResponse =
                analyticsReporting.getReportsResponse(metricExpressions, dimensionNames, nextPageToken)
            rows = reportsResponse.getReport().data.rows
            nextPageToken = reportsResponse.getReport().nextPageToken
            true
        } ?: false
    }

    private fun AnalyticsReporting.getReportsResponse(
        metricExpressions: List<String>,
        dimensionNames: List<String>,
        pageToken: String? = null
    ): GetReportsResponse {

        val dateRange = DateRange().apply {
            startDate = "1DaysAgo"
            endDate = "today"
        }
        val metrics: List<Metric> = metricExpressions.map { Metric().setExpression(it) }
        val dimensions: List<Dimension> = dimensionNames.map { Dimension().setName(it) }

        val request = ReportRequest()
            .setViewId(VIEW_ID)
            .setDateRanges(listOf(dateRange))
            .setMetrics(metrics)
            .setDimensions(dimensions)
            .setFiltersExpression("ga:pagePath=~^/stillinger")
            //burde være variabel
            .setPageSize(10000)

        pageToken?.let {
            request.pageToken = it
        }

        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()
    }

    //this is implying we only send one request
    private fun GetReportsResponse.getReport() = reports.first()

    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credential = GoogleCredentials
            .fromStream(GoogleAnalyticsService::class.java.getResourceAsStream("/credentials.json"))
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credential)

        return AnalyticsReporting.Builder(
            httpTransport,
            JSON_FACTORY, requestInitializer
        )
            .setApplicationName(APPLICATION_NAME).build()
    }

    companion object {
        private const val VIEW_ID = "177785619"
        private const val APPLICATION_NAME = "Analytics Reporting Demo"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    }
}

class ReferralEntity : DimensionEntity() {
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

class DateEntity : DimensionEntity() {
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

