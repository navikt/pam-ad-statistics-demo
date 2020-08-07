package no.nav.arbeidsplassen.analytics.googleapi

import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
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
import no.nav.arbeidsplassen.analytics.DimensionEntity
import no.nav.arbeidsplassen.analytics.StatisticsDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

//er det egentlig en service?
@Service
class GoogleAnalyticsQuery(
    @Value("\${GOOGLE_API_CREDENTIALS_PATH}") private val credentialPath: String,
    @Value("\${pagesize.value}") private val pagesize: Int
) {
    val analyticsReporting = initializeAnalyticsReporting()

    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = NetHttpTransport()
        val credential = GoogleCredentials
            .fromStream(File(credentialPath).inputStream())
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
        filterExpression: String,
        startDate: String,
        endDate: String,
        pageToken: String?
    ): GetReportsResponse {

        val dateRange = DateRange().apply {
            this.startDate = startDate
            this.endDate = endDate
        }
        val metrics: List<Metric> = metricExpressions.map { Metric().setExpression(it) }
        val dimensions: List<Dimension> = dimensionNames.map { Dimension().setName(it) }

        val request = ReportRequest()
            .setViewId(VIEW_ID)
            .setDateRanges(listOf(dateRange))
            .setMetrics(metrics)
            .setDimensions(dimensions)
            .setSamplingLevel("LARGE")
            .setFiltersExpression(filterExpression)
            .setPageSize(pagesize)

        pageToken?.let {
            request.pageToken = it
        }

        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()
    }

    fun <T : StatisticsDto<T>> getGoogleAnalyticsReport(
        dimensionEntity: DimensionEntity<T>,
        pageToken: String?
    ): GoogleAnalyticsReport {
        val reportsResponse = analyticsReporting.getReportsResponse(
            dimensionEntity.metricExpressions,
            dimensionEntity.dimensionNames,
            dimensionEntity.filterExpression,
            dimensionEntity.startDate,
            dimensionEntity.endDate,
            pageToken
        )
        return GoogleAnalyticsReport(
            rows = reportsResponse.getReport().data.rows,
            nextPageToken = reportsResponse.getReport().nextPageToken
        )
    }

    companion object {
        private const val VIEW_ID = "177785619"
        private const val APPLICATION_NAME = "Analytics Reporting Demo"
        private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    }
}

//only works when sending non-batched requests a.k.a one request per report
private fun GetReportsResponse.getReport() = reports.first()

data class GoogleAnalyticsReport(
    val rows: List<ReportRow>,
    val nextPageToken: String?
)