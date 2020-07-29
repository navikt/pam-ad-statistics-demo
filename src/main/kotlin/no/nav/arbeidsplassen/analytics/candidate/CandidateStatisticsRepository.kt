package no.nav.arbeidsplassen.analytics.candidate

import no.nav.arbeidsplassen.analytics.candidate.dto.CandidateStatisticsDto
import org.springframework.stereotype.Repository

@Repository
class CandidateStatisticsRepository {
    private var UUIDToCandidateStatisticsDtoMap = emptyMap<String, CandidateStatisticsDto>()

    fun updateUUIDToCandidateStatisticsDtoMap(UUIDToStatisticsDtoMap: Map<String, CandidateStatisticsDto>) {
        this.UUIDToCandidateStatisticsDtoMap = UUIDToStatisticsDtoMap
    }

    fun getCandidateStatisticsDtoFromUUID(UUID: String) = UUIDToCandidateStatisticsDtoMap[UUID]

    //debugging purposes
    fun prettyPrint() {
        UUIDToCandidateStatisticsDtoMap.forEach { (k, v) ->
            println("$k = ${v.pageViews}")
        }
    }
}
