package no.nav.arbeidsplassen.analytics

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues
import com.google.api.services.analyticsreporting.v4.model.ReportRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntityTest {

    val mapper = jacksonObjectMapper()

    @Test
    fun `Test that DateEntity returns the correct DTO for a given row`() {

        val testReportRow = mapper.readValue(
            EntityTest::class.java.getResource("/TestRows.json"),
            ReportRow::class.java
        )
        testReportRow.metrics = mutableListOf(mapper.convertValue(testReportRow.metrics.first(), DateRangeValues::class.java))
        val expected = DateEntity().toStatisticsDto(testReportRow)
        assertEquals(
            expected.dates,
            listOf("27081997")
        )
        assertEquals(
            expected.viewsPerDate,
            listOf(2)
        )
    }
}