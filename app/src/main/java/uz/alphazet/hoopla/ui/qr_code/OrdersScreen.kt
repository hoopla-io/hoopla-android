package uz.alphazet.hoopla.ui.qr_code

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.result.contract.ActivityResultContracts
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
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.views.imageviewer.StfalconImageViewer
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenOrdersBinding

class OrdersScreen : BaseFragment(R.layout.screen_orders), SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenOrdersBinding::bind)
    private val viewModel: QRCodeVM by viewModel()

    private val orderAdapter = OrderAdapter()

    private var imageViewer: StfalconImageViewer<Drawable>? = null

    private val resultListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            launch {
                viewModel.getOrderHistoryPager().collectLatest(::collectOrdersData)
            }
        }

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
                    }

                    is LoadState.Loading -> {
                        binding.swipeRefreshLayout.isRefreshing = true
                    }

                    is LoadState.Error -> {
                        binding.swipeRefreshLayout.isRefreshing = false
                        val throwable = (loadState.refresh as LoadState.Error).error
                        showErrorMessage(throwable.message)
                    }

                }
            }
        }

        orderAdapter.setOnItemClickListener {
            val intent = Intent(requireActivity(), OrderInfoScreen::class.java)
            intent.putExtra(Constants.ID, it?.id ?: -1)
            resultListener.launch(intent)
        }

    }

    private fun collectQRCodeData(t: UIResource<QRCodeAccessData>) = t.collect(onLoading = {}) {
        val drawable = generateQRCodeImage(it?.qrCode.toString())
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

    private suspend fun collectOrdersData(t: PagingData<OrderItemData>) {
        orderAdapter.submitData(t)
    }

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