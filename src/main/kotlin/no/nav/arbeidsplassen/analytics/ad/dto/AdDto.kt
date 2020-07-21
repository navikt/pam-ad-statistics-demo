package no.nav.arbeidsplassen.analytics.ad.dto

import com.google.api.services.analyticsreporting.v4.model.ReportRow

data class AdDto(
    var sidevisninger: Int = 0,
    //leaving the actual calculation to frontend
    var average: List<Double> = listOf(),
    var referrals: List<String> = listOf(),
    var viewsPerReferral: List<Int> = listOf(),
    var viewsOverTime: Map<String, String>
    /*
    var region: Map<String, String>,
    var device: Map<String, String>
     */
)

//kanskje ha annet sted
abstract class DimensionEntity {
    val dimensions: List<String>
    val metrics: List<String>

    constructor(row: ReportRow) {
        dimensions = row.dimensions
        metrics = row.getMetric()
    }

    abstract fun toAdDto()
}

private fun ReportRow.getMetric() = metrics.first().getValues()

class ReferralEntity(row: ReportRow): DimensionEntity(row) {

    //ikke sikker på return eller dto-variabel
    override fun toAdDto() {
        val(

        )
    }
}