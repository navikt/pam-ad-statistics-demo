package no.nav.arbeidsplassen.analytics

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("internal")
class GetStatusController {

    @GetMapping("/isAlive")
    fun isAlive(): ResponseEntity<String> =
        ResponseEntity("OK", HttpStatus.OK)

    @GetMapping("/isReady")
    fun isReady(): ResponseEntity<String> =
        ResponseEntity("OK", HttpStatus.OK)
}