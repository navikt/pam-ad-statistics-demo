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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class GoogleAnalyticsService(
    private val adAnalyticsRepository: AdAnalyticsRepository
) {

    private var analyticsReporting = initializeAnalyticsReporting()

    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credential = GoogleCredentials
            .fromStream(GoogleAnalyticsService::class.java.getResourceAsStream(KEY_FILE_LOCATION))
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credential)

        return AnalyticsReporting.Builder(
            httpTransport,
            JSON_FACTORY, requestInitializer
        )
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

        pageToken?.let {
            request.pageToken = it
        }


        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()
    }

    private fun reportsResponseToStatisticsRepo(
        reportsResponse: GetReportsResponse
    ): Map<String, AdDto> {
        var currentReportsResponse = reportsResponse
        //sorry :(
        val adDtoMap = mutableMapOf<String, AdDto>()
        var isNextToken = true

        while (isNextToken) {
            currentReportsResponse.getReport().data.rows.forEach { row ->
                val currentPath = row.dimensions.first().split("/").last()
                adDtoMap[currentPath] = adDtoMap[currentPath] mergeWith rowToDto(row)
            }

            val nextToken = currentReportsResponse.getReport().nextPageToken
            if (nextToken == null) {
                //kunne hatt return her
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

    private infix fun AdDto?.mergeWith(other: AdDto): AdDto {
        return this?.let {
            AdDto(
                sidevisninger = it.sidevisninger + other.sidevisninger,
                average = it.average + other.average,
                referrals = it.referrals + other.referrals,
                viewsPerReferral = it.viewsPerReferral + other.viewsPerReferral
            )
        } ?: other
    }

    private fun rowToDto(row: ReportRow) =
        AdDto(
            sidevisninger = row.getMetric().first().toInt(),
            average = listOf(row.getMetric().last().toDouble()),
            referrals = listOf(row.dimensions.last()),
            viewsPerReferral = listOf(row.getMetric().first().toInt())
        )

    private fun ReportRow.getMetric() = metrics.first().getValues()

    //this is implying we only send one request
    private fun GetReportsResponse.getReport() = reports.first()

    @PostConstruct
    private fun initializeRepo() {
        val reportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS,
            dimensionNames = DIMENSION_NAMES
        )

        val UUIDToDtoMap = reportsResponseToStatisticsRepo(reportsResponse)

        adAnalyticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }

    @ConditionalOnProperty(
        value = ["scheduler.enable"], havingValue = "true", matchIfMissing = true
    )
    //kanskje fixeddelay/fixedrate istedet for cron
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Oslo")
    private fun scheduledRepoUpdate() {
        val reportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS,
            dimensionNames = DIMENSION_NAMES
        )

        val UUIDToDtoMap = reportsResponseToStatisticsRepo(reportsResponse)
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
