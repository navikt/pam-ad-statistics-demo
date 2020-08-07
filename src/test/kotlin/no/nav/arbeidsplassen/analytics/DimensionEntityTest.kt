package no.nav.arbeidsplassen.analytics

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues
import com.google.api.services.analyticsreporting.v4.model.ReportRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntityTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `Test that ReferralEntity returns the correct DTO and key for a given row`() {

        val testReportRows = mapper.readValue(
            EntityTest::class.java.getResource("/DateEntityTestRows.json"),
            object : TypeReference<List<ReportRow>>() {}
        )
        var count = 0
        testReportRows.forEach { testReportRow ->
            count += 1
            testReportRow.metrics =
                mutableListOf(mapper.convertValue(testReportRow.metrics.first(), DateRangeValues::class.java))
            val expected = DateEntity().toStatisticsDto(testReportRow)
            assertEquals(
                expected.dates,
                listOf("$count".repeat(8))
            )
            assertEquals(
                expected.viewsPerDate,
                listOf(count)
            )
            assertEquals(
                DateEntity().getKey(testReportRow),
                listOf("testpath${count}")
            )
        }
    }

    @Test
    fun `Test that DateEntity returns the correct DTO and key for a given row`() {

        val testReportRows = mapper.readValue(
            EntityTest::class.java.getResource("/DateEntityTestRows.json"),
            object : TypeReference<List<ReportRow>>() {}
        )
        var count = 0
        testReportRows.forEach { testReportRow ->
            count += 1
            testReportRow.metrics =
                mutableListOf(mapper.convertValue(testReportRow.metrics.first(), DateRangeValues::class.java))
            val expected = DateEntity().toStatisticsDto(testReportRow)
            assertEquals(
                expected.dates,
                listOf("$count".repeat(8))
            )
            assertEquals(
                expected.viewsPerDate,
                listOf(count)
            )
            assertEquals(
                DateEntity().getKey(testReportRow),
                listOf("testpath${count}")
            )
        }
    }

    @Test
    fun `Test that CandidateEntity returns the correct DTO and key for a given row`() {

        val testReportRows = mapper.readValue(
            EntityTest::class.java.getResource("/CandidateEntityTestRows.json"),
            object : TypeReference<List<ReportRow>>() {}
        )
        var count = 0
        testReportRows.forEach { testReportRow ->
            count += 1
            testReportRow.metrics =
                mutableListOf(mapper.convertValue(testReportRow.metrics.first(), DateRangeValues::class.java))
            val expected = CandidateEntity().toStatisticsDto(testReportRow)
            assertEquals(
                expected.pageViews,
                count
            )
            assertEquals(
                DateEntity().getKey(testReportRow),
                listOf("testpath${count}")
            )
        }
    }

    @Test
    fun `Test that CandidateShortlistEntity returns the correct DTO and key for a given row`() {

        val testReportRows = mapper.readValue(
            EntityTest::class.java.getResource("/CandidateEntityTestRows.json"),
            object : TypeReference<List<ReportRow>>() {}
        )
        var count = 0
        testReportRows.forEach { testReportRow ->
            count += 1
            testReportRow.metrics =
                mutableListOf(mapper.convertValue(testReportRow.metrics.first(), DateRangeValues::class.java))
            val expected = CandidateShortlistEntity().toStatisticsDto(testReportRow)
            assertEquals(
                expected.pageViewsFromShortlist,
                count
            )
            assertEquals(
                DateEntity().getKey(testReportRow),
                listOf("testpath${count}")
            )
        }
    }

    @Test
    fun `Test that CandidateFilterEntity returns the correct DTO and key for a given row`() {

        val testReportRows = mapper.readValue(
            EntityTest::class.java.getResource("/CandidateFilterEntityTestRows.json"),
            object : TypeReference<List<ReportRow>>() {}
        )
        testReportRows.first().apply {
            this.metrics =
            mutableListOf(mapper.convertValue(this.metrics.first(), DateRangeValues::class.java))
            val expected = CandidateFilterEntity().toStatisticsDto(this)
            assertEquals(
                expected.pageViews,
                1
            )
            assertEquals(
                CandidateFilterEntity().getKey(this),
                listOf("kompetanser=2 2", "geografilist=3")
            )
        }
        testReportRows.last().apply {
            this.metrics =
                mutableListOf(mapper.convertValue(this.metrics.first(), DateRangeValues::class.java))
            val expected = CandidateFilterEntity().toStatisticsDto(this)
            assertEquals(
                expected.pageViews,
                2
            )
            assertEquals(
                CandidateFilterEntity().getKey(this),
                listOf("kompetanser=2 2", "kompetanser=4", "geografilist=6")
            )
        }
    }
}