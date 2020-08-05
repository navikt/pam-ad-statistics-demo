package no.nav.arbeidsplassen.analytics

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.services.analyticsreporting.v4.model.ReportRow
import org.junit.jupiter.api.Test

class DateEntityTest {
    val mapper = jacksonObjectMapper()

    data class Row(
        @JsonProperty("dimensions")
        val dimensions: List<String>,

        @JsonProperty("metrics")
        val metrics: List<MetricValues>
    )

    data class MetricValues(
        @JsonProperty("values")
        val values: List<String>
    )

    @Test
    fun `Test that DateEntity returns the correct DTO for a given row`() {
        val jsonDeserialized = mapper.readValue<List<Row>>(
            DateEntityTest::class.java.getResource("/TestRows.json").readText()
        )
        val reportRows = jsonDeserialized.map { row ->
            val reportRow = ReportRow()
            reportRow.set("dimensions", row.dimensions)
            reportRow.set("metrics", row.metrics)
            reportRow
        }
        val testDateDto = DateEntity()
    }

}