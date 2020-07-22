package no.nav.arbeidsplassen.analytics.ad

import no.nav.arbeidsplassen.analytics.ad.dto.AdDto
import org.springframework.stereotype.Repository

@Repository
class AdAnalyticsRepository {
    private var UUIDToDtoMap = emptyMap<String, AdDto>()

    fun updateUUIDToDtoMap(UUIDToDtoMap: Map<String, AdDto>) {
        this.UUIDToDtoMap = UUIDToDtoMap
    }

    fun getDtoFromUUID(UUID: String) = UUIDToDtoMap[UUID]
}