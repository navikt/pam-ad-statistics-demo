package no.nav.arbeidsplassen.analytics.ad

import no.nav.arbeidsplassen.analytics.ad.dto.AdStatisticsDto
import org.springframework.stereotype.Repository

@Repository
class AdStatisticsRepository {
    private var UUIDToAdStatisticsDtoMap = emptyMap<String, AdStatisticsDto>()

    fun updateUUIDToAdStatisticsDtoMap(UUIDToStatisticsDtoMap: Map<String, AdStatisticsDto>) {
        this.UUIDToAdStatisticsDtoMap = UUIDToStatisticsDtoMap
    }

    fun getAdStatisticsDtoFromUUID(UUID: String) = UUIDToAdStatisticsDtoMap[UUID]
}