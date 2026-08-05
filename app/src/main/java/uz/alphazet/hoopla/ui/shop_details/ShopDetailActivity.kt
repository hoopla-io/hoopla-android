package uz.alphazet.hoopla.ui.shop_details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import coil3.load
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.ShopCategoryData
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.ShopDrinksData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.views.imageviewer.StfalconImageViewer
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.formatRating
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToCall
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ActivityShopDetailBinding
import uz.alphazet.hoopla.databinding.ItemDrinkCategorySectionBinding
import uz.alphazet.hoopla.ui.cart.CartActivity
import uz.alphazet.hoopla.ui.cart.CartVM
import uz.alphazet.hoopla.ui.order.OrderActivity
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class ShopDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityShopDetailBinding
    private val viewModel: ShopVM by viewModel()
    private val cartViewModel: CartVM by viewModel()

    private val shopId by lazy {
        val uri = intent.data
        if (uri != null) {
            val i = uri.pathSegments.indexOfFirst { it.equals("shop") }
            if (i != -1) {
                val id = uri.pathSegments.getOrNull(i + 1)?.toIntOrNull()
                id ?: intent.getIntExtra(SHOP_ID, -1)
            } else
                intent.getIntExtra(SHOP_ID, -1)
        } else
            intent.getIntExtra(SHOP_ID, -1)
    }
    private val imagesAdapter = ImagesAdapter()
    private val workTimeAdapter = WorkTimeAdapter()

    private var isClickable = true
    private var canAcceptOrders = false
    private var shareUrl: String? = null

    /**
     * Last cart the server stated. Held because the bar depends on it *and* on this shop's
     * state, which arrive from two different requests in no fixed order.
     */
    private var cartSummary: CartData? = null

    /** Handed to the order flow so checkout can constrain the pickup-time picker. */
    private var workingHours: ArrayList<ShopData.WorkHour> = arrayListOf()

    // category name -> section root view
    private val sectionViews = mutableListOf<Pair<String, View>>()

    // category name -> chip
    private val categoryChips = mutableMapOf<String, Chip>()

    // prevent chip listener from triggering scroll while we update chip from scroll
    private var isSyncingChip = false

    private val orderListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_ORDER_CREATED -> {
                    setResult(RESULT_ORDER_CREATED)
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageViewPager.adapter = imagesAdapter
        binding.workTimeRv.adapter = workTimeAdapter

        binding.toolbar.setNavigationOnClickListener { finish() }

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

        binding.cartAction.setOnClickListener { openCart() }
        binding.cartBar.setOnClickListener { openCart() }

        launch { cartViewModel.cartFlow.collectLatest(::collectCartSummary) }

        setupAppBarAnimation()

        imagesAdapter.setOnItemClickListenerWithPosition { _, position ->
            StfalconImageViewer.Builder(
                this, imagesAdapter.currentList
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
    }

    private fun setupAppBarAnimation() {
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            val percentage = abs(verticalOffset).toFloat() / totalScrollRange.toFloat()
            binding.headerImage.alpha = 1f - (percentage * 0.3f)
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

    override fun onResume() {
        super.onResume()
        // The count changes from the order screen and from the cart itself, both of which
        // return here.
        refreshCartCount()
    }

    private fun refreshCartCount() {
        cartViewModel.getCart()
    }

    /**
     * Drives both cart affordances. The toolbar icon appears whenever anything is in the cart —
     * including a cart belonging to a different cafe, which is worth surfacing before the
     * customer adds something here and hits the cross-shop conflict.
     *
     * The bottom bar is narrower: it speaks for the order being built at *this* shop, so it
     * stays hidden for someone else's cart rather than pricing this menu with another's total.
     */
    private fun collectCartSummary(t: UIResource<CartData>) = t.collect(
        // A background refresh: it must not spin the screen, and a failure must not toast
        // over a shop the customer is reading.
        onLoading = null,
        // The bar quotes a sum of money, so a refresh that failed must drop it rather than keep
        // pricing a cart that may since have been emptied.
        onError = { cartSummary = null; renderCartAffordances() }
    ) { data ->
        cartSummary = data
        renderCartAffordances()
    }

    /**
     * The badge counts drinks across the whole cart, wherever it was built. The bar is narrower
     * still: it is the order being assembled *here*, so it also waits on the shop actually being
     * able to take one — offering checkout beside a "not accepting orders" badge would be a
     * promise this screen has already refused for every drink on it.
     */
    private fun renderCartAffordances() {
        val data = cartSummary
        val count = data?.items.orEmpty().sumOf { it.quantity ?: 0 }

        binding.cartAction.isVisible = count > 0
        binding.cartBadge.isVisible = count > 0
        binding.cartBadge.text = count.toString()

        val showBar = count > 0 && data?.shopId == shopId && canAcceptOrders && isClickable
        binding.cartBar.isVisible = showBar
        if (showBar) {
            binding.cartBarCount.text =
                resources.getQuantityString(R.plurals.cart_items_count, count, count)
            binding.cartBarTotal.text = (data?.total ?: 0.0).formatToPrice().plus(" UZS")
        }
        // Keep the last drinks reachable above the floating bar.
        binding.nestedScroll.updatePadding(bottom = if (showBar) CART_BAR_INSET_DP.dpToPx() else 0)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun openCart() {
        val intent1 = Intent(this, CartActivity::class.java)
        intent1.putExtra(SHOP_ID, shopId)
        intent1.putExtra(SHOP_NAME, binding.collapsingToolbar.title?.toString())
        orderListener.launch(intent1)
    }

    override fun updateStatusBarViewHeight() {}

    private fun collectData(t: UIResource<ShopData>) = t.collect { data ->
        canAcceptOrders = data?.canAcceptOrders == true
        // The bar is gated on this, and the cart may already have arrived.
        renderCartAffordances()

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
                intentToCall(phoneNumber.phoneNumber?.formatPhoneNumber() ?: "")
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

        val nonEmptyCategories = categories
            ?.filter { !it.drinks.isNullOrEmpty() }
            .orEmpty()

        if (nonEmptyCategories.isEmpty()) {
            binding.categoryScroll.gone()
            return
        }

        val categoryNames = nonEmptyCategories.map {
            it.name ?: getString(R.string.drinks_)
        }
        val inflater = LayoutInflater.from(this)

        // Build all sections first
        nonEmptyCategories.forEachIndexed { index, category ->
            addDrinkSection(
                inflater,
                categoryNames[index],
                category.drinks.orEmpty()
            )
        }

        if (categoryNames.size <= 1) {
            binding.categoryScroll.gone()
            return
        }

        binding.categoryScroll.visible()

        categoryNames.forEachIndexed { index, name ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(getColor(R.color.primary), getColor(R.color.grey_200))
                )
                setTextColor(
                    android.content.res.ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(getColor(R.color.white), getColor(R.color.black_300))
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
        adapter.isClickable = isClickable
        sectionBinding.categoryDrinksRv.adapter = adapter
        adapter.submitList(categoryDrinks)

        adapter.setOnItemClickListener { drink ->
            if (canAcceptOrders && isClickable) {
                val intent1 = Intent(this, OrderActivity::class.java)
                intent1.putExtra(SHOP_ID, shopId)
                intent1.putExtra(SHOP_NAME, binding.collapsingToolbar.title.toString())
                intent1.putExtra(DRINK_DATA, drink)
                intent1.putParcelableArrayListExtra(Constants.WORKING_HOURS, workingHours)
                orderListener.launch(intent1)
            } else {
                showMessageDF(getString(R.string.can_not_accepting_orders), "", "OK") {}
            }
        }

        binding.drinksContainer.addView(sectionBinding.root)
        sectionViews.add(categoryName to sectionBinding.root)
    }

    private fun setTodayWorkingTime(workingHours: List<ShopData.WorkHour?>?) {
        val todayName = Calendar.getInstance()
            .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH)?.lowercase()

        val todayWorkHour = workingHours
            ?.filterNotNull()
            ?.find { it.weekDay?.lowercase() == todayName }

        val isOpen = isNowWorking(todayWorkHour?.openAt, todayWorkHour?.closeAt)

        isClickable = isOpen
        // Outside working hours the bar has to go the same way the drink taps do.
        renderCartAffordances()

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
        /** Room under the menu for the floating cart bar: its height plus its margins. */
        private const val CART_BAR_INSET_DP = 96

        const val SHOP_ID = "shop_id"
        const val SHOP_NAME = "shop_name"
        const val DISTANCE = "distance"
        const val DRINK_DATA = "drink_data"
    }
}