package no.nav.arbeidsplassen.placeholder

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AnalyticsController(
    private val googleDemo: GoogleDemo
) {

    @GetMapping(value = ["/{stillingspath}"], produces = ["application/json"])
    fun getAdAnalyticsData(
        @PathVariable("stillingspath") path: String
    ): AdDto? {
        return googleDemo.returnStilling(path)
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