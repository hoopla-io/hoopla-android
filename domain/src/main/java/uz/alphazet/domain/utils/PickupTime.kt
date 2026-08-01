package uz.alphazet.domain.utils

import uz.alphazet.data.models.ShopData
import uz.alphazet.data.rv.BaseItem
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * One selectable pickup slot. [epochMillis] is the absolute instant that gets sent to the
 * backend; [time] and [dayOffset] describe how it reads on the shop's own clock, which is
 * what the customer is actually agreeing to ("I'll be there at 17:30").
 */
data class PickupSlot(
    val epochMillis: Long,
    /** 0 = today on the shop's clock, 1 = tomorrow. Never more, the horizon is 24 hours. */
    val dayOffset: Int,
    /** "HH:mm" on the shop's clock. */
    val time: String
) : BaseItem {
    override val uniqueId: String
        get() = epochMillis.toString()
}

/**
 * Everything the client needs to offer a scheduled pickup instead of the default ASAP order.
 *
 * The backend does the authoritative timezone-aware validation; this only pre-constrains the
 * picker so the customer rarely has to round-trip to find out a time doesn't work. Slots are
 * computed on the shop's clock ([SHOP_ZONE]) because that is the timezone shops configure
 * their working hours in, and [format] emits an absolute instant with an explicit offset so
 * the comparison is unambiguous regardless of where the device is.
 */
object PickupTime {

    /** Shops configure working hours on this clock; the backend compares against it too. */
    val SHOP_ZONE: ZoneId = ZoneId.of("Asia/Tashkent")

    /** The backend rejects anything sooner — it gives the shop minimal time to prepare. */
    const val MIN_LEAD_MINUTES = 10L

    /** The backend rejects anything further out. */
    const val MAX_AHEAD_HOURS = 24L

    /** Granularity offered in the picker. */
    const val SLOT_STEP_MINUTES = 15L

    /** RFC3339 with an explicit offset, e.g. `2026-07-30T17:30:00+05:00`. */
    private val WIRE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

    private val SLOT_LABEL_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /**
     * Serialises [epochMillis] for the `pickup_at` request field. Always carries an offset, so
     * the backend never has to guess which timezone a bare wall-clock time belonged to.
     */
    fun format(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(SHOP_ZONE).format(WIRE_FORMAT)

    /**
     * Reads a `pickupAt` / `pickup_at` value coming back from the API. Accepts anything with an
     * offset or a `Z`; returns null for absent, blank or unparseable values so display code can
     * simply hide the row rather than crash on an unexpected shape.
     */
    fun parse(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(raw.trim())).toEpochMilli()
        } catch (e: Exception) {
            try {
                Instant.parse(raw.trim()).toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }
    }

    /** "HH:mm" on the shop's clock — how a pickup time is shown to the customer. */
    fun formatForDisplay(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(SHOP_ZONE).format(SLOT_LABEL_FORMAT)

    /**
     * "d MMMM, HH:mm" on the shop's clock, for order history and order detail where the
     * pickup can be days old and "today / tomorrow" no longer means anything.
     */
    fun formatDayTimeForDisplay(
        epochMillis: Long,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = DateTimeFormatter.ofPattern("d MMMM, HH:mm", locale)
        val text = Instant.ofEpochMilli(epochMillis).atZone(SHOP_ZONE).format(formatter)
        return text.replaceFirstChar { it.uppercaseChar() }
    }

    /** Days between today and [epochMillis] on the shop's clock: 0 today, 1 tomorrow. */
    fun dayOffset(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
        val day = Instant.ofEpochMilli(epochMillis).atZone(SHOP_ZONE).toLocalDate()
        return (day.toEpochDay() - shopToday(nowMillis).toEpochDay()).toInt()
    }

    /** The earliest instant the backend will accept, rounded up to the next selectable slot. */
    fun earliestSlot(nowMillis: Long = System.currentTimeMillis()): Long {
        val earliest = Instant.ofEpochMilli(nowMillis).plusSeconds(MIN_LEAD_MINUTES * 60)
        return roundUpToStep(earliest.atZone(SHOP_ZONE)).toInstant().toEpochMilli()
    }

    /** The latest instant the backend will accept. */
    fun latestSlot(nowMillis: Long = System.currentTimeMillis()): Long =
        Instant.ofEpochMilli(nowMillis).plusSeconds(MAX_AHEAD_HOURS * 3600).toEpochMilli()

    /**
     * Every slot the customer may pick: at least [MIN_LEAD_MINUTES] out, at most
     * [MAX_AHEAD_HOURS] out, and inside the shop's working hours for that day.
     *
     * Slots outside working hours are omitted rather than shown disabled — at 15-minute
     * granularity over a 24-hour horizon a greyed-out grid would be mostly dead cells.
     *
     * When [workingHours] is null or empty the shop's schedule is unknown, so only the time
     * bounds are applied and the backend stays the authority on whether the shop is open.
     */
    fun slots(
        workingHours: List<ShopData.WorkHour?>?,
        nowMillis: Long = System.currentTimeMillis()
    ): List<PickupSlot> {
        val step = Duration.ofMinutes(SLOT_STEP_MINUTES)
        val last = Instant.ofEpochMilli(latestSlot(nowMillis)).atZone(SHOP_ZONE)
        val today = Instant.ofEpochMilli(nowMillis).atZone(SHOP_ZONE).toLocalDate()

        val result = ArrayList<PickupSlot>()
        var candidate = Instant.ofEpochMilli(earliestSlot(nowMillis)).atZone(SHOP_ZONE)

        while (!candidate.isAfter(last)) {
            if (isWithinWorkingHours(candidate, workingHours)) {
                result.add(
                    PickupSlot(
                        epochMillis = candidate.toInstant().toEpochMilli(),
                        dayOffset = (candidate.toLocalDate().toEpochDay() - today.toEpochDay())
                            .toInt(),
                        time = candidate.format(SLOT_LABEL_FORMAT)
                    )
                )
            }
            candidate = candidate.plus(step)
        }
        return result
    }

    /**
     * Whether a still-selected slot can be submitted right now. The picker may have been
     * populated minutes ago, so the lead time is re-checked against [nowMillis] before the
     * request goes out; the shop's hours can still change server-side, hence the 409 handling
     * on the checkout screen.
     */
    fun isSelectable(
        epochMillis: Long,
        workingHours: List<ShopData.WorkHour?>?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (epochMillis < earliestSlotExact(nowMillis)) return false
        if (epochMillis > latestSlot(nowMillis)) return false
        return isWithinWorkingHours(
            Instant.ofEpochMilli(epochMillis).atZone(SHOP_ZONE),
            workingHours
        )
    }

    /** The raw `now + 10 min` bound, without rounding up to a slot boundary. */
    private fun earliestSlotExact(nowMillis: Long): Long =
        nowMillis + MIN_LEAD_MINUTES * 60_000

    /**
     * Whether [moment] falls inside the shop's opening window.
     *
     * Overnight windows (a shop open 18:00–02:00) are anchored to the day they *start* on, so
     * 01:30 on Tuesday is covered by Monday's entry — the previous day is checked as well.
     */
    fun isWithinWorkingHours(
        moment: ZonedDateTime,
        workingHours: List<ShopData.WorkHour?>?
    ): Boolean {
        val hours = workingHours?.filterNotNull().orEmpty()
        if (hours.isEmpty()) return true // schedule unknown — let the backend decide

        val date = moment.toLocalDate()
        val time = moment.toLocalTime()

        if (coversSameDay(hours, date.dayOfWeek, time)) return true
        return coversFromPreviousDay(hours, date.minusDays(1).dayOfWeek, time)
    }

    /** The window opening on [day] itself, up to midnight when it runs overnight. */
    private fun coversSameDay(
        hours: List<ShopData.WorkHour>,
        day: DayOfWeek,
        time: LocalTime
    ): Boolean {
        val entry = hours.forDay(day) ?: return false
        val open = entry.openAt.toLocalTimeOrNull() ?: return false
        val close = entry.closeAt.toLocalTimeOrNull() ?: return false

        // An entry whose open and close coincide reads as "open around the clock"; assuming
        // the opposite would lock the customer out of a window the backend would accept.
        if (open == close) return true

        return if (close.isAfter(open)) {
            !time.isBefore(open) && time.isBefore(close)
        } else {
            !time.isBefore(open) // overnight: open .. midnight
        }
    }

    /** The tail of an overnight window that started on [previousDay]: midnight .. close. */
    private fun coversFromPreviousDay(
        hours: List<ShopData.WorkHour>,
        previousDay: DayOfWeek,
        time: LocalTime
    ): Boolean {
        val entry = hours.forDay(previousDay) ?: return false
        val open = entry.openAt.toLocalTimeOrNull() ?: return false
        val close = entry.closeAt.toLocalTimeOrNull() ?: return false

        if (open == close) return true
        if (!close.isBefore(open)) return false // not an overnight window

        return time.isBefore(close)
    }

    /**
     * Matches the API's `weekDay` against a [DayOfWeek]. The API sends English day names
     * ("monday"), which is what the shop detail screen already relies on; short forms are
     * accepted too so a backend tweak doesn't silently disable the picker.
     */
    private fun List<ShopData.WorkHour>.forDay(day: DayOfWeek): ShopData.WorkHour? {
        val full = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase(Locale.ENGLISH)
        val short = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase(Locale.ENGLISH)
        return find {
            val name = it.weekDay?.trim()?.lowercase(Locale.ENGLISH) ?: return@find false
            name == full || name == short
        }
    }

    /** Parses an "HH:mm" (or "HH:mm:ss") schedule value; null when the API sends junk. */
    private fun String?.toLocalTimeOrNull(): LocalTime? {
        val raw = this?.trim() ?: return null
        if (raw.isEmpty()) return null
        // "24:00" is a legal way to spell midnight-at-the-end-of-day but LocalTime rejects it.
        if (raw.startsWith("24:")) return LocalTime.MAX
        return try {
            LocalTime.parse(raw, DateTimeFormatter.ofPattern("H:mm[:ss]", Locale.US))
        } catch (e: Exception) {
            null
        }
    }

    /** Rounds forward to the next [SLOT_STEP_MINUTES] boundary on the shop's clock. */
    private fun roundUpToStep(moment: ZonedDateTime): ZonedDateTime {
        val truncated = moment.withSecond(0).withNano(0)
        val remainder = truncated.minute % SLOT_STEP_MINUTES.toInt()
        val aligned =
            if (remainder == 0 && moment == truncated) truncated
            else truncated.plusMinutes((SLOT_STEP_MINUTES.toInt() - remainder).toLong())
        return aligned
    }

    /** Today's date on the shop's clock — used to label a slot as today or tomorrow. */
    fun shopToday(nowMillis: Long = System.currentTimeMillis()): LocalDate =
        Instant.ofEpochMilli(nowMillis).atZone(SHOP_ZONE).toLocalDate()
}
