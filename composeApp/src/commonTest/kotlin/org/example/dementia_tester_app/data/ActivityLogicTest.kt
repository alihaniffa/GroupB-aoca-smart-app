package org.example.dementia_tester_app.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityLogicTest {

    @Test
    fun `logging an appointment activity succeeds`() {
        val fake = FakeActivityService()
        val activity = Activity(id = "1", title = "Doctor Visit", type = "appointment", description = "Checkup")
        var result: DatabaseResult<Unit>? = null

        fake.logActivity(activity) { result = it }

        assertTrue(result is DatabaseResult.Success)
    }

    @Test
    fun `appointment history returns previously logged activities`() = runTest {
        val fake = FakeActivityService()
        val activity1 = Activity(id = "1", title = "Checkup", type = "appointment")
        val activity2 = Activity(id = "2", title = "Blood Test", type = "test")

        fake.logActivity(activity1) {}
        fake.logActivity(activity2) {}

        val latest = fake.getActivitiesFlow().first()

        assertEquals(2, latest.size)
        assertTrue(latest.any { it.type == "appointment" })
        assertTrue(latest.any { it.type == "test" })
    }

    @Test
    fun `today summary groups activities by type correctly`() {
        val fake = FakeActivityService()
        fake.logActivity(Activity(id = "1", type = "reminder")) {}
        fake.logActivity(Activity(id = "2", type = "reminder")) {}
        fake.logActivity(Activity(id = "3", type = "appointment")) {}

        var result: DatabaseResult<Map<String, Int>>? = null
        fake.getTodaySummary { result = it }

        assertTrue(result is DatabaseResult.Success)
        val summary = (result as DatabaseResult.Success).data
        assertEquals(2, summary["reminder"])
        assertEquals(1, summary["appointment"])
    }

    @Test
    fun `today summary excludes activities from previous days`() {
        val fake = FakeActivityService()
        val yesterday = Clock.System.now().minus(1, kotlinx.datetime.DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        fake.logActivity(Activity(id = "1", type = "reminder", timestamp = yesterday)) {}
        fake.logActivity(Activity(id = "2", type = "reminder")) {}

        var result: DatabaseResult<Map<String, Int>>? = null
        fake.getTodaySummary { result = it }

        val summary = (result as DatabaseResult.Success).data
        assertEquals(1, summary["reminder"])
    }

    @Test
    fun `logging activity failure returns error and does not update flow`() = runTest {
        val fake = FakeActivityService().apply { shouldSucceed = false }
        var result: DatabaseResult<Unit>? = null

        fake.logActivity(Activity(id = "1", type = "test")) { result = it }

        assertTrue(result is DatabaseResult.Error)
        assertEquals(0, fake.getActivitiesFlow().first().size)
    }
}