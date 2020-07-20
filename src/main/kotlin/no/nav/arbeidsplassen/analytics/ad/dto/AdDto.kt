package no.nav.arbeidsplassen.analytics.ad.dto

data class AdDto(
    var sidevisninger: Int = 0,
    var average: Double = 0.0,
    var referrals: List<String> = listOf(),
    var viewsPerReferral: List<Int> = listOf()
    /*
    var viewsOverTime: Map<String, String>,
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)
