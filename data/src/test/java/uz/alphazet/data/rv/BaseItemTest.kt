package uz.alphazet.data.rv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseItemTest {

    private data class FakeItem(
        override val uniqueId: String,
        val label: String
    ) : BaseItem

    // --- getBaseItemDiffUtil -------------------------------------------

    @Test
    fun diffUtil_areItemsTheSame_compares_by_uniqueId() {
        val cb = getBaseItemDiffUtil<FakeItem>()

        val a = FakeItem(uniqueId = "1", label = "old")
        val b = FakeItem(uniqueId = "1", label = "new")
        val c = FakeItem(uniqueId = "2", label = "other")

        assertTrue(cb.areItemsTheSame(a, b))
        assertFalse(cb.areItemsTheSame(a, c))
    }

    @Test
    fun diffUtil_areContentsTheSame_compares_by_equals() {
        val cb = getBaseItemDiffUtil<FakeItem>()

        val a = FakeItem(uniqueId = "1", label = "old")
        val aCopy = FakeItem(uniqueId = "1", label = "old")
        val aChanged = FakeItem(uniqueId = "1", label = "new")

        assertTrue(cb.areContentsTheSame(a, aCopy))
        assertFalse(cb.areContentsTheSame(a, aChanged))
    }

    // --- emptyItem / emptyItems ----------------------------------------

    @Test
    fun emptyItem_equals_itself_through_uniqueId_comparison() {
        // emptyItem.equals delegates to uniqueId equality, so any BaseItem
        // with the same hashCode-derived uniqueId is considered equal.
        assertTrue(emptyItem == emptyItem)
    }

    @Test
    fun emptyItem_is_not_equal_to_item_with_different_unique_id() {
        val other: BaseItem = FakeItem(uniqueId = "not-empty", label = "x")

        assertNotEquals(emptyItem, other)
    }

    @Test
    fun emptyItems_produces_the_requested_size() {
        val items = emptyItems(size = 7)

        assertEquals(7, items.size)
        items.forEach { assertNotNull(it) }
    }

    @Test
    fun emptyItems_default_size_is_twenty() {
        assertEquals(20, emptyItems().size)
    }
}