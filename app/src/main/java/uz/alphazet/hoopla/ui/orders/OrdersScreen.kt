package uz.alphazet.hoopla.ui.orders

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil3.load
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.OrderHistoryItemData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.views.imageviewer.StfalconImageViewer
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenOrdersBinding
import uz.alphazet.hoopla.ui.home.FeedbackBD.Companion.showFeedbackBD
import uz.alphazet.hoopla.ui.navigateTo

class OrdersScreen : BaseFragment(R.layout.screen_orders), SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenOrdersBinding::bind)
    private val viewModel: OrdersVM by viewModel()

    private val orderAdapter = OrderHistoryAdapter()

    private var imageViewer: StfalconImageViewer<Drawable>? = null

    override fun initialize() {

        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.orderRv.adapter = orderAdapter

//        launch {
//            viewModel.qrCodeDataFlow.collectLatest(::collectQRCodeData)
//        }

//        launch {
//            viewModel.drinksStatDataFlow.collectLatest(::collectDrinksStat)
//        }

//        launch {
//            viewModel.userDataFlow.collectLatest(::collectUserData)
//        }

        launch {
            viewModel.getOrderHistoryPager().collectLatest(::collectOrdersData)
        }

        launch {
            orderAdapter.loadStateFlow.collectLatest { loadState ->
                when (loadState.refresh) {
                    is LoadState.NotLoading -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        if (orderAdapter.itemCount == 0) {
                            binding.emptyState.visible()
                        } else {
                            binding.emptyState.gone()
                        }
                    }

                    is LoadState.Loading -> {
                        binding.swipeRefreshLayout.isRefreshing = true
                    }

                    is LoadState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val throwable = (loadState.refresh as LoadState.Error).error
                        if (throwable !is NullPointerException)
                            showErrorMessage(
                                throwable.message ?: getString(uz.alphazet.domain.R.string.error)
                            )
                        else
                            if (orderAdapter.itemCount == 0) {
                                binding.emptyState.visible()
                            } else {
                                binding.emptyState.gone()
                            }
                    }

                }
            }
        }

        orderAdapter.setOnItemClickListener {
            navigateTo(OrderInfoScreen.newInstance(it?.id ?: -1))
        }

        // The rated order must lose its button, and only the server knows it has: reload
        // rather than patch the row, so the card and `hasFeedback` cannot drift apart.
        orderAdapter.setOnRateClickListener { order ->
            showFeedbackBD(order.toFeedbackDetail()) { orderAdapter.refresh() }
        }

    }

    // Re-shown after a tab switch, a fresh checkout landing here, or popping back from the
    // order-info screen (where an order can be cancelled) — the list must reflect all three.
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) orderAdapter.refresh()
    }

    private fun collectQRCodeData(t: UIResource<QRCodeAccessData>) = t.collect(onLoading = {}) {
        val drawable = generateQRCodeImage(it?.token.toString())
        val bitmap = drawableToBitmap(drawable)
        val bitmapDrawable = bitmap.toDrawable(resources)
        binding.qrCode.load(bitmapDrawable)

        binding.qrCode.setOnClickListener {
            imageViewer = StfalconImageViewer.Builder(
                requireContext(), arrayListOf(drawable)
            ) { view, image ->
                view.load(drawable)
            }.allowZooming(false).withHiddenStatusBar(false).build()
            imageViewer?.show(true)
        }

    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect {

        binding.phoneNumber.text = it?.phoneNumber?.formatPhoneNumber()

        if (it?.subscription != null) {
            binding.tariffName.text = it.subscription?.name
        } else {
            binding.tariffName.text = ""
        }
    }

    private fun collectDrinksStat(t: UIResource<DailyDrinksStatData>) = t.collect {
        binding.available.text =
            getString(uz.alphazet.domain.R.string.label_total, it?.available.toString())
        binding.used.text = getString(uz.alphazet.domain.R.string.label_used, it?.used.toString())

        binding.progress.max = it?.available ?: 0
        binding.progress.progress = it?.used ?: 0
    }

    private suspend fun collectOrdersData(t: PagingData<OrderHistoryItemData>) {
        orderAdapter.submitData(t)
    }

    /**
     * The rating sheet is written against a single-drink order, so a multi-drink one is
     * summarised into it: every drink's name, the order's total, and the first picture there
     * is. The feedback itself is filed against the order id either way.
     */
    private fun OrderHistoryItemData.toFeedbackDetail() = FeedbackDetail(
        cashbackEarned = cashbackEarned,
        cashbackUsed = null,
        drinkName = drinks.orEmpty().mapNotNull { it.drinkName }.joinToString(", ")
            .ifBlank { null },
        id = id,
        orderStatus = commonStatus,
        partnerName = shopName,
        productPrice = totalPrice,
        purchasedAt = purchasedAt,
        purchasedAtUnix = purchasedAtUnix,
        shopName = shopName,
        drinkImage = drinks.orEmpty().firstNotNullOfOrNull { it.drinkImageUrl }
    )

    private fun generateQRCodeImage(data: String): Drawable {
        val options = QrVectorOptions.Builder()
            .setPadding(0.2f)
            .setBackground(
                QrVectorBackground(
                    drawable = ContextCompat
                        .getDrawable(
                            requireContext(),
                            uz.alphazet.domain.R.drawable.bg_corner20_white
                        ),
                    scale = BitmapScale.CenterCrop
                )
            )
            .setColors(
                QrVectorColors(
                    ball = QrVectorColor.Solid(
                        ContextCompat.getColor(requireContext(), uz.alphazet.domain.R.color.black)
                    ),
                )
            )
            .setShapes(
                QrVectorShapes(
                    darkPixel = QrVectorPixelShape
                        .RoundCorners(.5f),
                    ball = QrVectorBallShape
                        .RoundCorners(.25f),
                    frame = QrVectorFrameShape
                        .RoundCorners(.25f),
                )
            )
            .build()

        val drawable: Drawable = QrCodeDrawable(QrData.Url(data), options)
        return drawable
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 512
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    override fun showLoading() {
        if (isResumed)
            binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun hideLoading() {
        if (isResumed)
            binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onRefresh() {
        launch {
            viewModel.getOrderHistoryPager().collectLatest(::collectOrdersData)
        }

//        viewModel.getDrinksStat()
//        viewModel.generateQRCode()
    }

    override fun toString(): String {
        return TAG
    }

    companion object Companion {
        const val TAG = "QRCodeScreen"
    }

}