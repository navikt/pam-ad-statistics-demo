package no.nav.arbeidsplassen.analytics.filter

import no.nav.arbeidsplassen.analytics.filter.dto.CandidateFilterStatisticsDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("filter")
class CandidateFilterStatisticsController(
    private val candidateFilterStatisticsRepository: CandidateFilterStatisticsRepository
) {
    @GetMapping
    fun getCandidateFilterData(
        @RequestParam(value = "filterName", required = true) filterName: String,
        @RequestParam(value = "filterValue", required = false) filterValue: String
    ): CandidateFilterStatisticsDto? {
        candidateFilterStatisticsRepository.prettyPrint()
        val UUID = "${filterName.toLowerCase()}=${filterValue.toLowerCase()}"
        return candidateFilterStatisticsRepository.getCandidateStatisticsDtoFromUUID(UUID)
    }
}