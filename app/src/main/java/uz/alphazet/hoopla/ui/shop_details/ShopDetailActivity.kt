package uz.alphazet.hoopla.ui.shop_details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import coil3.load
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.ShopCategoryData
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.ShopDrinksData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.views.imageviewer.StfalconImageViewer
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToCall
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ActivityShopDetailBinding
import uz.alphazet.hoopla.databinding.ItemDrinkCategorySectionBinding
import uz.alphazet.hoopla.ui.order.OrderActivity
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class ShopDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityShopDetailBinding
    private val viewModel: ShopVM by viewModel()

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

    override fun updateStatusBarViewHeight() {}

    private fun collectData(t: UIResource<ShopData>) = t.collect { data ->
        canAcceptOrders = data?.canAcceptOrders == true
        binding.collapsingToolbar.title = data?.name

        // Shop name below header
        binding.shopName.text = data?.name

        // Load first image into header
        val firstImage = data?.pictures?.firstOrNull()
        if (firstImage != null) {
            binding.headerImage.load(firstImage.pictureUrl)
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
        const val SHOP_NAME = "shop_name"
        const val DISTANCE = "distance"
        const val DRINK_DATA = "drink_data"
    }
}