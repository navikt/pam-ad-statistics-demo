package no.nav.arbeidsplassen.analytics.ad

import no.nav.arbeidsplassen.analytics.ad.dto.AdStatisticsDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("ad")
class AdStatisticsController(
    private val adStatisticsRepository: AdStatisticsRepository
) {

    @GetMapping(value = ["/{UUID}"])
    fun getAdStatisticsData(
        @PathVariable("UUID") UUID: String
    ): AdStatisticsDto? {
        return adStatisticsRepository.getAdStatisticsDtoFromUUID(UUID)
    }
}
