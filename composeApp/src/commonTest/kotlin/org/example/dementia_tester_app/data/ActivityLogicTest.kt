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

        val activity = Activity(
            id = "1",
            title = "Doctor Visit",
            type = ActivityType.APPOINTMENT,
            description = "Checkup"
        )

        var result: DatabaseResult<Unit>? = null

        fake.logActivity(activity) {
            result = it
        }

        assertTrue(result is DatabaseResult.Success)
    }

    @Test
    fun `appointment history returns previously logged activities`() = runTest {
        val fake = FakeActivityService()

        val activity1 = Activity(
            id = "1",
            title = "Checkup",
            type = ActivityType.APPOINTMENT
        )

        val activity2 = Activity(
            id = "2",
            title = "Blood Test",
            type = ActivityType.TEST
        )

        fake.logActivity(activity1) {}
        fake.logActivity(activity2) {}

        val latest =
            fake.getActivitiesFlow().first()

        assertEquals(
            2,
            latest.size
        )

        assertTrue(
            latest.any {
                it.type == ActivityType.APPOINTMENT
            }
        )

        assertTrue(
            latest.any {
                it.type == ActivityType.TEST
            }
        )
    }

    @Test
    fun `today summary groups activities by type correctly`() {
        val fake =
            FakeActivityService()

        fake.logActivity(
            Activity(
                id = "1",
                type = ActivityType.REMINDER
            )
        ) {}

        fake.logActivity(
            Activity(
                id = "2",
                type = ActivityType.REMINDER
            )
        ) {}

        fake.logActivity(
            Activity(
                id = "3",
                type = ActivityType.APPOINTMENT
            )
        ) {}

        var result:
            DatabaseResult<Map<String, Int>>? = null

        fake.getTodaySummary {
            result = it
        }

        assertTrue(
            result is DatabaseResult.Success
        )

        val summary =
            (result as DatabaseResult.Success).data

        assertEquals(
            2,
            summary[ActivityType.REMINDER.value]
        )

        assertEquals(
            1,
            summary[ActivityType.APPOINTMENT.value]
        )

        assertEquals(
            3,
            summary["total"]
        )
    }

    @Test
    fun `today summary excludes activities from previous days`() {
        val fake =
            FakeActivityService()

        val yesterday =
            Clock.System.now().minus(
                1,
                kotlinx.datetime.DateTimeUnit.DAY,
                TimeZone.currentSystemDefault()
            )

        fake.logActivity(
            Activity(
                id = "1",
                type = ActivityType.REMINDER,
                timestamp = yesterday
            )
        ) {}

        fake.logActivity(
            Activity(
                id = "2",
                type = ActivityType.REMINDER
            )
        ) {}

        var result:
            DatabaseResult<Map<String, Int>>? = null

        fake.getTodaySummary {
            result = it
        }

        val summary =
            (result as DatabaseResult.Success).data

        assertEquals(
            1,
            summary[ActivityType.REMINDER.value]
        )

        assertEquals(
            1,
            summary["total"]
        )
    }

    @Test
    fun `logging activity failure returns error and does not update flow`() = runTest {
        val fake =
            FakeActivityService().apply {
                shouldSucceed = false
            }

        var result:
            DatabaseResult<Unit>? = null

        fake.logActivity(
            Activity(
                id = "1",
                type = ActivityType.TEST
            )
        ) {
            result = it
        }

        assertTrue(
            result is DatabaseResult.Error
        )

        assertEquals(
            0,
            fake.getActivitiesFlow()
                .first()
                .size
        )
    }
}