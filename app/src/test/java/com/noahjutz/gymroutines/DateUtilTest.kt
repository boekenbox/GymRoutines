package com.noahjutz.splitfit

import com.noahjutz.gymroutines.util.currentDailyStreakInternal
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import org.junit.Test

@ExperimentalTime
class DateUtilTest {

    private val now = Calendar.getInstance().time

    private val dates5Streak = listOf(
        now,
        Date((now.time - 24.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 48.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 72.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
    )

    private val dates3StreakInterrupted = listOf(
        now,
        Date((now.time - 24.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 48.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
    )

    private val datesNoStreak = listOf(
        Date((now.time - 24.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 48.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 72.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
    )

    private val dates5StreakMultipleADay = listOf(
        now,
        Date((now.time - 24.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 24.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 48.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 48.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 72.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
    )

    private val datesNoStreak2 = listOf(
        Date((now.time - 48.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 72.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 96.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 110.hours.absoluteValue.inWholeMilliseconds).toLong()),
        Date((now.time - 134.hours.absoluteValue.inWholeMilliseconds).toLong()),
    )

    @Test
    fun `5 Day streak`() {
        val streak = dates5Streak.currentDailyStreakInternal(now)
        assertEquals(5, streak)
    }

    @Test
    fun `3 Day streak`() {
        val streak = dates5Streak.subList(0, 3).currentDailyStreakInternal(now)
        assertEquals(3, streak)
    }

    @Test
    fun `3 Day streak with fourth day seperated by gap`() {
        val streak = dates3StreakInterrupted.currentDailyStreakInternal(now)
        assertEquals(3, streak)
    }

    @Test
    fun `1 Day streak`() {
        val streak = dates5Streak.subList(0, 1).currentDailyStreakInternal(now)
        assertEquals(1, streak)
    }

    @Test
    fun `No workout today, no streak`() {
        val streak = datesNoStreak.currentDailyStreakInternal(now)
        assertEquals(0, streak)
    }

    @Test
    fun `No streak 2`() {
        val streak = datesNoStreak2.currentDailyStreakInternal(now)
        assertEquals(0, streak)
    }

    @Test
    fun `1 Day streak 2`() {
        val reference = Date(1616841912690)
        val streak = listOf(
            Date(1616713200000),
            Date(0),
            reference
        ).currentDailyStreakInternal(reference)
        assertEquals(1, streak)
    }

    @Test
    fun `Empty workout list, no streak`() {
        val streak = emptyList<Date>().currentDailyStreakInternal(now)
        assertEquals(0, streak)
    }

    @Test
    fun `5 Day streak with multiple dates per day`() {
        val streak = dates5StreakMultipleADay.currentDailyStreakInternal(now)
        assertEquals(5, streak)
    }
}
