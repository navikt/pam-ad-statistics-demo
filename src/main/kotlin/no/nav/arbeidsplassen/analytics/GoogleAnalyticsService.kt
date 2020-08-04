package no.nav.arbeidsplassen.analytics

import com.google.api.services.analyticsreporting.v4.model.ReportRow
import no.nav.arbeidsplassen.analytics.ad.AdStatisticsRepository
import no.nav.arbeidsplassen.analytics.candidate.CandidateStatisticsRepository
import no.nav.arbeidsplassen.analytics.filter.CandidateFilterStatisticsRepository
import no.nav.arbeidsplassen.analytics.googleapi.GoogleAnalyticsQuery
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
//not a huge fan of having the googleAnalyticsQuery go through GoogleAnalyticsService to reach DimensionEntity
class GoogleAnalyticsService(
    private val adStatisticsRepository: AdStatisticsRepository,
    private val candidateStatisticsRepository: CandidateStatisticsRepository,
    private val candidateFilterStatisticsRepository: CandidateFilterStatisticsRepository,
    private val googleAnalyticsQuery: GoogleAnalyticsQuery
) {

    private fun <T : StatisticsDto<T>> dimensionEntitiesToStatisticsDtoMap(
        startDate: String,
        endDate: String,
        vararg dimensionEntities: DimensionEntity<T>
    ): Map<String, T> {
        return dimensionEntities.map { dimensionEntity ->
            val listOfGoogleAnalyticsReportsRows = mutableListOf<ReportRow>()
            var pageToken: String? = "init"
            dimensionEntity.setDateRange(startDate, endDate)
            while (pageToken != null) {
                val googleAnalyticsReport = dimensionEntity.getGoogleAnalyticsReport(pageToken)
                listOfGoogleAnalyticsReportsRows += googleAnalyticsReport.rows
                pageToken = googleAnalyticsReport.nextPageToken
            }
            dimensionEntity.googleAnalyticsReportsToStatisticsDtoMap(listOfGoogleAnalyticsReportsRows)
        }.reduce { acc, map -> acc.mergeStatisticsDtoMaps(map) }
    }

    private fun <T : StatisticsDto<T>> Map<String, T>.mergeStatisticsDtoMaps(other: Map<String, T>): Map<String, T> {
        return (this.asSequence() + other.asSequence())
            .groupBy(
                { statisticsDtoMapEntry -> statisticsDtoMapEntry.key },
                { statisticsDtoMapEntry -> statisticsDtoMapEntry.value }
            )
            .mapValues { (_, values) -> values.reduce { acc, statisticsDto -> acc.mergeWith(statisticsDto) } }
    }

    @PostConstruct
    private fun initializeRepo() {

        val UUIDToAdDtoMap = dimensionEntitiesToStatisticsDtoMap(
            "1DaysAgo",
            "today",
            ReferralEntity(googleAnalyticsQuery),
            DateEntity(googleAnalyticsQuery)
        )
        adStatisticsRepository.updateUUIDToAdStatisticsDtoMap(UUIDToAdDtoMap)

        val UUIDToCandidateDtoMap = dimensionEntitiesToStatisticsDtoMap(
            "1DaysAgo",
            "today",
            CandidateEntity(googleAnalyticsQuery),
            CandidateShortlistEntity(googleAnalyticsQuery)
        )
        candidateStatisticsRepository.updateUUIDToCandidateStatisticsDtoMap(UUIDToCandidateDtoMap)

        val UUIDToCandidateFilterDtoMap = dimensionEntitiesToStatisticsDtoMap(
            "1DaysAgo",
            "today",
            CandidateFilterEntity(googleAnalyticsQuery)
        )
        candidateFilterStatisticsRepository.updateUUIDToCandidateFilterStatisticsDtoMap(UUIDToCandidateFilterDtoMap)
    }

    @ConditionalOnProperty(
        value = ["scheduler.enable"], havingValue = "true", matchIfMissing = true
    )
    //kanskje fixeddelay/fixedrate istedet for cron
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Oslo")
    private fun scheduledRepoUpdate() {
        val UUIDToAdDtoMap = dimensionEntitiesToStatisticsDtoMap(
            "1DaysAgo",
            "today",
            ReferralEntity(googleAnalyticsQuery),
            DateEntity(googleAnalyticsQuery)
        )
        adStatisticsRepository.updateUUIDToAdStatisticsDtoMap(UUIDToAdDtoMap)

        val UUIDToCandidateDtoMap = dimensionEntitiesToStatisticsDtoMap(
            "1DaysAgo",
            "today",
            CandidateEntity(googleAnalyticsQuery),
            CandidateShortlistEntity(googleAnalyticsQuery)
        )
        candidateStatisticsRepository.updateUUIDToCandidateStatisticsDtoMap(UUIDToCandidateDtoMap)

        val UUIDToCandidateFilterDtoMap = dimensionEntitiesToStatisticsDtoMap(
            "1DaysAgo",
            "today",
            CandidateFilterEntity(googleAnalyticsQuery)
        )
        candidateFilterStatisticsRepository.updateUUIDToCandidateFilterStatisticsDtoMap(UUIDToCandidateFilterDtoMap)
    }
}
