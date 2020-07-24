package no.nav.arbeidsplassen.analytics.ad.dto

data class AdDto(
    var pageViews: Int = 0,
    //leaving the actual calculation to frontend
    var averageTimeOnPage: List<Double> = listOf(),
    var referrals: List<String> = listOf(),
    var viewsPerReferral: List<Int> = listOf(),
    var dates: List<String> = listOf(),
    var viewsPerDate: List<Int> = listOf()
    /*
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)
