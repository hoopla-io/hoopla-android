package uz.i_tv.qahvazor.ui.qr_code

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import coil3.load
import com.github.alexzhirkevich.customqrgenerator.QrData
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
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.QRCodeAccessData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenQrCodeBinding

class QRCodeScreen : BaseFragment(R.layout.screen_qr_code) {

    private val binding by viewBinding(ScreenQrCodeBinding::bind)
    private val viewModel: QRCodeVM by viewModel()

    override fun initialize() {

        launch {
            viewModel.generateQRCode().collectLatest(::collectQRCodeData)
        }

    }

    private fun collectQRCodeData(t: UIResource<QRCodeAccessData>) = t.collect {
        val drawable = generateQRCodeImage(it?.qrCode.toString())
        binding.qrCode.load(drawable)
    }

    private fun generateQRCodeImage(data: String): Drawable {
        val options = QrVectorOptions.Builder()
            .setPadding(.3f)
            .setLogo(
                QrVectorLogo(
                    drawable = ContextCompat
                        .getDrawable(requireContext(), uz.i_tv.domain.R.drawable.logo_img_text),
                    size = .25f,
                    padding = QrVectorLogoPadding.Natural(.2f),
                    shape = QrVectorLogoShape
                        .Circle
                )
            )
            .setBackground(
                QrVectorBackground(
                    drawable = ContextCompat
                        .getDrawable(requireContext(), uz.i_tv.domain.R.drawable.bg_corner20_white),
                )
            )
            .setColors(
                QrVectorColors(
                    dark = QrVectorColor
                        .Solid(Color(0xff345288)),
                    ball = QrVectorColor.Solid(
                        ContextCompat.getColor(requireContext(), uz.i_tv.domain.R.color.primary)
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

}