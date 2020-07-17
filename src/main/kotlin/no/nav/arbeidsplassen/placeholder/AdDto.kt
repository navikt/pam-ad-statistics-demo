package no.nav.arbeidsplassen.placeholder

data class AdDto(
    var tittel: String = "",
    var sidevisninger: Int = 0,
    var avg: MutableList<Double> = mutableListOf(),
    var referrals: MutableMap<String, Int> = mutableMapOf()
    /*
    var viewsOverTime: Map<String, String>,
    var region: Map<String, String>,
    var device: Map<String, String>

     */
)
