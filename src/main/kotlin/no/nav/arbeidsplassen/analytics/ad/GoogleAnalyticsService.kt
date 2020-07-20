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
import javax.annotation.PostConstruct


@Service
class GoogleAnalyticsService(
    private val adStatisticsRepository: AdStatisticsRepository
) {

    private var analyticsReporting = initializeAnalyticsReporting()

    /*
    @Cacheable(value=["DTOByUUID"], key="#UUID")
    fun fetchAnalyticsByAdId(UUID: String): AdDto? {
        //skal flytte denne til repo component
        val map = analyticsReporting.getReport(
                metricExpressions = listOf("ga:pageviews", "ga:avgTimeOnPage"),
                dimensionNames = listOf("ga:pagePath", "ga:fullReferrer")
        ).toAdRepo()
        return map[UUID]
    }
     */

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
        dimensionNames: List<String>
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
            .setPageSize(100000)
            .setFiltersExpression("ga:pagePath=~^/stillinger")

        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()

    }

    private fun GetReportsResponse.toAdRepo(): Map<String, AdDto> {
        //sorry :(
        val map = mutableMapOf<String, AdDto>()
        reports.first().data.rows.forEach{row ->
            val currentPath = row.dimensions.first().split("/").last()
            map[currentPath] = map[currentPath] merge rowToDto(row)
        }
        return map
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

    @PostConstruct
    private fun initializeRepo() {
        val UUIDToDtoMap = analyticsReporting.getReport(
            metricExpressions = listOf("ga:pageviews", "ga:avgTimeOnPage"),
            dimensionNames = listOf("ga:pagePath", "ga:fullReferrer")
        ).toAdRepo()

        adStatisticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }

    companion object {
        private const val KEY_FILE_LOCATION = "/credentials.json"
        private const val VIEW_ID = "177785619"
        private const val APPLICATION_NAME = "Analytics Reporting Demo"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()

        //metrics and dimension

    }

}
