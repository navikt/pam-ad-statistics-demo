package no.nav.arbeidsplassen.analytics.candidate

import no.nav.arbeidsplassen.analytics.candidate.dto.CandidateStatisticsDto
import no.nav.arbeidsplassen.analytics.filter.CandidateFilterStatisticsRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("candidate")
class CandidateStatisticsController(
    private val candidateStatisticsRepository: CandidateStatisticsRepository
) {
    @GetMapping
    fun getCandidateStatisticsData(
        @RequestParam(value = "candidateID", required = true) UUID: String
    ): CandidateStatisticsDto? {
        candidateStatisticsRepository.prettyPrint()
        return candidateStatisticsRepository.getCandidateStatisticsDtoFromUUID(UUID)
    }
}