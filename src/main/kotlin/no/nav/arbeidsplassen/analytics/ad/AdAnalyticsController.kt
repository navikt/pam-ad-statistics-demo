package no.nav.arbeidsplassen.analytics.ad

import no.nav.arbeidsplassen.analytics.ad.dto.AdDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("ad")
class AdAnalyticsController(
    private val adAnalyticsRepository: AdAnalyticsRepository
) {

    @GetMapping(value = ["/{UUID}"])
    fun getAdAnalyticsData(
        @PathVariable("UUID") UUID: String
    ): AdDto? {
        return adAnalyticsRepository.getDtoFromUUID(UUID)
    }
}
