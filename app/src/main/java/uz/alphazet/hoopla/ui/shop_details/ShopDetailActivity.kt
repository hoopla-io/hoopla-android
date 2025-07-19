package uz.alphazet.hoopla.ui.shop_details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import coil3.load
import com.example.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.UrlTypes.URL_TYPE_FACEBOOK
import uz.alphazet.data.models.UrlTypes.URL_TYPE_INSTAGRAM
import uz.alphazet.data.models.UrlTypes.URL_TYPE_WEB
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.utils.formatDistance
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.intentToCall
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ActivityShopDetailBinding
import uz.alphazet.hoopla.ui.order.OrderActivity
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    private val distance by lazy { intent.getDoubleExtra(DISTANCE, -1.0) }

    private val imagesAdapter = ImagesAdapter()
    private val drinksAdapter = DrinksAdapter()
    private val workTimeAdapter = WorkTimeAdapter()

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
        binding.drinkRv.adapter = drinksAdapter
        binding.workTimeRv.adapter = workTimeAdapter

        binding.toolbar.setNavigationOnClickListener { finish() }

        imagesAdapter.setOnItemClickListenerWithPosition { item, position ->
            StfalconImageViewer.Builder(
                this, imagesAdapter.currentList
            ) { view, image ->
                view.load(image.pictureUrl)
            }.withStartPosition(position).show()
        }

        launch {
            viewModel.getShopDetail(shopId).collectLatest(::collectData)
        }
    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

    private fun collectData(t: UIResource<ShopData>) = t.collect { data ->
        binding.toolbar.title = data?.name

        imagesAdapter.submitList(data?.pictures)
        drinksAdapter.submitList(data?.drinks)
        workTimeAdapter.submitList(data?.workingHours)

        drinksAdapter.setOnItemClickListener {
            if (data?.canAcceptOrders == true) {
                val intent1 = Intent(this, OrderActivity::class.java)
                intent1.putExtra(SHOP_ID, shopId)
                intent1.putExtra(SHOP_NAME, binding.toolbar.title.toString())
                intent1.putExtra(DRINK_DATA, it)
                orderListener.launch(intent1)
            } else {
                showMessageDF(getString(R.string.can_not_accepting_orders), "", "OK") {}
            }

        }

        if (data?.workingHours.isNullOrEmpty()) {
            binding.workingTimeContainer.gone()
        } else {
            binding.workingTimeContainer.visible()
            setTodayWorkingTime(data?.workingHours)

            binding.workingTimeContainer.setOnClickListener {
                binding.workTimeRv.isVisible = !binding.workTimeRv.isVisible
            }

        }

        binding.distance.text = formatDistance(distance)
        binding.locationContainer.setOnClickListener {
            val uri =
                "http://maps.google.com/maps?f=d&hl=en&daddr=" + data?.location?.lat.toString() + "," + data?.location?.lng.toString()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(Intent.createChooser(intent, "Select an application"))
        }

        val phoneNumber = data?.phoneNumbers?.firstOrNull()
        if (phoneNumber != null) {
            binding.phoneNumber.text = phoneNumber.phoneNumber?.formatPhoneNumber()
            binding.phoneNumber.setOnClickListener {
                intentToCall(phoneNumber.phoneNumber?.formatPhoneNumber() ?: "")
            }
        } else {
            binding.phoneNumber.text = getString(R.string.not_specified)
        }

        data?.urls?.forEach { item ->
            when (item.urlType) {
                URL_TYPE_WEB -> {
                    binding.web.visible()
                    binding.web.setOnClickListener {
                        intentToBrowser(item.url.toString())
                    }
                }

                URL_TYPE_INSTAGRAM -> {
                    binding.instagram.visible()
                    binding.instagram.setOnClickListener {
                        intentToBrowser(item.url.toString())
                    }
                }

                URL_TYPE_FACEBOOK -> {
                    binding.facebook.visible()
                    binding.facebook.setOnClickListener {
                        intentToBrowser(item.url.toString())
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

        val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        val now = Calendar.getInstance()
        val openTime = Calendar.getInstance()
        val closeTime = Calendar.getInstance()

        openTime.time = format.parse(openAt) ?: return false
        closeTime.time = format.parse(closeAt) ?: return false

        // Ish vaqti tonggacha bo‘lsa (ya'ni yopilish vaqti ochilish vaqtidan oldin)
        val isOvernight = closeTime.before(openTime)

        // openTime va closeTime ni bugungi kun vaqtlariga moslashtiramiz
        openTime.set(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        if (isOvernight) {
            closeTime.set(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH) + 1 // ertangi kun
            )
        } else {
            closeTime.set(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
            )
        }

        return now.after(openTime) && now.before(closeTime)
    }

    companion object {
        const val SHOP_ID = "shop_id"
        const val SHOP_NAME = "shop_name"
        const val DISTANCE = "distance"
        const val DRINK_DATA = "drink_data"
    }


}