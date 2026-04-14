package uz.alphazet.domain.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/**
 * JVM-only tests for the pure formatters in `Utils.kt`. Any helper that needs
 * `Context` (e.g. `formatDistance`) lives in its own Robolectric suite.
 */
class UtilsTest {

    private val originalLocale: Locale = Locale.getDefault()
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun pinLocaleAndTimeZone() {
        // Pin to a deterministic locale + UTC so date-format assertions stay
        // stable across the dev machines this suite runs on.
        Locale.setDefault(Locale.ENGLISH)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreLocaleAndTimeZone() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    // --- getMapImageUrl ---------------------------------------------------

    @Test
    fun getMapImageUrl_builds_expected_yandex_url() {
        val url = getMapImageUrl(long = "69.24", lat = "41.31")

        assertEquals(
            "http://static-maps.yandex.ru/1.x/?lang=en-US&ll=69.24,41.31" +
                "&size=450,450&z=14&l=map&pt=69.24,41.31,vkbkm",
            url
        )
    }

    // --- formatPhoneNumber ------------------------------------------------

    @Test
    fun formatPhoneNumber_formats_uz_mobile_number() {
        assertEquals("+998 90 123-45-67", "998901234567".formatPhoneNumber())
    }

    @Test
    fun formatPhoneNumber_returns_input_when_too_short() {
        assertEquals("99890", "99890".formatPhoneNumber())
    }

    @Test
    fun formatPhoneNumber_ignores_non_digit_input() {
        assertEquals("abc", "abc".formatPhoneNumber())
    }

    // --- moneyType --------------------------------------------------------

    @Test
    fun moneyType_groups_thousands_with_spaces() {
        assertEquals("1 234 567", "1234567".moneyType())
    }

    @Test
    fun moneyType_short_string_unchanged() {
        assertEquals("12", "12".moneyType())
    }

    @Test
    fun moneyType_exactly_three_digits_unchanged() {
        assertEquals("123", "123".moneyType())
    }

    // --- formatToPrice ----------------------------------------------------

    @Test
    fun formatToPrice_integer_valued_double_has_no_decimal_part() {
        assertEquals("1 234 567", 1_234_567.0.formatToPrice())
    }

    @Test
    fun formatToPrice_zero() {
        assertEquals("0", 0.0.formatToPrice())
    }

    @Test
    fun formatToPrice_keeps_fractional_part_after_dot() {
        assertEquals("1 234.5", 1234.5.formatToPrice())
    }

    // --- date formatters (pinned to UTC + ENGLISH above) ------------------
    // 1_704_067_200L * 1000 ms = 2024-01-01T00:00:00Z

    private val epochSeconds2024 = 1_704_067_200L

    @Test
    fun getDateDMMMMYYYY_renders_day_month_year() {
        assertEquals("1 January, 2024", epochSeconds2024.getDateDMMMMYYYY())
    }

    @Test
    fun getDateDDMMMMYYYYHHmm_pads_day_and_includes_time() {
        assertEquals("01 January, 2024  00:00", epochSeconds2024.getDateDDMMMMYYYYHHmm())
    }

    @Test
    fun getDateDMMMMYYYYHHmm_unpadded_day_with_time() {
        assertEquals("1 January, 2024  00:00", epochSeconds2024.getDateDMMMMYYYYHHmm())
    }

    @Test
    fun getDateHHmm_renders_hours_minutes_only() {
        assertEquals("00:00", epochSeconds2024.getDateHHmm())
    }
}
