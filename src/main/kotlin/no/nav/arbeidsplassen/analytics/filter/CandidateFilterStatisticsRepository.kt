package no.nav.arbeidsplassen.analytics.filter

import no.nav.arbeidsplassen.analytics.filter.dto.CandidateFilterStatisticsDto
import org.springframework.stereotype.Repository

@Repository
class CandidateFilterStatisticsRepository {
    var UUIDToCandidateFilterStatisticsDtoMap = emptyMap<String, CandidateFilterStatisticsDto>()

    fun updateUUIDToCandidateFilterStatisticsDtoMap(UUIDToCandidateFilterStatisticsDtoMap: Map<String, CandidateFilterStatisticsDto>) {
        this.UUIDToCandidateFilterStatisticsDtoMap = UUIDToCandidateFilterStatisticsDtoMap
    }

    fun getCandidateStatisticsDtoFromUUID(UUID: String) = UUIDToCandidateFilterStatisticsDtoMap[UUID]

    //debugging purposes
    fun prettyPrint() {
        UUIDToCandidateFilterStatisticsDtoMap.forEach{ (k, v) ->
            println("$k = ${v.pageViews}")
        }
    }
}