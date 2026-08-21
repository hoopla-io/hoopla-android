package uz.alphazet.hoopla.ui.shop_details

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil3.load
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.ShopCategoryData
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.ShopDrinksData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.domain.R
import uz.alphazet.domain.cart.CartStore
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.ui.views.imageviewer.StfalconImageViewer
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.formatRating
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToCall
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.databinding.ItemDrinkCategorySectionBinding
import uz.alphazet.hoopla.databinding.ScreenShopDetailBinding
import uz.alphazet.hoopla.ui.cart.CartVM
import uz.alphazet.hoopla.ui.mainActivity
import uz.alphazet.hoopla.ui.navigateTo
import uz.alphazet.hoopla.ui.order.OrderScreen
import uz.alphazet.hoopla.ui.popScreen
import uz.alphazet.hoopla.ui.views.showTopPill
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class ShopDetailScreen : BaseFragment(uz.alphazet.hoopla.R.layout.screen_shop_detail) {

    private val binding by viewBinding(ScreenShopDetailBinding::bind)
    private val viewModel: ShopVM by viewModel()

    private val shopId by lazy { arguments?.getInt(SHOP_ID, -1) ?: -1 }
    private val imagesAdapter = ImagesAdapter()
    private val workTimeAdapter = WorkTimeAdapter()

    private var isClickable = true
    private var canAcceptOrders = false
    private var shareUrl: String? = null

    /** Handed to the order flow so checkout can constrain the pickup-time picker. */
    private var workingHours: ArrayList<ShopData.WorkHour> = arrayListOf()

    // category name -> section root view
    private val sectionViews = mutableListOf<Pair<String, View>>()

    // category name -> chip
    private val categoryChips = mutableMapOf<String, Chip>()

    // prevent chip listener from triggering scroll while we update chip from scroll
    private var isSyncingChip = false

    // ------------------------------------------------------------ cart on the menu

    private val cartViewModel: CartVM by viewModel()
    private val cartStore: CartStore by inject()

    /** The cart as the server last stated it. */
    private var cart: CartData? = null

    /** One adapter per category section, so a cart change can reach all of them. */
    private val drinkAdapters = mutableListOf<DrinksAdapter>()

    /** cart line id -> the count the customer has stepped to but not yet sent. */
    private val pendingQuantities = mutableMapOf<Int, Int>()
    private val quantityJobs = mutableMapOf<Int, Job>()

    /** Drinks whose "does this need options?" round-trip is still in the air. */
    private val pendingAdds = mutableSetOf<Int>()

    /** The drink a quick-add is for, kept so a 409 can retry it after clearing. */
    private var pendingQuickAddDrink: DrinkItemData? = null

    /** Orders responses so a slow earlier one cannot repaint over a newer cart. */
    private var mutationSeq = 0

    override fun initialize() {
        binding.imageViewPager.adapter = imagesAdapter
        binding.workTimeRv.adapter = workTimeAdapter

        binding.toolbar.setNavigationOnClickListener { popScreen() }

        binding.toolbar.inflateMenu(uz.alphazet.hoopla.R.menu.menu_shop_detail)
        binding.toolbar.menu.findItem(uz.alphazet.hoopla.R.id.action_share)?.isVisible = false
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                uz.alphazet.hoopla.R.id.action_share -> {
                    shareShop()
                    true
                }

                else -> false
            }
        }

        setupAppBarAnimation()

        imagesAdapter.setOnItemClickListenerWithPosition { _, position ->
            StfalconImageViewer.Builder(
                requireContext(), imagesAdapter.currentList
            ) { view, image ->
                view.load(image.pictureUrl)
            }.withStartPosition(position).show()
        }

        launch {
            viewModel.getShopDetail(shopId).collectLatest(::collectData)
        }
        launch {
            viewModel.getShopDrinks(shopId).collectLatest(::collectDrinks)
        }

        // Any screen that edits the cart publishes here, so the menu keeps up without asking.
        launch {
            cartStore.cartFlow.collectLatest { data ->
                cart = data
                renderCartState()
            }
        }
        cartViewModel.getCart()
    }

    /**
     * Tabs and pushed screens are hidden rather than destroyed, so `onResume` does not fire on the
     * way back from the order screen — this is where a cart edited elsewhere is picked up.
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) flushPendingEdits() else cartViewModel.getCart()
    }

    override fun onPause() {
        super.onPause()
        flushPendingEdits()
    }

    /** Commits anything the customer already sees applied but that has not been sent yet. */
    private fun flushPendingEdits() {
        quantityJobs.values.forEach { it.cancel() }
        quantityJobs.clear()
        pendingQuantities.forEach { (id, quantity) ->
            cartViewModel.updateItemQuantityInBackground(id, quantity)
        }
        pendingQuantities.clear()
    }

    private fun setupAppBarAnimation() {
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@addOnOffsetChangedListener
            val percentage = abs(verticalOffset).toFloat() / totalScrollRange.toFloat()
            binding.headerImage.alpha = 1f - (percentage * 0.3f)

            // The info block now collapses with the header. `remaining` is how much scroll is left
            // before it is fully collapsed, and it equals the block's own height exactly when the
            // block reaches the toolbar — so fading it out over the stretch before that keeps it
            // from showing through the toolbar while the content scrim is still transparent.
            val infoHeight = binding.headerInfo.height.coerceAtLeast(1)
            val remaining = (totalScrollRange + verticalOffset).toFloat()
            val infoAlpha = ((remaining - infoHeight) / infoHeight).coerceIn(0f, 1f)
            binding.headerInfo.alpha = infoAlpha
            // Alpha alone still takes touches, so a block faded past the point of being seen would
            // keep dialling the shop when the empty strip is tapped. INVISIBLE drops it out of
            // touch dispatch while keeping its height, which the header's scroll range depends on.
            binding.headerInfo.isInvisible = infoAlpha < 0.1f
        }

        binding.nestedScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            syncChipToScroll(scrollY)
        }
    }

    private fun syncChipToScroll(scrollY: Int) {
        if (sectionViews.isEmpty()) return
        // Find the last section whose top is at or above the current scroll position (+ small offset)
        val offset = 8
        var activeCategory = sectionViews.first().first
        for ((name, view) in sectionViews) {
            val sectionTop = getSectionTopInScroll(view)
            if (scrollY + offset >= sectionTop) activeCategory = name
            else break
        }
        val chip = categoryChips[activeCategory] ?: return
        if (chip.isChecked) return
        isSyncingChip = true
        chip.isChecked = true
        isSyncingChip = false
        // Scroll chip into view
        val chipIndex = categoryChips.keys.indexOf(activeCategory)
        if (chipIndex >= 0) {
            val chipView = binding.categoryChipGroup.getChildAt(chipIndex)
            binding.categoryScroll.smoothScrollTo(chipView.left - 16, 0)
        }
    }

    private fun getSectionTopInScroll(sectionView: View): Int {
        // Y of section relative to the NestedScrollView content (its LinearLayout parent)
        var y = sectionView.top
        var parent = sectionView.parent as? View
        while (parent != null && parent != binding.nestedScroll) {
            y += parent.top
            parent = parent.parent as? View
        }
        return y
    }

    private fun scrollToSection(categoryName: String) {
        val sectionView = sectionViews.firstOrNull { it.first == categoryName }?.second ?: return
        binding.nestedScroll.post {
            val top = getSectionTopInScroll(sectionView)
            binding.nestedScroll.smoothScrollTo(0, top)
        }
    }

    private fun collectData(t: UIResource<ShopData>) = t.collect { data ->
        canAcceptOrders = data?.canAcceptOrders == true
        // The shop's state and its menu arrive on separate flows, so whichever lands second has
        // to re-apply the gate to the cards already built.
        renderCartState()

        // Share action: shown once the shop (and its shareUrl) has loaded.
        shareUrl = data?.shareUrl
        binding.toolbar.menu.findItem(uz.alphazet.hoopla.R.id.action_share)?.isVisible =
            !data?.shareUrl.isNullOrBlank()
        // Paused state matches the list overlay: shown only when explicitly false.
        binding.pausedBadge.isVisible = data?.canAcceptOrders == false
        binding.collapsingToolbar.title = data?.name

        // Shop name below header
        binding.shopName.text = data?.name

        // Shop rating: "★ 4.7". Hidden only if the field is somehow absent.
        val rating = data?.rating
        binding.ratingRow.isVisible = rating != null
        if (rating != null) binding.shopRating.text = rating.formatRating()

        // Load first image into header
        val firstImage = data?.pictures?.firstOrNull()
        if (firstImage != null) {
            binding.headerImage.load(firstImage.pictureUrl)
        } else {
            binding.headerImage.load(data?.pictureUrl)
        }

        // Show remaining images in carousel if more than 1
        val remainingImages = data?.pictures?.drop(1)
        if (!remainingImages.isNullOrEmpty()) {
            binding.imageViewPager.visible()
            imagesAdapter.submitList(remainingImages)
        } else {
            binding.imageViewPager.gone()
        }

        workTimeAdapter.submitList(data?.workingHours)
        workingHours = ArrayList(data?.workingHours?.filterNotNull().orEmpty())

        // Working hours & time setup
        if (data?.workingHours.isNullOrEmpty()) {
            binding.workingTimeContainer.gone()
            binding.workingHoursText.gone()
        } else {
            binding.workingTimeContainer.visible()
            setTodayWorkingTime(data?.workingHours)

            binding.workingTimeContainer.setOnClickListener {
                binding.workTimeRv.isVisible = !binding.workTimeRv.isVisible
            }
        }

        // Direction button
        binding.btnDirection.setOnClickListener {
            val uri =
                "http://maps.google.com/maps?f=d&hl=en&daddr=" + data?.location?.lat.toString() + "," + data?.location?.lng.toString()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(Intent.createChooser(intent, "Select an application"))
        }

        // Call button
        val phoneNumber = data?.phoneNumbers?.firstOrNull()
        if (phoneNumber != null) {
            binding.btnCall.setOnClickListener {
                requireContext().intentToCall(phoneNumber.phoneNumber?.formatPhoneNumber() ?: "")
            }
        }
    }

    private fun shareShop() {
        val url = shareUrl
        if (url.isNullOrBlank()) return
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share)))
    }

    private fun collectDrinks(t: UIResource<ShopDrinksData>) = t.collect { data ->
        buildCategoryChipsAndSections(data?.categories)
    }

    private fun buildCategoryChipsAndSections(categories: List<ShopCategoryData>?) {
        binding.categoryChipGroup.removeAllViews()
        binding.drinksContainer.removeAllViews()
        sectionViews.clear()
        categoryChips.clear()
        drinkAdapters.clear()

        val nonEmptyCategories = categories
            ?.filter { !it.drinks.isNullOrEmpty() }
            .orEmpty()

        if (nonEmptyCategories.isEmpty()) {
            binding.categoryScroll.gone()
            renderCartState()
            return
        }

        val categoryNames = nonEmptyCategories.map {
            it.name ?: getString(R.string.drinks_)
        }
        val inflater = LayoutInflater.from(requireContext())

        // Build all sections first
        nonEmptyCategories.forEachIndexed { index, category ->
            addDrinkSection(
                inflater,
                categoryNames[index],
                category.drinks.orEmpty()
            )
        }
        renderCartState()

        if (categoryNames.size <= 1) {
            binding.categoryScroll.gone()
            return
        }

        binding.categoryScroll.visible()

        categoryNames.forEachIndexed { index, name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        requireContext().getColor(R.color.primary),
                        requireContext().getColor(R.color.grey_200)
                    )
                )
                setTextColor(
                    android.content.res.ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(
                            requireContext().getColor(R.color.white),
                            requireContext().getColor(R.color.black_300)
                        )
                    )
                )
                typeface = resources.getFont(R.font.inter_medium)
                textSize = 14f
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(20f * resources.displayMetrics.density)
                    .build()
            }

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !isSyncingChip) {
                    scrollToSection(name)
                }
            }

            binding.categoryChipGroup.addView(chip)
            categoryChips[name] = chip

            if (index == 0) chip.isChecked = true
        }
    }

    private fun addDrinkSection(
        inflater: LayoutInflater,
        categoryName: String,
        categoryDrinks: List<DrinkItemData>
    ) {
        val sectionBinding = ItemDrinkCategorySectionBinding.inflate(
            inflater, binding.drinksContainer, false
        )

        sectionBinding.categoryName.text = categoryName

        val adapter = DrinksAdapter()
        adapter.isClickable = canAcceptOrders && isClickable
        sectionBinding.categoryDrinksRv.adapter = adapter
        adapter.submitList(categoryDrinks)
        drinkAdapters.add(adapter)

        adapter.setOnItemClickListener { drink ->
            if (canAcceptOrders && isClickable) {
                openDrink(drink)
            } else {
                showMessageDF(getString(R.string.can_not_accepting_orders), "", "OK") {}
            }
        }
        adapter.setOnAddClickListener(::onAddClicked)
        adapter.setOnQuantityChangeListener(::onQuantityStepped)

        binding.drinksContainer.addView(sectionBinding.root)
        sectionViews.add(categoryName to sectionBinding.root)
    }

    private fun openDrink(drink: DrinkItemData) {
        navigateTo(
            OrderScreen.newInstance(
                shopId = shopId,
                drink = drink,
                workingHours = workingHours,
            )
        )
    }

    // ------------------------------------------------------------ cart on the menu

    /**
     * The lines a menu card may step in place. A card carries a single stepper, so it can only
     * stand for a drink held by exactly one line, added without modifiers — anything else is
     * ambiguous about which line a tap means, and offers "+" instead.
     */
    private fun steppableLines(): Map<Int, CartLineRef> {
        val data = cart ?: return emptyMap()
        if (data.shopId != shopId) return emptyMap() // the cart belongs to another cafe
        return data.items.orEmpty()
            .groupBy { it.drinkId }
            .mapNotNull { (drinkId, lines) ->
                val id = drinkId ?: return@mapNotNull null
                val line = lines.singleOrNull() ?: return@mapNotNull null
                if (!line.modifiers.isNullOrEmpty()) return@mapNotNull null
                val itemId = line.id ?: return@mapNotNull null
                val quantity = pendingQuantities[itemId] ?: line.quantity ?: 0
                if (quantity <= 0) return@mapNotNull null
                id to CartLineRef(itemId, quantity)
            }
            .toMap()
    }

    private fun renderCartState() {
        val lines = steppableLines()
        val enabled = canAcceptOrders && isClickable
        drinkAdapters.forEach { adapter ->
            adapter.isClickable = enabled
            adapter.cartLines = lines
            adapter.pendingAdds = pendingAdds.toSet()
        }
        // Only once a cart has actually arrived: a null one here means "not fetched yet", and
        // pushing that would blank a badge the host had already filled in correctly.
        cart?.let { mainActivity?.onCartUpdated(optimisticCart()) }
    }

    /** The cart as the customer currently sees it, so the nav-bar badge agrees with the menu. */
    private fun optimisticCart(): CartData? {
        val data = cart ?: return null
        if (pendingQuantities.isEmpty()) return data
        val items = data.items.orEmpty().mapNotNull { item ->
            val id = item.id ?: return@mapNotNull item
            val quantity = pendingQuantities[id] ?: return@mapNotNull item
            if (quantity <= 0) null else item.copy(quantity = quantity)
        }
        return data.copy(items = items)
    }

    /**
     * The menu payload says nothing about whether a drink has modifiers, so the only way to tell
     * a one-tap drink from one whose options have to be picked is to ask.
     */
    private fun onAddClicked(drink: DrinkItemData) {
        if (!canAcceptOrders || !isClickable) {
            showMessageDF(getString(R.string.can_not_accepting_orders), "", "OK") {}
            return
        }

        val drinkId = drink.id ?: return

        // The card can be back on "+" only because the count was stepped to zero and the removal
        // has not gone out yet. Stepping it back up is what the customer means — adding would
        // race that queued PATCH, which could then drop the line that was just re-added.
        val revivableId = cart?.items.orEmpty().firstOrNull { item ->
            item.drinkId == drinkId && item.modifiers.isNullOrEmpty() &&
                    item.id != null && pendingQuantities[item.id] == 0
        }?.id
        if (revivableId != null) {
            onQuantityStepped(CartLineRef(revivableId, 0), 1)
            return
        }

        if (!pendingAdds.add(drinkId)) return // already asking about this one
        renderCartState()

        launch {
            viewModel.validateOrder(shopId, drinkId).collectLatest { resource ->
                resource.collect(
                    onLoading = null,
                    onError = { throwable ->
                        finishAdd(drinkId)
                        checkErrors(throwable)
                    }
                ) { details ->
                    if (details != null && !hasAnyOption(details)) {
                        quickAdd(drink)
                    } else {
                        finishAdd(drinkId)
                        openDrink(drink)
                    }
                }
            }
        }
    }

    /**
     * Whether anything has to be chosen before this drink can be ordered. The legacy sections
     * count: the order screen pre-selects the first option of each non-empty one, and those
     * selections carry a price — quick-adding would charge for a choice never offered.
     */
    private fun hasAnyOption(details: OrderDetails): Boolean =
        !details.modifierGroups.isNullOrEmpty() ||
                !details.modifications.size.isNullOrEmpty() ||
                !details.modifications.sugar.isNullOrEmpty() ||
                !details.modifications.milk.isNullOrEmpty() ||
                !details.modifications.syrup.isNullOrEmpty()

    private fun quickAdd(drink: DrinkItemData) {
        val drinkId = drink.id ?: return
        pendingQuickAddDrink = drink
        launch {
            cartViewModel.addItem(shopId, drinkId, 1, arrayListOf()).collectLatest { resource ->
                resource.collect(
                    onLoading = null,
                    onError = { throwable ->
                        finishAdd(drinkId)
                        // A conflict still needs the drink, to retry after clearing.
                        if (throwable !is ConflictException) pendingQuickAddDrink = null
                        checkErrors(throwable)
                    }
                ) { data ->
                    finishAdd(drinkId)
                    pendingQuickAddDrink = null
                    cart = data
                    renderCartState()
                    showTopPill(R.string.cart_item_added)
                }
            }
        }
    }

    private fun finishAdd(drinkId: Int) {
        pendingAdds.remove(drinkId)
        renderCartState()
    }

    /**
     * Answers the tap straight away and sends the settled count once the customer stops stepping,
     * so three quick taps are one PATCH of +3 rather than three racing ones.
     */
    private fun onQuantityStepped(line: CartLineRef, quantity: Int) {
        val id = line.itemId
        pendingQuantities[id] = quantity.coerceAtLeast(0)
        renderCartState()

        quantityJobs[id]?.cancel()
        quantityJobs[id] = launch {
            delay(QUANTITY_DEBOUNCE_MS)
            val seq = ++mutationSeq
            cartViewModel.updateItemQuantity(id, quantity).collectLatest { resource ->
                resource.collect(
                    onLoading = null,
                    onError = { throwable ->
                        // The server never took it, so the optimistic number has to go back.
                        pendingQuantities.remove(id)
                        renderCartState()
                        checkErrors(throwable)
                    }
                ) { data ->
                    pendingQuantities.remove(id)
                    acceptCart(data, seq)
                }
            }
        }
    }

    /** Ignores a response overtaken by a later one, so the grid cannot go backwards. */
    private fun acceptCart(data: CartData?, seq: Int) {
        if (seq != mutationSeq) return
        cart = data
        renderCartState()
    }

    /**
     * A 409 while adding means the cart already holds another cafe's drinks. Emptying someone's
     * cart is destructive, so it is offered rather than done, and the drink is retried afterwards.
     */
    override fun onConflictException(message: String?, code: Int) {
        val drink = pendingQuickAddDrink
        pendingQuickAddDrink = null

        if (drink == null) {
            super.onConflictException(message, code)
            return
        }

        showRequestDF(
            getString(R.string.cart_different_shop_title),
            message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.cart_different_shop_message),
            getString(R.string.cart_clear_and_add),
            getString(R.string.cancel)
        ) {
            launch {
                cartViewModel.clearCart().collectLatest { resource ->
                    resource.collect(
                        onLoading = null,
                        onError = { throwable -> checkErrors(throwable) }
                    ) {
                        val drinkId = drink.id
                        if (drinkId != null) pendingAdds.add(drinkId)
                        renderCartState()
                        quickAdd(drink)
                    }
                }
            }
        }
    }

    private fun setTodayWorkingTime(workingHours: List<ShopData.WorkHour?>?) {
        val todayName = Calendar.getInstance()
            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH)?.lowercase()

        val todayWorkHour = workingHours
            ?.filterNotNull()
            ?.find { it.weekDay?.lowercase() == todayName }

        val isOpen = isNowWorking(todayWorkHour?.openAt, todayWorkHour?.closeAt)

        isClickable = isOpen
        renderCartState()

        // Show inline working hours below shop name
        if (todayWorkHour != null) {
            binding.workingHoursText.visible()
            binding.workingHoursText.text =
                todayWorkHour.openAt?.plus(" - ")?.plus(todayWorkHour.closeAt)
        } else {
            binding.workingHoursText.gone()
        }

        if (isOpen) {
            binding.hours.text = todayWorkHour?.openAt?.plus(" - ")?.plus(todayWorkHour.closeAt)
            binding.hours.setTextColorRes(R.color.green_300)
            binding.openToday.visible()
            binding.closedToday.gone()
        } else {
            binding.hours.text = todayWorkHour?.openAt?.plus(" - ")?.plus(todayWorkHour.closeAt)
            binding.hours.setTextColorRes(R.color.error_400)
            binding.openToday.gone()
            binding.closedToday.visible()
        }
    }

    private fun isNowWorking(openAt: String?, closeAt: String?): Boolean {
        if (openAt == null || closeAt == null) return false

        val format = SimpleDateFormat("HH:mm", Locale.ENGLISH)
        val now = Calendar.getInstance()

        val nowTime = format.format(now.time)

        val open = format.parse(openAt) ?: return false
        val close = format.parse(closeAt) ?: return false
        val current = format.parse(nowTime) ?: return false

        val openCal = Calendar.getInstance().apply { time = open }
        val closeCal = Calendar.getInstance().apply { time = close }
        val currentCal = Calendar.getInstance().apply { time = current }

        val overnight = closeCal.before(openCal)

        return if (overnight) {
            currentCal.after(openCal) || currentCal.before(closeCal)
        } else {
            currentCal.after(openCal) && currentCal.before(closeCal)
        }
    }

    companion object {
        const val SHOP_ID = "shop_id"
        const val DRINK_DATA = "drink_data"

        /** How long the grid waits for the customer to settle on a count before sending it. */
        private const val QUANTITY_DEBOUNCE_MS = 500L

        fun newInstance(shopId: Int?) = ShopDetailScreen().apply {
            arguments = bundleOf(SHOP_ID to (shopId ?: -1))
        }
    }
}
