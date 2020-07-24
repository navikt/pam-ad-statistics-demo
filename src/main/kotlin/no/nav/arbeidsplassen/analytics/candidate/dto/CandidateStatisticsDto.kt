package no.nav.arbeidsplassen.analytics.candidate.dto

import no.nav.arbeidsplassen.analytics.StatisticsDto

class CandidateStatisticsDto(
    var pageViews: Int = 0
) : StatisticsDto<CandidateStatisticsDto> {
    override infix fun mergeWith(other: CandidateStatisticsDto?): CandidateStatisticsDto {
        return other?.let {
            CandidateStatisticsDto(
                pageViews = it.pageViews + this.pageViews
            )
        } ?: this
    }
}