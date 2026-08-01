package uz.alphazet.domain.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.alphazet.data.models.ShopData
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * JVM-only tests for the pickup-slot rules. Everything is driven off a fixed `now` so the
 * assertions stay stable, and the weekday names are derived from that instant rather than
 * hardcoded so the suite doesn't quietly rot if the anchor date changes.
 */
class PickupTimeTest {

    /** Thursday 30 July 2026, 12:00 on the shop's clock. */
    private val now: Long = ZonedDateTime
        .of(2026, 7, 30, 12, 0, 0, 0, PickupTime.SHOP_ZONE)
        .toInstant()
        .toEpochMilli()

    private val minute = 60_000L
    private val hour = 60 * minute

    // --- wire format ------------------------------------------------------

    @Test
    fun format_emits_rfc3339_with_the_shop_offset() {
        val at = at(2026, 7, 30, 17, 30)

        assertEquals("2026-07-30T17:30:00+05:00", PickupTime.format(at))
    }

    @Test
    fun parse_round_trips_what_format_produced() {
        val at = at(2026, 7, 30, 17, 30)

        assertEquals(at, PickupTime.parse(PickupTime.format(at)))
    }

    @Test
    fun parse_accepts_a_utc_instant() {
        assertEquals(at(2026, 7, 30, 17, 30), PickupTime.parse("2026-07-30T12:30:00Z"))
    }

    @Test
    fun parse_returns_null_for_absent_or_unusable_values() {
        assertNull(PickupTime.parse(null))
        assertNull(PickupTime.parse(""))
        assertNull(PickupTime.parse("   "))
        assertNull(PickupTime.parse("17:30"))
        assertNull(PickupTime.parse("2026-07-30T17:30:00")) // no offset — ambiguous
        assertNull(PickupTime.parse("not a timestamp"))
    }

    // --- bounds -----------------------------------------------------------

    @Test
    fun earliest_slot_clears_the_lead_time_and_lands_on_a_step_boundary() {
        // 12:03 + 10 min = 12:13, which rounds up to 12:15.
        val earliest = PickupTime.earliestSlot(now + 3 * minute)

        assertEquals(at(2026, 7, 30, 12, 15), earliest)
        assertTrue(earliest >= now + 3 * minute + PickupTime.MIN_LEAD_MINUTES * minute)
    }

    @Test
    fun earliest_slot_stays_put_when_the_lead_lands_exactly_on_a_step() {
        // 12:05 + 10 min = 12:15 exactly — already a boundary, so no extra wait is imposed.
        assertEquals(at(2026, 7, 30, 12, 15), PickupTime.earliestSlot(now + 5 * minute))
    }

    @Test
    fun latest_slot_is_24_hours_out() {
        assertEquals(at(2026, 7, 31, 12, 0), PickupTime.latestSlot(now))
    }

    // --- slot generation --------------------------------------------------

    @Test
    fun slots_without_a_schedule_only_apply_the_time_bounds() {
        val slots = PickupTime.slots(workingHours = null, nowMillis = now)

        assertTrue(slots.isNotEmpty())
        assertTrue(slots.all { it.epochMillis >= now + PickupTime.MIN_LEAD_MINUTES * minute })
        assertTrue(slots.all { it.epochMillis <= now + PickupTime.MAX_AHEAD_HOURS * hour })
        assertEquals(at(2026, 7, 30, 12, 15), slots.first().epochMillis)
    }

    @Test
    fun slots_are_spaced_by_the_step_and_never_repeat() {
        val slots = PickupTime.slots(workingHours = emptyList(), nowMillis = now)

        assertEquals(slots.map { it.epochMillis }.distinct().size, slots.size)
        slots.zipWithNext { a, b ->
            assertEquals(PickupTime.SLOT_STEP_MINUTES * minute, b.epochMillis - a.epochMillis)
        }
    }

    @Test
    fun slots_stay_inside_a_daytime_window() {
        val slots = PickupTime.slots(everyDay("09:00", "22:00"), now)

        assertTrue(slots.isNotEmpty())
        assertTrue(slots.all { it.time >= "09:00" && it.time < "22:00" })
        // The window closes at 22:00, so the last slot of today is 21:45.
        assertEquals(at(2026, 7, 30, 21, 45), slots.last { it.dayOffset == 0 }.epochMillis)
    }

    @Test
    fun slots_label_today_and_tomorrow_relative_to_the_shop_clock() {
        val slots = PickupTime.slots(everyDay("09:00", "22:00"), now)

        assertTrue(slots.any { it.dayOffset == 0 })
        assertTrue(slots.any { it.dayOffset == 1 })
        assertTrue(slots.all { it.dayOffset == 0 || it.dayOffset == 1 })
        // Tomorrow reopens at 09:00, not at midnight.
        assertEquals(at(2026, 7, 31, 9, 0), slots.first { it.dayOffset == 1 }.epochMillis)
    }

    @Test
    fun overnight_windows_carry_past_midnight_into_the_next_day() {
        val slots = PickupTime.slots(everyDay("18:00", "02:00"), now)

        // Nothing before the shop opens this evening.
        assertEquals(at(2026, 7, 30, 18, 0), slots.first().epochMillis)
        // The tail of tonight's window belongs to tomorrow's date but Thursday's entry.
        assertTrue(slots.any { it.epochMillis == at(2026, 7, 31, 1, 45) })
        // ...and it stops at 02:00 rather than running through the morning.
        assertFalse(slots.any { it.epochMillis == at(2026, 7, 31, 2, 15) })
    }

    @Test
    fun a_day_the_shop_is_closed_contributes_no_slots() {
        // Only Friday is open, so nothing today (Thursday) before midnight.
        val slots = PickupTime.slots(listOf(dayAfterToday("09:00", "22:00")), now)

        assertTrue(slots.all { it.dayOffset == 1 })
        assertEquals(at(2026, 7, 31, 9, 0), slots.first().epochMillis)
    }

    @Test
    fun a_schedule_with_no_matching_weekday_yields_nothing() {
        val slots = PickupTime.slots(listOf(WorkHour("someday", "09:00", "22:00")), now)

        assertTrue(slots.isEmpty())
    }

    @Test
    fun equal_open_and_close_reads_as_open_around_the_clock() {
        // Treating this as "closed" would lock the customer out of a window the backend
        // would happily accept, so the permissive reading wins.
        val slots = PickupTime.slots(everyDay("00:00", "00:00"), now)

        assertEquals(at(2026, 7, 30, 12, 15), slots.first().epochMillis)
        assertEquals(at(2026, 7, 31, 12, 0), slots.last().epochMillis)
    }

    @Test
    fun unparseable_schedule_values_disable_that_day_rather_than_crashing() {
        val slots = PickupTime.slots(everyDay("open", "close"), now)

        assertTrue(slots.isEmpty())
    }

    // --- re-validation before submit --------------------------------------

    @Test
    fun a_slot_still_inside_every_bound_is_selectable() {
        assertTrue(
            PickupTime.isSelectable(at(2026, 7, 30, 17, 30), everyDay("09:00", "22:00"), now)
        )
    }

    @Test
    fun a_slot_that_slipped_inside_the_lead_window_is_no_longer_selectable() {
        val slot = at(2026, 7, 30, 12, 15)

        assertTrue(PickupTime.isSelectable(slot, null, now))
        // Nine minutes later the same slot is inside the 10-minute lead time.
        assertFalse(PickupTime.isSelectable(slot, null, now + 6 * minute))
    }

    @Test
    fun a_slot_beyond_the_24_hour_horizon_is_not_selectable() {
        assertFalse(PickupTime.isSelectable(now + 25 * hour, null, now))
    }

    @Test
    fun a_slot_outside_working_hours_is_not_selectable() {
        assertFalse(
            PickupTime.isSelectable(at(2026, 7, 31, 7, 0), everyDay("09:00", "22:00"), now)
        )
    }

    // --- display ----------------------------------------------------------

    @Test
    fun display_helpers_read_the_shop_clock() {
        val at = at(2026, 7, 30, 17, 30)

        assertEquals("17:30", PickupTime.formatForDisplay(at))
        assertEquals("30 July, 17:30", PickupTime.formatDayTimeForDisplay(at, Locale.ENGLISH))
    }

    @Test
    fun day_offset_counts_days_on_the_shop_clock() {
        assertEquals(0, PickupTime.dayOffset(at(2026, 7, 30, 23, 45), now))
        assertEquals(1, PickupTime.dayOffset(at(2026, 7, 31, 0, 15), now))
    }

    // --- helpers ----------------------------------------------------------

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, PickupTime.SHOP_ZONE)
            .toInstant()
            .toEpochMilli()

    private fun WorkHour(weekDay: String, openAt: String, closeAt: String) =
        ShopData.WorkHour(weekDay = weekDay, closeAt = closeAt, openAt = openAt)

    private fun everyDay(openAt: String, closeAt: String): List<ShopData.WorkHour> =
        DayOfWeek.entries.map {
            WorkHour(it.getDisplayName(TextStyle.FULL, Locale.ENGLISH), openAt, closeAt)
        }

    /** A single entry for the day after the anchor instant, on the shop's clock. */
    private fun dayAfterToday(openAt: String, closeAt: String): ShopData.WorkHour {
        val day = Instant.ofEpochMilli(now)
            .atZone(PickupTime.SHOP_ZONE)
            .plusDays(1)
            .dayOfWeek
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return WorkHour(day, openAt, closeAt)
    }
}
