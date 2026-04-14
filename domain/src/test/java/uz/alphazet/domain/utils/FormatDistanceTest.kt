package uz.alphazet.domain.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.alphazet.domain.R

@RunWith(RobolectricTestRunner::class)
class FormatDistanceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun sub_one_kilometer_is_rendered_in_meters() {
        val result = context.formatDistance(0.45)

        val expected = context.getString(
            R.string.label_distance_away,
            "450",
            context.getString(R.string.short_metr)
        )
        assertEquals(expected, result)
    }

    @Test
    fun at_or_above_one_kilometer_is_rendered_in_km() {
        val result = context.formatDistance(2.5)

        val expected = context.getString(
            R.string.label_distance_away,
            "2.5",
            context.getString(R.string.short_km)
        )
        assertEquals(expected, result)
    }
}
