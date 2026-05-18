package uz.alphazet.hoopla.ui.orders

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
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
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogOrderQrCodeBinding

class OrderQrCodeBD : BaseBottomSheetDF(R.layout.dialog_order_qr_code) {

    private val binding by viewBinding(DialogOrderQrCodeBinding::bind)
    private val viewModel: OrdersVM by viewModel()

    private val orderId: Int by lazy { arguments?.getInt(Constants.ID, -1) ?: -1 }

    override fun initialize() {

        binding.close.setOnClickListener { dismiss() }

        launch {
            viewModel.pickupQrFlow.collectLatest(::collectPickupQr)
        }

        viewModel.getOrderPickupQR(orderId)
    }

    private fun collectPickupQr(t: UIResource<QRCodeAccessData>) = t.collect(
        onLoading = { loading ->
            if (loading) binding.progress.visible() else binding.progress.gone()
        }
    ) { data ->
        val payload = data?.token.orEmpty()
        if (payload.isBlank()) return@collect

        val orderId = data?.orderId
        if (orderId != null && orderId > 0) {
            binding.qrCodeText.text = orderId.toString()
            binding.qrCodeText.visible()
        } else {
            binding.qrCodeText.gone()
        }

        val drawable = generateQRCodeImage(payload)
        val bitmap = drawableToBitmap(drawable)
        binding.qrCode.load(bitmap.toDrawable(resources))

        val expiresAt = data?.expiresAt
        if (expiresAt != null && expiresAt > 0) {
            binding.expireAt.visible()
            binding.expireAt.text = getString(
                uz.alphazet.domain.R.string.label_valid_until_,
                expiresAt.getDateDDMMMMYYYYHHmm()
            )
        } else {
            binding.expireAt.gone()
        }
    }

    private fun generateQRCodeImage(data: String): Drawable {
        val options = QrVectorOptions.Builder()
            .setPadding(0.2f)
            .setBackground(
                QrVectorBackground(
                    drawable = ContextCompat.getDrawable(
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
                    darkPixel = QrVectorPixelShape.RoundCorners(.5f),
                    ball = QrVectorBallShape.RoundCorners(.25f),
                    frame = QrVectorFrameShape.RoundCorners(.25f),
                )
            )
            .build()

        return QrCodeDrawable(QrData.Url(data), options)
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

    companion object {
        private const val TAG = "OrderQrCodeBD"

        private fun newInstance(orderId: Int): OrderQrCodeBD = OrderQrCodeBD().apply {
            arguments = android.os.Bundle().apply { putInt(Constants.ID, orderId) }
        }

        fun BaseActivity.showOrderQrCodeBD(orderId: Int) {
            if (supportFragmentManager.findFragmentByTag(TAG) == null) {
                newInstance(orderId).show(supportFragmentManager, TAG)
            }
        }

        fun BaseFragment.showOrderQrCodeBD(orderId: Int) {
            if (childFragmentManager.findFragmentByTag(TAG) == null) {
                newInstance(orderId).show(childFragmentManager, TAG)
            }
        }
    }
}