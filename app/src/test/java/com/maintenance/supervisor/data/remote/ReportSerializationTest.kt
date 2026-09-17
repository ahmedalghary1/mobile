package com.maintenance.supervisor.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSerializationTest {
    @Test fun `report uses API snake case fields`() {
        val report = ReportInput("6ba7b810-9dad-11d1-80b4-00c04fd430c8", 4, "2026-09-17", "2026-09-17T08:00:00Z", "2026-09-17T09:00:00Z", "2026-09-17T09:00:00Z", listOf(AnswerInput(7, true, "")))
        val json = Json.encodeToString(report)
        assertTrue(json.contains("\"client_report_id\"")); assertTrue(json.contains("\"checklist_item_id\""))
    }
}
