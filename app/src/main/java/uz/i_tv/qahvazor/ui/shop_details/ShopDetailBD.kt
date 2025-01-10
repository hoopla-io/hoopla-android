package uz.i_tv.qahvazor.ui.shop_details

import android.content.Intent
import android.net.Uri
import coil3.load
import com.example.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.ShopData
import uz.i_tv.domain.ui.BaseActivity
import uz.i_tv.domain.ui.BaseBottomSheetDF
import uz.i_tv.domain.utils.Utils
import uz.i_tv.domain.utils.Utils.formatPhoneNumber
import uz.i_tv.domain.utils.intentToCall
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenShopDetailsBinding

class ShopDetailBD(private val shopId: Int) : BaseBottomSheetDF(R.layout.screen_shop_details) {

    private val binding by viewBinding(ScreenShopDetailsBinding::bind)
    private val viewModel: ShopVM by viewModel()

    override val screenSize: SheetSizes? = SheetSizes.FULLSCREEN

    private val imagesAdapter = ImagesAdapter()
    private val workTimeAdapter = WorkTimeAdapter()

    override fun initialize() {
        binding.imageViewPager.adapter = imagesAdapter
        binding.workTimeRv.adapter = workTimeAdapter

        imagesAdapter.setOnItemClickListenerWithPosition { item, position ->
            StfalconImageViewer.Builder(
                requireContext(), imagesAdapter.currentList
            ) { view, image ->
                view.load(image.pictureUrl)
            }.withStartPosition(position).show()
        }

        launch {
            viewModel.getShopDetail(shopId).collectLatest(::collectData)
        }
    }

    private fun collectData(t: UIResource<ShopData>) = t.collect { data ->
        binding.headerTitle.text = data?.name

        imagesAdapter.submitList(data?.pictures)
        workTimeAdapter.submitList(data?.workingHours)

        binding.mapImage.load(
            Utils.getMapImageUrl(
                data?.location?.lng.toString(),
                data?.location?.lat.toString()
            )
        )

        binding.maximizeImg.setOnClickListener {
            val uri =
                "http://maps.google.com/maps?f=d&hl=en&daddr=" + data?.location?.lat.toString() + "," + data?.location?.lng.toString()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(Intent.createChooser(intent, "Select an application"))
        }
        val phoneNumber = data?.phoneNumbers?.firstOrNull()
        if (phoneNumber != null) {
            binding.phoneNumber.text = phoneNumber.phoneNumber?.formatPhoneNumber()
            binding.phoneNumber.setOnClickListener {
                requireContext().intentToCall(phoneNumber.phoneNumber?.formatPhoneNumber() ?: "")
            }
        }
    }

    companion object {
        private const val TAG = "ShopDetailBD"

        fun BaseActivity.showShopDetailBD(shopId: Int) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                ShopDetailBD(shopId).show(supportFragmentManager, TAG)
            }
        }
    }

}