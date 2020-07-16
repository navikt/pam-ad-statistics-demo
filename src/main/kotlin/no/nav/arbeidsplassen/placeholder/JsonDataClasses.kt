package no.nav.arbeidsplassen.placeholder

data class Stilling(
    var tittel: String,
    var sidevisninger: String,
    var avg: String,
    var referrals: List<String>
    /*
    var date: Map<String, String>,
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)
