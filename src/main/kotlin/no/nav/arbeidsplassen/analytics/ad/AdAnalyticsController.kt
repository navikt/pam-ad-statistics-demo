package no.nav.arbeidsplassen.analytics.ad

import no.nav.arbeidsplassen.analytics.ad.dto.AdDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("ad")
class AdAnalyticsController(
    private val googleAnalyticsService: GoogleAnalyticsService
) {

    @GetMapping(value = ["{UUID}"])
    fun getAdAnalyticsData(
        @PathVariable("UUID") UUID: String
    ): AdDto? {
        return googleAnalyticsService.fetchAnalyticsByAdId(UUID)
    }

    /*
    @GetMapping("/internal/isAlive")
    fun isAlive(): ResponseEntity<String> =
        ResponseEntity("OK", HttpStatus.OK)

    @GetMapping("/internal/isReady")
    fun isReady(): ResponseEntity<String> =
        ResponseEntity("OK", HttpStatus.OK)

     */

}
