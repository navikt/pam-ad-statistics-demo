package no.nav.arbeidsplassen.analytics

import com.google.api.services.analyticsreporting.v4.model.ReportRow
import no.nav.arbeidsplassen.analytics.ad.AdStatisticsRepository
import no.nav.arbeidsplassen.analytics.candidate.CandidateStatisticsRepository
import no.nav.arbeidsplassen.analytics.filter.CandidateFilterStatisticsRepository
import no.nav.arbeidsplassen.analytics.googleapi.GoogleAnalyticsQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GoogleAnalyticsService(
    private val adStatisticsRepository: AdStatisticsRepository,
    private val candidateStatisticsRepository: CandidateStatisticsRepository,
    private val candidateFilterStatisticsRepository: CandidateFilterStatisticsRepository,
    private val googleAnalyticsQuery: GoogleAnalyticsQuery
) {

    private fun <T : StatisticsDto<T>> dimensionEntitiesToStatisticsDtoMap(
        vararg dimensionEntities: DimensionEntity<T>
    ): Map<String, T> {
        return dimensionEntities.map { dimensionEntity ->
            val listOfGoogleAnalyticsReportsRows = mutableListOf<ReportRow>()
            var pageToken: String? = "init"
            while (pageToken != null) {
                val googleAnalyticsReport = googleAnalyticsQuery.getGoogleAnalyticsReport(dimensionEntity, pageToken)
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

    //not sure what event to use with EventListener
    @ConditionalOnProperty(
        value = ["scheduler.enable"], havingValue = "true", matchIfMissing = true
    )
    //kanskje fixeddelay/fixedrate istedet for cron
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Oslo")
    @EventListener(ApplicationReadyEvent::class)
    private fun scheduledRepoUpdate() {
        val logger: Logger = LoggerFactory.getLogger(GoogleAnalyticsService::class.java)

        val UUIDToAdDtoMap = dimensionEntitiesToStatisticsDtoMap(
            ReferralEntity("5DaysAgo", "today"),
            DateEntity("5DaysAgo", "today")
        )
        adStatisticsRepository.updateUUIDToAdStatisticsDtoMap(UUIDToAdDtoMap)

        val UUIDToCandidateDtoMap = dimensionEntitiesToStatisticsDtoMap(
            CandidateEntity("5DaysAgo", "today"),
            CandidateShortlistEntity("5DaysAgo", "today")
        )
        candidateStatisticsRepository.updateUUIDToCandidateStatisticsDtoMap(UUIDToCandidateDtoMap)

        val UUIDToCandidateFilterDtoMap = dimensionEntitiesToStatisticsDtoMap(
            CandidateFilterEntity("5DaysAgo", "today")
        )
        candidateFilterStatisticsRepository.updateUUIDToCandidateFilterStatisticsDtoMap(UUIDToCandidateFilterDtoMap)



        logger.info("Repositories have been updated")
    }
}
