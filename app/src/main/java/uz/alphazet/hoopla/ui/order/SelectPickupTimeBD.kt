package uz.alphazet.hoopla.ui.order

import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import uz.alphazet.data.models.ShopData
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.utils.PickupSlot
import uz.alphazet.domain.utils.PickupTime
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogSelectPickupTimeBinding

/**
 * Lets the customer swap the default "as soon as possible" order for a specific pickup time.
 *
 * The offered slots are pre-constrained with [PickupTime] — at least 10 minutes out, at most
 * 24 hours out, and inside [workingHours] — so the customer almost never has to submit an
 * order to discover the time doesn't work. The backend stays the authority: the shop's hours
 * or pause state can change between opening this sheet and checking out, which the checkout
 * screen handles as a 409.
 *
 * Reports back the chosen instant in epoch millis, or null for ASAP.
 */
class SelectPickupTimeBD(
    private val workingHours: List<ShopData.WorkHour?>?,
    private val currentSelection: Long? = null,
    private val onPickupTimeSelected: (Long?) -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_select_pickup_time) {

    private val binding by viewBinding(DialogSelectPickupTimeBinding::bind)

    private val todayAdapter = PickupSlotAdapter()
    private val tomorrowAdapter = PickupSlotAdapter()

    /** Null means ASAP, which is what the sheet opens on unless a time is already chosen. */
    private var selected: Long? = null

    override fun initialize() {
        val slots = PickupTime.slots(workingHours)
        val today = slots.filter { it.dayOffset == 0 }
        val tomorrow = slots.filter { it.dayOffset >= 1 }

        // A previously chosen time is only pre-selected while it is still offered; a sheet
        // reopened half an hour later must not keep a slot that has since fallen inside the
        // 10-minute lead window.
        selected = currentSelection?.takeIf { current -> slots.any { it.epochMillis == current } }

        binding.todaySlots.adapter = todayAdapter
        binding.tomorrowSlots.adapter = tomorrowAdapter

        todayAdapter.submitList(today)
        tomorrowAdapter.submitList(tomorrow)

        binding.todayTitle.isVisible = today.isNotEmpty()
        binding.todaySlots.isVisible = today.isNotEmpty()
        binding.tomorrowTitle.isVisible = tomorrow.isNotEmpty()
        binding.tomorrowSlots.isVisible = tomorrow.isNotEmpty()
        binding.emptySlots.isVisible = slots.isEmpty()

        todayAdapter.setOnItemClickListener(::onSlotClicked)
        tomorrowAdapter.setOnItemClickListener(::onSlotClicked)

        binding.asapRow.setOnClickListener { selectAsap() }

        binding.btConfirm.setOnClickListener {
            onPickupTimeSelected(selected)
            dismiss()
        }

        applySelection()
    }

    private fun onSlotClicked(slot: PickupSlot) {
        selected = slot.epochMillis
        applySelection()
    }

    private fun selectAsap() {
        selected = null
        applySelection()
    }

    /** Keeps the ASAP row and both day grids agreeing on the single current choice. */
    private fun applySelection() {
        todayAdapter.selectSlot(selected)
        tomorrowAdapter.selectSlot(selected)
        binding.asapCheck.isVisible = selected == null
    }

    companion object {
        private const val TAG = "SelectPickupTimeBD"

        fun FragmentActivity.showSelectPickupTimeBD(
            workingHours: List<ShopData.WorkHour?>?,
            currentSelection: Long? = null,
            onPickupTimeSelected: (Long?) -> Unit = {}
        ) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                SelectPickupTimeBD(workingHours, currentSelection, onPickupTimeSelected)
                    .show(supportFragmentManager, TAG)
            }
        }
    }
}
