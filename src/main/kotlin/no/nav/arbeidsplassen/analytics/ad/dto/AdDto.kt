package no.nav.arbeidsplassen.analytics.ad.dto

data class AdDto(
    var sidevisninger: Int = 0,
    //leaving the actual calculation to frontend
    var average: List<Double> = listOf(),
    var referrals: List<String> = listOf(),
    var viewsPerReferral: List<Int> = listOf()
    /*
    var viewsOverTime: Map<String, String>,
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)
