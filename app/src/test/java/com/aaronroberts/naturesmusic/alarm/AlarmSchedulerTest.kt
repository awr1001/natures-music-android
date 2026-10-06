package com.aaronroberts.naturesmusic.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class AlarmSchedulerTest {
    @Test
    fun pickerCalendarKeepsChosenDay() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 29, 22, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val laterSameDay = AlarmScheduler.triggerCalendar(2026, Calendar.AUGUST, 29, 23, 0, now)
        val earlierSameDay = AlarmScheduler.triggerCalendar(2026, Calendar.AUGUST, 29, 7, 0, now)
        assertEquals(29, laterSameDay.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, laterSameDay.get(Calendar.HOUR_OF_DAY))
        assertEquals(29, earlierSameDay.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, earlierSameDay.get(Calendar.HOUR_OF_DAY))
        assertTrue(laterSameDay.timeInMillis > now.timeInMillis)
        assertTrue(earlierSameDay.timeInMillis < now.timeInMillis)
    }

    @Test
    fun pastDatetimeDoesNotRollForwardADay() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 29, 22, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val trigger = AlarmScheduler.triggerCalendar(2026, Calendar.AUGUST, 29, 22, 30, now = now)
        assertEquals(2026, trigger.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, trigger.get(Calendar.MONTH))
        assertEquals(29, trigger.get(Calendar.DAY_OF_MONTH))
        assertEquals(22, trigger.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, trigger.get(Calendar.MINUTE))
    }

    @Test
    fun backupSchedulesFuturePickerAsIs() {
        val now = cal(2026, Calendar.AUGUST, 29, 22, 30, 0)
        val picker = cal(2026, Calendar.AUGUST, 29, 22, 32, 0)
        assertEquals(
            picker.timeInMillis,
            AlarmScheduler.backupTriggerMillis(picker.timeInMillis, now.timeInMillis),
        )
    }

    @Test
    fun backupSameDisplayedMinuteSchedulesOneSecondFromNow() {
        val now = cal(2026, Calendar.AUGUST, 29, 22, 30, 45)
        val picker = cal(2026, Calendar.AUGUST, 29, 22, 30, 0)
        assertEquals(
            now.timeInMillis + 1_000L,
            AlarmScheduler.backupTriggerMillis(picker.timeInMillis, now.timeInMillis),
        )
    }

    @Test
    fun backupWithinGraceSchedulesOneSecondFromNow() {
        // Picker is aligned to :00, so any gap under 60s is still the same displayed
        // minute (covered above). The grace window only matters once the clock has
        // rolled into the next minute: 22:31:00 is 60s after a 22:30 picker.
        val now = cal(2026, Calendar.AUGUST, 29, 22, 31, 0)
        val picker = cal(2026, Calendar.AUGUST, 29, 22, 30, 0)
        val delta = now.timeInMillis - picker.timeInMillis
        assertTrue(now.get(Calendar.MINUTE) != picker.get(Calendar.MINUTE))
        assertTrue(delta <= AlarmScheduler.SAME_MINUTE_GRACE_MS)
        assertEquals(
            now.timeInMillis + 1_000L,
            AlarmScheduler.backupTriggerMillis(picker.timeInMillis, now.timeInMillis),
        )
    }

    @Test
    fun backupClearlyPastDoesNotJumpADay() {
        val now = cal(2026, Calendar.AUGUST, 29, 22, 30, 0)
        val picker = cal(2026, Calendar.AUGUST, 29, 7, 0, 0)
        assertNull(AlarmScheduler.backupTriggerMillis(picker.timeInMillis, now.timeInMillis))
        val yesterday = cal(2026, Calendar.AUGUST, 28, 22, 30, 0)
        assertNull(AlarmScheduler.backupTriggerMillis(yesterday.timeInMillis, now.timeInMillis))
    }

    @Test
    fun formatDateTimeMatchesCurrentTimeOnSameMinute() {
        val now = cal(2026, Calendar.AUGUST, 29, 22, 59, 37)
        val labelFromDate = AlarmStore.formatDateTime(now.time)
        val labelFromFields = AlarmStore.formatDateTime(
            2026,
            Calendar.AUGUST,
            29,
            22,
            59,
        )
        assertEquals(labelFromDate, labelFromFields)
        val laterSecond = (now.clone() as Calendar).apply { set(Calendar.SECOND, 5) }
        assertEquals(labelFromDate, AlarmStore.formatDateTime(laterSecond.time))
        val nextMinute = (now.clone() as Calendar).apply { add(Calendar.MINUTE, 1) }
        assertTrue(labelFromDate != AlarmStore.formatDateTime(Date(nextMinute.timeInMillis)))
        assertNotNull(labelFromDate)
    }

    @Test
    fun snoozePlusStepsThenJumpsByFive() {
        assertEquals(2, AlarmStore.nextSnoozeMinutes(1, plus = true))
        assertEquals(5, AlarmStore.nextSnoozeMinutes(4, plus = true))
        assertEquals(10, AlarmStore.nextSnoozeMinutes(5, plus = true))
        assertEquals(15, AlarmStore.nextSnoozeMinutes(10, plus = true))
    }

    @Test
    fun snoozeMinusStepsThenJumpsByFive() {
        assertEquals(1, AlarmStore.nextSnoozeMinutes(1, plus = false))
        assertEquals(4, AlarmStore.nextSnoozeMinutes(5, plus = false))
        assertEquals(1, AlarmStore.nextSnoozeMinutes(2, plus = false))
        assertEquals(5, AlarmStore.nextSnoozeMinutes(10, plus = false))
    }

    private fun cal(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Calendar {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
