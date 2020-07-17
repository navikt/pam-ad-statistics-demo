package no.nav.arbeidsplassen.placeholder

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes
import com.google.api.services.analyticsreporting.v4.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import org.springframework.stereotype.Component


private const val KEY_FILE_LOCATION = "/credentials.json"
private const val VIEW_ID = "177785619"
private const val APPLICATION_NAME = "Analytics Reporting Demo"

@Component
class GoogleDemo {

    private val JSON_FACTORY = GsonFactory.getDefaultInstance()

    fun returnStilling(id: String): AdDto? {
        val returnting = initializeAnalyticsReporting().getReport(
                listOf("ga:pageviews", "ga:avgTimeOnPage"),
                listOf("Sidevisninger", "Gj.tid"),
                listOf("ga:pageTitle", "ga:pagePath", "ga:fullReferrer"),
                id
        ).createJsonObject()
        return returnting[id]
    }


    private fun initializeAnalyticsReporting(): AnalyticsReporting {
        val httpTransport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()

        val credential = GoogleCredentials
            .fromStream(
                GoogleDemo::class.java.getResourceAsStream(
                    KEY_FILE_LOCATION
                ))
            .createScoped(listOf(AnalyticsReportingScopes.ANALYTICS_READONLY))

        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credential)

        return AnalyticsReporting.Builder(httpTransport,
            JSON_FACTORY, requestInitializer)
            .setApplicationName(APPLICATION_NAME).build()
    }

    private fun AnalyticsReporting.getReport(metricExpressions: List<String>, aliases: List<String>, dimensionNames: List<String>, key: String): GetReportsResponse {
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
            .setFiltersExpression("ga:pagePath=~^/stillinger/stilling/${key}")

        return reports().batchGet(GetReportsRequest().setReportRequests(listOf(request))).execute()

    }


    private fun GetReportsResponse.createJsonObject(): Map<String, AdDto> {
        val testMap = mutableMapOf<String, AdDto>()
        reports.forEach {report ->
            val rows = report.data.rows
            if (rows == null) {
                println("No data found for $VIEW_ID")
                //throw noe exception eller return emptymap
            }
            rows?.forEach { row ->
                val dimensions = row.dimensions
                val metrics = row.metrics
                val key = dimensions[1].split("/").last()
                if (testMap.containsKey(key)) {
                    testMap[key]?.referrals?.put(dimensions[2], metrics.first().getValues()[0].toInt())
                    testMap[key]?.sidevisninger = testMap[key]?.sidevisninger?.plus(metrics.first().getValues()[0].toInt()) ?: 0
                    testMap[key]?.avg?.add(metrics.first().getValues()[1].toDouble())
                } else {
                    val stilling = AdDto(
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
