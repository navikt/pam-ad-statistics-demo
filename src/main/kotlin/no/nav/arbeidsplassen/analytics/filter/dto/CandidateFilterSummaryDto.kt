package no.nav.arbeidsplassen.analytics.filter.dto

import no.nav.arbeidsplassen.analytics.StatisticsDto

class CandidateFilterSummaryDto(
    var pageViews: List<Int> = listOf(),
    var filterValues: List<String> = listOf()
) : StatisticsDto<CandidateFilterSummaryDto> {
    override infix fun mergeWith(other: CandidateFilterSummaryDto?): CandidateFilterSummaryDto {
        return other?.let {
            CandidateFilterSummaryDto(
                pageViews = this.pageViews + other.pageViews,
                filterValues = this.filterValues + other.filterValues
            )
        } ?: this
    }
}