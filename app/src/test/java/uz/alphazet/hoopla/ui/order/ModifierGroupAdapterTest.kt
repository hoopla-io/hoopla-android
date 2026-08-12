package uz.alphazet.hoopla.ui.order

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.alphazet.data.models.order.ModifierGroupData
import uz.alphazet.hoopla.TestApp

/**
 * The group header states two separate things: whether the group has to be answered at all, and
 * any counting rule the badge cannot carry. These check the two do not repeat each other.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class ModifierGroupAdapterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val adapter = ModifierGroupAdapter { }

    private fun group(minSelect: Int, maxSelect: Int?) = ModifierGroupData(
        key = "milk",
        name = "Milk",
        minSelect = minSelect,
        maxSelect = maxSelect,
        options = null
    )

    private fun required() = context.getString(uz.alphazet.domain.R.string.modifier_badge_required)
    private fun optional() = context.getString(uz.alphazet.domain.R.string.modifier_badge_optional)

    @Test
    fun `a group that must be answered reads as required`() {
        assertEquals(required(), adapter.badgeText(context, group(minSelect = 1, maxSelect = 1)))
    }

    @Test
    fun `a group that need not be answered reads as optional`() {
        assertEquals(optional(), adapter.badgeText(context, group(minSelect = 0, maxSelect = 1)))
    }

    @Test
    fun `pick-one says nothing the required badge has not already said`() {
        assertEquals("", adapter.ruleText(context, group(minSelect = 1, maxSelect = 1)))
    }

    @Test
    fun `an optional single-choice group has no extra rule either`() {
        assertEquals("", adapter.ruleText(context, group(minSelect = 0, maxSelect = 1)))
    }

    @Test
    fun `unlimited choice has no counting rule to state`() {
        assertEquals("", adapter.ruleText(context, group(minSelect = 0, maxSelect = null)))
    }

    @Test
    fun `a floor above one is spelled out, because the badge cannot`() {
        assertEquals(
            context.getString(uz.alphazet.domain.R.string.modifier_rule_at_least, 2),
            adapter.ruleText(context, group(minSelect = 2, maxSelect = 4))
        )
    }

    @Test
    fun `a ceiling above one is spelled out too`() {
        assertEquals(
            context.getString(uz.alphazet.domain.R.string.modifier_rule_up_to, 3),
            adapter.ruleText(context, group(minSelect = 0, maxSelect = 3))
        )
    }
}
