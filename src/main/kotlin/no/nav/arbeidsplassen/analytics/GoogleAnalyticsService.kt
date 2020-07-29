package no.nav.arbeidsplassen.analytics

import no.nav.arbeidsplassen.analytics.ad.AdStatisticsRepository
import no.nav.arbeidsplassen.analytics.candidate.CandidateStatisticsRepository
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
    private val googleAnalyticsQuery: GoogleAnalyticsQuery
) {

    private fun <T : StatisticsDto<T>> reportsResponseToStatisticsRepo(
        startDate: String,
        endDate: String,
        vararg dimensionEntities: DimensionEntity<T>
    ): MutableMap<String, T> {
        val StatisticsDtoMap = mutableMapOf<String, T>()
        //kanskje litt mange foreaches
        dimensionEntities.forEach { dimensionEntity ->
            dimensionEntity.setDateRange(startDate, endDate)
            while (dimensionEntity.nextPage()) {
                dimensionEntity.rows.forEach { row ->
                    val path = dimensionEntity.getPath(row)
                    path.forEach { key ->
                        StatisticsDtoMap[key] = dimensionEntity.toStatisticsDto(row) mergeWith StatisticsDtoMap[key]
                    }
                }
            }
        }
        return StatisticsDtoMap
    }

    @PostConstruct
    private fun initializeRepo() {
        val UUIDToAdDtoMap = reportsResponseToStatisticsRepo(
            "1DaysAgo",
            "today",
            ReferralEntity(googleAnalyticsQuery),
            DateEntity(googleAnalyticsQuery)
        )
        adStatisticsRepository.updateUUIDToAdStatisticsDtoMap(UUIDToAdDtoMap)

        val UUIDToCandidateDtoMap = reportsResponseToStatisticsRepo(
            "1DaysAgo",
            "today",
            CandidateEntity(googleAnalyticsQuery)
        )
        candidateStatisticsRepository.updateUUIDToCandidateStatisticsDtoMap(UUIDToCandidateDtoMap)
    }

    @ConditionalOnProperty(
        value = ["scheduler.enable"], havingValue = "true", matchIfMissing = true
    )
    //kanskje fixeddelay/fixedrate istedet for cron
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Oslo")
    private fun scheduledRepoUpdate() {
        val UUIDToAdDtoMap = reportsResponseToStatisticsRepo(
            "1DaysAgo",
            "today",
            ReferralEntity(googleAnalyticsQuery),
            DateEntity(googleAnalyticsQuery)
        )
        adStatisticsRepository.updateUUIDToAdStatisticsDtoMap(UUIDToAdDtoMap)

        val UUIDToCandidateDtoMap = reportsResponseToStatisticsRepo(
            "1DaysAgo",
            "today",
            CandidateEntity(googleAnalyticsQuery)
        )
        candidateStatisticsRepository.updateUUIDToCandidateStatisticsDtoMap(UUIDToCandidateDtoMap)
    }
}
