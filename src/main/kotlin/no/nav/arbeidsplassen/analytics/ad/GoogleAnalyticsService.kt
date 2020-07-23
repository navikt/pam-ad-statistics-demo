package no.nav.arbeidsplassen.analytics.ad

import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes
import com.google.api.services.analyticsreporting.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import no.nav.arbeidsplassen.analytics.ad.dto.AdDto
import no.nav.arbeidsplassen.analytics.ad.dto.DateEntity
import no.nav.arbeidsplassen.analytics.ad.dto.DimensionEntity
import no.nav.arbeidsplassen.analytics.ad.dto.ReferralEntity
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import javax.annotation.PostConstruct

@Service
class GoogleAnalyticsService(
    private val adAnalyticsRepository: AdAnalyticsRepository
) {

    private var analyticsReporting = initializeAnalyticsReporting()

    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = NetHttpTransport()

        // TODO - environment specific file path in application.yml
        val credential = GoogleCredentials
            .fromStream(File("/secret/credential/googleCredentials.json").inputStream())
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credential)

        return AnalyticsReporting.Builder(
            httpTransport,
            JSON_FACTORY,
            requestInitializer
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
        dimensionEntity: DimensionEntity,
        adDtoMap: MutableMap<String, AdDto> = mutableMapOf<String, AdDto>(),
        metricExpressions: List<String>,
        dimensionNames: List<String>
    ): MutableMap<String, AdDto> {
        var isNextToken = true
        while (isNextToken) {
            dimensionEntity.rows.forEach { row ->
                val adPath = row.dimensions.first().split("/").last()
                adDtoMap[adPath] = adDtoMap[adPath] mergeWith dimensionEntity.toAdDto(row)
            }

            val nextToken = dimensionEntity.nextPageToken
            if (nextToken == null) {
                //kunne hatt return her
                isNextToken = false
            } else {
                val newReportsResponse = analyticsReporting.getReportsResponse(
                    metricExpressions = metricExpressions,
                    dimensionNames = dimensionNames,
                    pageToken = nextToken
                )
                dimensionEntity.setGetReportsResponse(newReportsResponse)
            }
        }
        return adDtoMap
    }

    private infix fun AdDto?.mergeWith(other: AdDto): AdDto {
        return this?.let {
            AdDto(
                pageViews = it.pageViews + other.pageViews,
                averageTimeOnPage = it.averageTimeOnPage + other.averageTimeOnPage,
                referrals = it.referrals + other.referrals,
                viewsPerReferral = it.viewsPerReferral + other.viewsPerReferral,
                dates = it.dates + other.dates,
                viewsPerDate = it.viewsPerDate + other.viewsPerDate
            )
        } ?: other
    }

    @PostConstruct
    private fun initializeRepo() {
        val referralReportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS1,
            dimensionNames = DIMENSION_NAMES1
        )

        val dateReportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS2,
            dimensionNames = DIMENSION_NAMES2
        )

        val halfwayMap = reportsResponseToStatisticsRepo(
            dimensionEntity = ReferralEntity(referralReportsResponse),
            metricExpressions = METRIC_EXPRESSIONS1,
            dimensionNames = DIMENSION_NAMES1
        )

        val UUIDToDtoMap = reportsResponseToStatisticsRepo(
            dimensionEntity = DateEntity(dateReportsResponse),
            adDtoMap = halfwayMap,
            metricExpressions = METRIC_EXPRESSIONS2,
            dimensionNames = DIMENSION_NAMES2
        )

        adAnalyticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }

    @ConditionalOnProperty(
        value = ["scheduler.enable"], havingValue = "true", matchIfMissing = true
    )
    //kanskje fixeddelay/fixedrate istedet for cron
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Oslo")
    private fun scheduledRepoUpdate() {
        val referralReportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS1,
            dimensionNames = DIMENSION_NAMES1
        )

        val dateReportsResponse = analyticsReporting.getReportsResponse(
            metricExpressions = METRIC_EXPRESSIONS2,
            dimensionNames = DIMENSION_NAMES2
        )

        val halfwayMap = reportsResponseToStatisticsRepo(
            dimensionEntity = ReferralEntity(referralReportsResponse),
            metricExpressions = METRIC_EXPRESSIONS1,
            dimensionNames = DIMENSION_NAMES1
        )

        val UUIDToDtoMap = reportsResponseToStatisticsRepo(
            dimensionEntity = DateEntity(dateReportsResponse),
            adDtoMap = halfwayMap,
            metricExpressions = METRIC_EXPRESSIONS2,
            dimensionNames = DIMENSION_NAMES2
        )
        adAnalyticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }

    companion object {
        private const val VIEW_ID = "177785619"
        private const val APPLICATION_NAME = "Analytics Reporting Demo"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()

        //metrics and dimension
        private val METRIC_EXPRESSIONS1 = listOf("ga:pageviews", "ga:avgTimeOnPage")
        private val DIMENSION_NAMES1 = listOf("ga:pagePath", "ga:fullReferrer")

        private val METRIC_EXPRESSIONS2 = listOf("ga:pageviews")
        private val DIMENSION_NAMES2 = listOf("ga:pagePath", "ga:date")
    }
}
