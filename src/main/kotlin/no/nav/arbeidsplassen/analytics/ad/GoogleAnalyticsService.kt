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
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct


@Service
class GoogleAnalyticsService(
    private val adStatisticsRepository: AdStatisticsRepository
) {

    private var analyticsReporting = initializeAnalyticsReporting()


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

        if (pageToken != null) {
            request.pageToken = pageToken
        }


        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()

    }

    private fun ReportsResponseToStatisticsRepo(
        reportsResponse: GetReportsResponse
    ): Map<String, AdDto> {
        var currentReportsResponse = reportsResponse
        //sorry :(
        val adDtoMap = mutableMapOf<String, AdDto>()
        var isNextToken = true

        while (isNextToken) {
            currentReportsResponse.getReport().data.rows.forEach { row ->
                val currentPath = row.dimensions.first().split("/").last()
                adDtoMap[currentPath] = adDtoMap[currentPath] merge rowToDto(row)
            }

            val nextToken = currentReportsResponse.getReport().nextPageToken
            if (nextToken == null) {
                isNextToken = false
            } else {
                currentReportsResponse = analyticsReporting.getReportsResponse(
                    metricExpressions = METRIC_EXPRESSIONS,
                    dimensionNames = DIMENSION_NAMES,
                    pageToken = nextToken
                )
            }
        }

        return adDtoMap
    }

    private infix fun AdDto?.merge(other: AdDto): AdDto {
        return this?.let {
            AdDto(
                sidevisninger = this.sidevisninger + other.sidevisninger,
                referrals = this.referrals + other.referrals,
                viewsPerReferral = this.viewsPerReferral + other.viewsPerReferral
            )
        } ?: other
    }

    private fun rowToDto(row: ReportRow) =
        AdDto(
            sidevisninger = row.getMetric().first().toInt(),
            referrals = listOf(row.dimensions.last()),
            viewsPerReferral = listOf(row.getMetric().first().toInt())
            )


    private fun ReportRow.getMetric() = metrics.first().getValues()

    private fun GetReportsResponse.getReport() = reports.first()

    @PostConstruct
    private fun initializeRepo() {
        val reportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS,
            dimensionNames = DIMENSION_NAMES
        )

        val UUIDToDtoMap = ReportsResponseToStatisticsRepo(reportsResponse)

        adStatisticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }

    companion object {
        private const val KEY_FILE_LOCATION = "/credentials.json"
        private const val VIEW_ID = "177785619"
        private const val APPLICATION_NAME = "Analytics Reporting Demo"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()

        //metrics and dimension
        private val METRIC_EXPRESSIONS = listOf("ga:pageviews", "ga:avgTimeOnPage")
        private val DIMENSION_NAMES = listOf("ga:pagePath", "ga:fullReferrer")
    }

}
