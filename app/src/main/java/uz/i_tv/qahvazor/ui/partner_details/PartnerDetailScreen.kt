package uz.i_tv.qahvazor.ui.partner_details

import android.os.Bundle
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.LocationData
import uz.i_tv.data.models.PartnerData
import uz.i_tv.data.models.ShopItemData
import uz.i_tv.data.models.UrlTypes.URL_TYPE_INSTAGRAM
import uz.i_tv.data.models.UrlTypes.URL_TYPE_WEB
import uz.i_tv.domain.ui.BaseActivity
import uz.i_tv.domain.utils.intentToBrowser
import uz.i_tv.domain.utils.intentToCall
import uz.i_tv.domain.utils.visible
import uz.i_tv.qahvazor.databinding.ScreenPartnerDetailsBinding
import uz.i_tv.qahvazor.ui.shop_details.ShopDetailBD.Companion.showShopDetailBD

class PartnerDetailScreen : BaseActivity() {

    private lateinit var binding: ScreenPartnerDetailsBinding
    private val viewModel: PartnerVM by viewModel()
    private val adapter = ShopsAdapter()
    private val drinksAdapter = DrinksAdapter()

    private val companyId: Int by lazy { intent.extras?.getInt(PARTNER_ID) ?: -1 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenPartnerDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.shopRv.adapter = adapter
        binding.drinkRv.adapter = drinksAdapter

        binding.toolbar.setNavigationOnClickListener { finish() }

        launch {
            viewModel.getPartnerDetail(companyId).collectLatest(::collectData)
        }
        launch {
            viewModel.getPartnerShops(companyId).collectLatest(::collectPartnerShops)
        }

        adapter.setOnItemClickListener {
            showShopDetailBD(it.shopId ?: -1)
        }

    }

    private fun collectData(t: UIResource<PartnerData>) = t.collect { partnerData ->
        binding.name.text = partnerData?.name
        binding.toolbar.title = partnerData?.name
        binding.desc.originalText = partnerData?.description.toString()

        drinksAdapter.submitList(partnerData?.drinks)

//        binding.mainImage.load(it?.logoUrl) {
////            transformations(BlurTransformation(this@PartnerDetailScreen))
//        }
        binding.image.load(partnerData?.logoUrl)

        val phoneNumber = partnerData?.phoneNumbers?.firstOrNull()
        if (phoneNumber != null) {
            binding.phoneNumber.visible()
            binding.phoneNumber.setOnClickListener {
                intentToCall(phoneNumber.phoneNumber.toString())
            }
        }

        partnerData?.urls?.forEach { item ->
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
            }
        }
    }

    private fun collectPartnerShops(t: UIResource<List<ShopItemData>>) =
        t.collect { shopItemDataList ->
            adapter.submitList(shopItemDataList)
        }

//    private fun getMapImageUrlByPoint(list: List<Point>): String {
//        var url = "http://static-maps.yandex.ru/1.x/?lang=en-US&"
//
//        list.forEachIndexed { index, point ->
//            if (index == 0) {
//                url = url.plus("ll=${point.longitude},${point.latitude}&")
//                url = url.plus("pt=${point.longitude},${point.latitude}")
//            } else
//                url = url.plus("~${point.longitude},${point.latitude}")
//        }
//
//        url = url.plus(",vkbkm&size=450,450&z=14&l=map")
//        return url
//    }

    private fun getMapImageUrl(long: String?, lat: String?): String {
        return "http://static-maps.yandex.ru/1.x/?lang=en-US&ll=$long,$lat&size=450,450&z=14&l=map&pt=$long,$lat,vkbkm"
    }

//    override fun onStop() {
//        binding.mapView.onStop()
//        MapKitFactory.getInstance().onStop()
//        super.onStop()
//    }
//
//    override fun onStart() {
//        super.onStart()
//        MapKitFactory.getInstance().onStart()
//        binding.mapView.onStart()
//    }

    companion object {
        const val PARTNER_ID = "partner_id"
    }
}