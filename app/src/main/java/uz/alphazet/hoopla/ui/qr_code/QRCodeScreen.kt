package uz.alphazet.hoopla.ui.qr_code

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import coil3.load
import com.github.alexzhirkevich.customqrgenerator.QrData
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale
import com.github.alexzhirkevich.customqrgenerator.style.Color
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColors
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogo
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorShapes
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.OrderItemData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenQrCodeBinding

class QRCodeScreen : BaseFragment(R.layout.screen_qr_code) {

    private val binding by viewBinding(ScreenQrCodeBinding::bind)
    private val viewModel: QRCodeVM by viewModel()

    private val orderAdapter = OrderAdapter()

    private var time: Long = 0

    override fun initialize() {

        binding.orderRv.adapter = orderAdapter

        launch {
            viewModel.generateQRCode().collectLatest(::collectQRCodeData)
        }

        launch {
            viewModel.getOrders().collectLatest(::collectOrdersData)
        }

        binding.refresh.setOnClickListener {
            launch {
                viewModel.generateQRCode().collectLatest(::collectQRCodeData)
            }
        }

    }

    private fun collectQRCodeData(t: UIResource<QRCodeAccessData>) = t.collect(onLoading = {
        if (it)
            binding.refresh.playAnimation()
        else
            binding.refresh.pauseAnimation()
    }) {

        binding.qrCode.alpha = 1f
        binding.refresh.isEnabled = false
        binding.refresh.isClickable = false
        binding.refresh.gone()

        time = (it?.expireAt ?: 0L) - System.currentTimeMillis() / 1000L
        startCountDownTimer()
        val drawable = generateQRCodeImage(it?.qrCode.toString())
        binding.qrCode.load(drawable)
    }

    private fun collectOrdersData(t: UIResource<List<OrderItemData>>) = t.collect {
        orderAdapter.submitList(it)
    }

    private var countDownTimer: CountDownTimer? = null

    private fun startCountDownTimer() {
        countDownTimer = object : CountDownTimer(time * 1000, 1000) {

            override fun onTick(p0: Long) {
                val minute = time / 60
                val second = time % 60
                if (p0 < 0)
                    this.onFinish()
                else {
                    binding.timer.text = "${checkDigit(minute)}:" + checkDigit(second)
                    time--
                }
            }

            override fun onFinish() {
                binding.qrCode.alpha = 0.5f
                binding.timer.text = "00:00"
                binding.refresh.isEnabled = true
                binding.refresh.isClickable = true
                binding.refresh.visible()
            }
        }

        countDownTimer?.start()
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        super.onDestroyView()
    }

    private fun generateQRCodeImage(data: String): Drawable {
        val options = QrVectorOptions.Builder()
            .setPadding(.3f)
            .setLogo(
                QrVectorLogo(
                    drawable = ContextCompat
                        .getDrawable(requireContext(), uz.alphazet.domain.R.drawable.img_logo_hoopla),
                    size = .25f,
                    padding = QrVectorLogoPadding.Natural(.2f),
                    shape = QrVectorLogoShape
                        .Circle
                )
            )
            .setBackground(
                QrVectorBackground(
                    drawable = ContextCompat
                        .getDrawable(requireContext(), uz.alphazet.domain.R.drawable.bg_corner20_white),
                    scale = BitmapScale.CenterCrop
                )
            )
            .setColors(
                QrVectorColors(
                    dark = QrVectorColor
                        .Solid(Color(0xff345288)),
                    ball = QrVectorColor.Solid(
                        ContextCompat.getColor(requireContext(), uz.alphazet.domain.R.color.primary)
                    ),
                    frame = QrVectorColor.LinearGradient(
                        colors = listOf(
                            0f to Color.RED,
                            1f to Color.BLUE,
                        ),
                        orientation = QrVectorColor.LinearGradient
                            .Orientation.LeftDiagonal
                    )
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

    private fun checkDigit(number: Long): String = if (number <= 9) {
        "0$number"
    } else {
        number.toString()
    }

}