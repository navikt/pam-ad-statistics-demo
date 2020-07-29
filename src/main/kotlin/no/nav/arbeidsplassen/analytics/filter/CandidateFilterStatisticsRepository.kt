package no.nav.arbeidsplassen.analytics.filter

import no.nav.arbeidsplassen.analytics.StatisticsDto
import no.nav.arbeidsplassen.analytics.filter.dto.CandidateFilterStatisticsDto
import no.nav.arbeidsplassen.analytics.filter.dto.CandidateFilterSummaryDto
import org.springframework.stereotype.Repository

@Repository
class CandidateFilterStatisticsRepository {
    private var UUIDToCandidateFilterStatisticsDtoMap = emptyMap<String, CandidateFilterStatisticsDto>()

    fun updateUUIDToCandidateFilterStatisticsDtoMap(
        UUIDToCandidateFilterStatisticsDtoMap: Map<String, CandidateFilterStatisticsDto>
    ) {
        this.UUIDToCandidateFilterStatisticsDtoMap = UUIDToCandidateFilterStatisticsDtoMap
    }

    fun getCandidateFilterStatisticsDtoFromUUID(UUID: String) = UUIDToCandidateFilterStatisticsDtoMap[UUID]

    fun getCandidateFilterSummaryDtoFromFilterName(filterName: String): StatisticsDto<CandidateFilterSummaryDto> {
        val filterNameMap = UUIDToCandidateFilterStatisticsDtoMap.filterKeys { it.startsWith(filterName) }
        var candidateFilterSummaryDto = CandidateFilterSummaryDto()
        filterNameMap.forEach { (k, v) ->
            candidateFilterSummaryDto = candidateFilterSummaryDto mergeWith toCandidateFilterSummaryDto(k, v)
        }
        return candidateFilterSummaryDto
    }

    fun toCandidateFilterSummaryDto(
        key: String,
        dto: CandidateFilterStatisticsDto
    ): CandidateFilterSummaryDto {
        return CandidateFilterSummaryDto(
            pageViews = listOf(dto.pageViews),
            filterValues = listOf(key.split("=").last())
        )
    }
}