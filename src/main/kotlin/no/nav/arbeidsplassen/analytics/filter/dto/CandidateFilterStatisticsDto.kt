package no.nav.arbeidsplassen.analytics.filter.dto

import no.nav.arbeidsplassen.analytics.StatisticsDto

class CandidateFilterStatisticsDto(
    var pageViews: Int = 0
) : StatisticsDto<CandidateFilterStatisticsDto> {
    override infix fun mergeWith(other: CandidateFilterStatisticsDto?): CandidateFilterStatisticsDto {
        return other?.let {
            CandidateFilterStatisticsDto(
                pageViews = it.pageViews + this.pageViews
            )
        } ?: this
    }
}