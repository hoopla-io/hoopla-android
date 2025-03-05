package uz.alphazet.hoopla.ui.shop_details

import android.content.Intent
import android.net.Uri
import coil3.load
import com.example.imageviewer.StfalconImageViewer
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.UrlTypes.URL_TYPE_FACEBOOK
import uz.alphazet.data.models.UrlTypes.URL_TYPE_INSTAGRAM
import uz.alphazet.data.models.UrlTypes.URL_TYPE_WEB
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.getMapImageUrl
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.intentToCall
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenShopDetailsBinding

class ShopDetailBD(private val shopId: Int) : BaseBottomSheetDF(R.layout.screen_shop_details) {

    private val binding by viewBinding(ScreenShopDetailsBinding::bind)
    private val viewModel: ShopVM by viewModel()

    override val screenSize: SheetSizes? = SheetSizes.FULLSCREEN

    private val imagesAdapter = ImagesAdapter()
    private val drinksAdapter = DrinksAdapter()
    private val workTimeAdapter = WorkTimeAdapter()

    override fun initialize() {
        binding.imageViewPager.adapter = imagesAdapter
        binding.drinkRv.adapter = drinksAdapter
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
        drinksAdapter.submitList(data?.drinks)

        binding.mapImage.load(
            getMapImageUrl(data?.location?.lng.toString(), data?.location?.lat.toString())
        )

        binding.maximizeImg.setOnClickListener {
            val uri =
                "http://maps.google.com/maps?f=d&hl=en&daddr=" + data?.location?.lat.toString() + "," + data?.location?.lng.toString()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(Intent.createChooser(intent, "Select an application"))
        }

        val phoneNumber = data?.phoneNumbers?.firstOrNull()
        if (phoneNumber != null) {
            binding.phoneNumber.visible()
            binding.phoneNumber.text = phoneNumber.phoneNumber?.formatPhoneNumber()
            binding.phoneNumber.setOnClickListener {
                requireContext().intentToCall(phoneNumber.phoneNumber?.formatPhoneNumber() ?: "")
            }
        }

        data?.urls?.forEach { item ->
            when (item.urlType) {
                URL_TYPE_WEB -> {
                    binding.web.visible()
                    binding.web.setOnClickListener {
                        requireContext().intentToBrowser(item.url.toString())
                    }
                }

                URL_TYPE_INSTAGRAM -> {
                    binding.instagram.visible()
                    binding.instagram.setOnClickListener {
                        requireContext().intentToBrowser(item.url.toString())
                    }
                }

                URL_TYPE_FACEBOOK -> {
                    binding.facebook.visible()
                    binding.facebook.setOnClickListener {
                        requireContext().intentToBrowser(item.url.toString())
                    }
                }

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

        fun BaseFragment.showShopDetailBD(shopId: Int) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                ShopDetailBD(shopId).show(childFragmentManager, TAG)
            }
        }

    }

}