package no.nav.arbeidsplassen.analytics.candidate

import no.nav.arbeidsplassen.analytics.candidate.dto.CandidateStatisticsDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("candidate")
class CandidateStatisticsController(
    private val candidateStatisticsRepository: CandidateStatisticsRepository
) {
    @GetMapping(value = ["/{UUID}"])
    fun getCandidateStatisticsData(
        @PathVariable("UUID") UUID: String
    ): CandidateStatisticsDto? {
        println(candidateStatisticsRepository.UUIDToCandidateStatisticsDtoMap)
        return candidateStatisticsRepository.getCandidateStatisticsDtoFromUUID(UUID)
    }
}