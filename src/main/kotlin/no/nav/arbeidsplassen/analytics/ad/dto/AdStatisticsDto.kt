package no.nav.arbeidsplassen.analytics.ad.dto

import no.nav.arbeidsplassen.analytics.StatisticsDto

class AdStatisticsDto(
    var pageViews: Int = 0,
    var averageTimeOnPage: List<Double> = listOf(),
    var referrals: List<String> = listOf(),
    var viewsPerReferral: List<Int> = listOf(),
    var dates: List<String> = listOf(),
    var viewsPerDate: List<Int> = listOf()
) : StatisticsDto<AdStatisticsDto> {
    override infix fun mergeWith(other: AdStatisticsDto?): AdStatisticsDto {
        return other?.let {
            AdStatisticsDto(
                pageViews = it.pageViews + this.pageViews,
                averageTimeOnPage = it.averageTimeOnPage + this.averageTimeOnPage,
                referrals = it.referrals + this.referrals,
                viewsPerReferral = it.viewsPerReferral + this.viewsPerReferral,
                dates = it.dates + this.dates,
                viewsPerDate = it.viewsPerDate + this.viewsPerDate
            )
        } ?: this
    }
}
