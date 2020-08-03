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

    override infix fun mergeWith(other: AdStatisticsDto) =
            AdStatisticsDto(
                pageViews = this.pageViews + other.pageViews,
                averageTimeOnPage = this.averageTimeOnPage + other.averageTimeOnPage,
                referrals = this.referrals + other.referrals,
                viewsPerReferral = this.viewsPerReferral + other.viewsPerReferral,
                dates = this.dates + other.dates,
                viewsPerDate = this.viewsPerDate + other.viewsPerDate
            )
}
