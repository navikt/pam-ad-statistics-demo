package no.nav.arbeidsplassen.analytics.ad

import no.nav.arbeidsplassen.analytics.ad.dto.AdDto
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class GoogleAnalyticsService(
    private val adAnalyticsRepository: AdAnalyticsRepository
) {

    private fun reportsResponseToStatisticsRepo(
        startDate: String,
        endDate: String,
        vararg dimensionEntities: DimensionEntity
    ): MutableMap<String, AdDto> {
        val adDtoMap = mutableMapOf<String, AdDto>()
        //kanskje litt mange foreaches
        dimensionEntities.forEach { dimensionEntity ->
            dimensionEntity.setDateRange(startDate, endDate)
            while (dimensionEntity.nextPage()) {
                dimensionEntity.rows.forEach { row ->
                    val adPath = row.dimensions.first().split("/").last()
                    adDtoMap[adPath] = adDtoMap[adPath] mergeWith dimensionEntity.toAdDto(row)
                }
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
        val UUIDToDtoMap = reportsResponseToStatisticsRepo(
            "1DaysAgo",
            "today",
            ReferralEntity(),
            DateEntity()
        )

        adAnalyticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }

    @ConditionalOnProperty(
        value = ["scheduler.enable"], havingValue = "true", matchIfMissing = true
    )
    //kanskje fixeddelay/fixedrate istedet for cron
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Oslo")
    private fun scheduledRepoUpdate() {
        val UUIDToDtoMap = reportsResponseToStatisticsRepo(
            "1DaysAgo",
            "today",
            ReferralEntity(),
            DateEntity()
        )

        adAnalyticsRepository.updateUUIDToDtoMap(UUIDToDtoMap)
    }
}
