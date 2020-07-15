import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes
import com.google.api.services.analyticsreporting.v4.model.*

object GoogleDemo {

    private const val APPLICATION_NAME = "Analytics Reporting Demo"
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
    private const val KEY_FILE_LOCATION = "/credentials.json"
    private const val VIEW_ID = "177785619"

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val reportingService = initializeAnalyticsReporting()
            reportingService.getReport()
                .prettyPrint()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credential = GoogleCredential
            .fromStream(GoogleDemo::class.java.getResourceAsStream(KEY_FILE_LOCATION))
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        return AnalyticsReporting.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME).build()

    }

    private fun AnalyticsReporting.getReport(): GetReportsResponse {
        val dateRange = DateRange().apply {
            startDate = "1DaysAgo"
            endDate = "today"
        }

        val sessionsMetric = Metric().setExpression("ga:pageviews").setAlias("sidevisninger")
        val pageTime = Metric().setExpression("ga:avgTimeOnPage").setAlias("Gjennomsnittstid")
        val pageTitleDimension = Dimension().setName("ga:pageTitle")
        val pagePathDimension = Dimension().setName("ga:pagePath")

        val request = ReportRequest()
            .setViewId(VIEW_ID)
            .setDateRanges(listOf(dateRange))
            .setMetrics(listOf(sessionsMetric,pageTime))
            .setDimensions(listOf(pageTitleDimension, pagePathDimension))

        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()

    }

    private fun GetReportsResponse.prettyPrint() {
        return reports.forEach { report ->
            val header = report.columnHeader
            val metricHeader = header.metricHeader.metricHeaderEntries
            val rows = report.data.rows
            if (rows == null) {
                println("No data found for $VIEW_ID")
            }
            rows?.forEach { row ->
                val dimensions = row.dimensions
                val metrics = row.metrics
                println("${dimensions[1].split("/").last()}\t${dimensions[0]} - " +
                            "${metricHeader[0].name}: ${metrics.first().getValues()[0]} - " +
                            "${metricHeader[1].name}: ${metrics.first().getValues()[1]}"
                )
            }
        }
    }

}
