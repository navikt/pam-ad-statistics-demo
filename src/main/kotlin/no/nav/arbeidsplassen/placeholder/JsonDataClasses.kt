package no.nav.arbeidsplassen.placeholder

data class Stilling(
    var tittel: String,
    var sidevisninger: Int?,
    var avg: MutableList<Double>,
    var referrals: MutableMap<String, Int>
    /*
    var date: Map<String, String>,
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)
