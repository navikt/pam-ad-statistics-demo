package no.nav.arbeidsplassen.placeholder

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes
import com.google.api.services.analyticsreporting.v4.model.*
import org.springframework.stereotype.Component

private const val KEY_FILE_LOCATION = "/credentials.json"
private const val VIEW_ID = "177785619"
private const val APPLICATION_NAME = "Analytics Reporting Demo"

@Component
class GoogleDemo {

    private val JSON_FACTORY = GsonFactory.getDefaultInstance()

    /*
    fun main(args: Array<String>) {
        try {
            val reportingService = initializeAnalyticsReporting()
            println(reportingService.getReport(listOf("ga:pageviews", "ga:avgTimeOnPage"),
                                       listOf("Sidevisninger", "Gj.tid"),
                                       listOf("ga:pageTitle", "ga:pagePath", "ga:fullReferrer")).createJsonObject())

            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
     */

    fun returnStilling(id: String): Stilling? {
        val reportingService = initializeAnalyticsReporting()
        return reportingService.getReport(
            listOf("ga:pageviews", "ga:avgTimeOnPage"),
            listOf("Sidevisninger", "Gj.tid"),
            listOf("ga:pageTitle", "ga:pagePath", "ga:fullReferrer")
        ).createJsonObject()[id]

    }


    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credential = GoogleCredential
            .fromStream(
                GoogleDemo::class.java.getResourceAsStream(
                    KEY_FILE_LOCATION
                ))
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        return AnalyticsReporting.Builder(httpTransport,
            JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME).build()

    }

    private fun AnalyticsReporting.getReport(metricExpressions: List<String>, aliases: List<String>, dimensionNames: List<String>): GetReportsResponse {
        val dateRange = DateRange().apply {
            startDate = "1DaysAgo"
            endDate = "today"
        }
        val metrics: MutableList<Metric> = mutableListOf<Metric>()
        val dimensions: MutableList<Dimension> = mutableListOf<Dimension>()
        for (i in metricExpressions.indices) {
            metrics.add(Metric().setExpression(metricExpressions[i]).setAlias(aliases[i]))
        }
        dimensionNames.forEach{
            dimensions.add(Dimension().setName(it))
        }

        /*
        val sessionsMetric = Metric().setExpression("ga:pageviews").setAlias("sidevisninger")
        val pageTime = Metric().setExpression("ga:avgTimeOnPage").setAlias("Gjennomsnittstid")
        val pageTitleDimension = Dimension().setName("ga:pageTitle")
        val pagePathDimension = Dimension().setName("ga:pagePath")
        val referralDimension = Dimension().setName("ga:fullReferrer")

         */

        val request = ReportRequest()
            .setViewId(VIEW_ID)
            .setDateRanges(listOf(dateRange))
            .setMetrics(metrics)
            .setDimensions(dimensions)
            .setFiltersExpression("ga:pagePath=~^/stillinger")

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
                println(
                        "${dimensions[1].split("/").last()}\t${dimensions[0]} - " +
                        "${metricHeader[0].name}: ${metrics.first().getValues()[0]} - " +
                        "${metricHeader[1].name}: ${metrics.first().getValues()[1]} - ${dimensions[2]}"
                )
            }
        }
    }

    private fun GetReportsResponse.createJsonObject(): Map<String, Stilling> {
        val testMap = mutableMapOf<String, Stilling>()
        reports.forEach {report ->
            val rows = report.data.rows
            if (rows == null) {
                println("No data found for $VIEW_ID")
            }
            rows?.forEach { row ->
                val dimensions = row.dimensions
                val metrics = row.metrics
                val key = dimensions[1].split("/").last()
                if (testMap.containsKey(key)) {
                    testMap[key]?.referrals?.put(dimensions[2], metrics.first().getValues()[0].toInt())
                    testMap[key]?.sidevisninger =
                        testMap[key]?.sidevisninger?.plus(metrics.first().getValues()[0].toInt())
                    testMap[key]?.avg?.add(metrics.first().getValues()[1].toDouble())
                } else {
                    val stilling = Stilling(
                        dimensions[0],
                        metrics.first().getValues()[0].toInt(),
                        mutableListOf(metrics.first().getValues()[1].toDouble()),
                        mutableMapOf(dimensions[2] to metrics.first().getValues()[0].toInt())
                    )
                    testMap[dimensions[1].split("/").last()] = stilling
                }
            }
        }
        return testMap
    }


}