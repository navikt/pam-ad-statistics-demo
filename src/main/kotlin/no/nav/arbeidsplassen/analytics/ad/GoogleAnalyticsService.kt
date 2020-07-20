package no.nav.arbeidsplassen.analytics.ad

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes
import com.google.api.services.analyticsreporting.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import no.nav.arbeidsplassen.analytics.ad.dto.AdDto
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.SpringVersion
import org.springframework.stereotype.Service



@Service
class GoogleAnalyticsService {

    private var analyticsReporting = initializeAnalyticsReporting()

    @Cacheable(value=["DTOByUUID"], key="#UUID")
    fun fetchAnalyticsByAdId(UUID: String): AdDto? {
        return analyticsReporting.getReport(
                metricExpressions = listOf("ga:pageviews", "ga:avgTimeOnPage"),
                dimensionNames = listOf("ga:pagePath", "ga:fullReferrer"),
                key = UUID
        ).toAdDto()
    }

    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credential = GoogleCredentials
            .fromStream(GoogleAnalyticsService::class.java.getResourceAsStream(KEY_FILE_LOCATION))
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credential)

        return AnalyticsReporting.Builder(httpTransport,
            JSON_FACTORY, requestInitializer)
            .setApplicationName(APPLICATION_NAME).build()
    }


    private fun AnalyticsReporting.getReport(
        metricExpressions: List<String>,
        dimensionNames: List<String>,
        key: String
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
            .setFiltersExpression("ga:pagePath=~^/stillinger/stilling/${key}")

        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()

    }

    private fun GetReportsResponse.toAdDto() : AdDto {
        return reports.first().data.rows.let { row ->
            AdDto(
                sidevisninger = row.mapNotNull { it.getMetric().first().toInt() }.sum(),
                average = row.mapNotNull { it.getMetric().last().toDouble() }.average(),
                referrals = row.mapNotNull { it.dimensions.last() },
                viewsPerReferral = row.mapNotNull { it.getMetric().first().toInt() }
            )
        }
    }


    private fun ReportRow.getMetric() = metrics.first().getValues()

    companion object {
        private const val KEY_FILE_LOCATION = "/credentials.json"
        private const val VIEW_ID = "177785619"
        private const val APPLICATION_NAME = "Analytics Reporting Demo"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()

        //metrics and dimension

    }

}
