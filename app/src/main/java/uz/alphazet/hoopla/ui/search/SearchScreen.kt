package uz.alphazet.hoopla.ui.search

import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.hideKeyboard
import uz.alphazet.domain.utils.showKeyboard
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenSearchBinding
import uz.alphazet.hoopla.ui.home.NearShopAdapter
import uz.alphazet.hoopla.ui.navigateTo
import uz.alphazet.hoopla.ui.shop_details.ShopDetailScreen

/**
 * Two-step partner search backed by the public partner endpoints:
 *   1. Partners mode — search/list brands via GET /v1/partners/list.
 *   2. Shops mode — after tapping a partner, list its active shops via
 *      GET /v1/partners/shops (nearest-first when a location is passed).
 * Tapping a shop opens its detail screen.
 */
class SearchScreen : BaseFragment(R.layout.screen_search) {

    private val binding by viewBinding(ScreenSearchBinding::bind)
    private val viewModel: SearchVM by viewModel()

    private val partnerAdapter = PartnerAdapter()
    private val shopAdapter = NearShopAdapter(showDistance = true)

    private var selectedPartner: PartnerItemData? = null

    private val lat: Double by lazy { arguments?.getDouble(EXTRA_LAT, DEFAULT_LATITUDE) ?: DEFAULT_LATITUDE }
    private val long: Double by lazy { arguments?.getDouble(EXTRA_LONG, DEFAULT_LONGITUDE) ?: DEFAULT_LONGITUDE }

    override fun initialize() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        partnerAdapter.setOnItemClickListener { showPartnerShops(it) }

        shopAdapter.setOnItemClickListener {
            navigateTo(ShopDetailScreen.newInstance(it.shopId))
        }

        binding.inputSearch.doAfterTextChanged { text ->
            if (selectedPartner == null) loadPartners(text?.toString())
        }

        showPartners()
    }

    /** Shops mode collapses back to partners mode before the screen itself pops. */
    override fun onBackPressed(): Boolean {
        if (selectedPartner != null) {
            showPartners()
            return true
        }
        return false
    }

    private fun showPartners() {
        selectedPartner = null
        binding.toolbar.setTitle(uz.alphazet.domain.R.string.search)
        binding.inputSearchLayout.visible()
        binding.itemsRv.adapter = partnerAdapter

        binding.root.post {
            if (view == null) return@post
            binding.inputSearch.requestFocus()
            requireContext().showKeyboard()
        }
        loadPartners(binding.inputSearch.text?.toString())
    }

    private fun showPartnerShops(partner: PartnerItemData) {
        val partnerId = partner.id ?: return
        selectedPartner = partner
        hideKeyboard()
        binding.toolbar.title = partner.name
        binding.inputSearchLayout.gone()
        binding.itemsRv.adapter = shopAdapter
        shopAdapter.submitList(emptyList())

        launch {
            viewModel.getPartnerShops(partnerId, lat, long).collectLatest(::collectShops)
        }
    }

    private fun loadPartners(name: String?) {
        launch {
            viewModel.getPartners(name?.takeIf { it.isNotBlank() }).collectLatest(::collectPartners)
        }
    }

    private fun collectPartners(t: UIResource<List<PartnerItemData>>) = t.collect {
        partnerAdapter.submitList(it)
    }

    private fun collectShops(t: UIResource<List<ShopItemData>>) = t.collect {
        shopAdapter.submitList(it)
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LONG = "extra_long"

        // Fallback when no location is passed: Tashkent center.
        private const val DEFAULT_LATITUDE = 41.31125776157484
        private const val DEFAULT_LONGITUDE = 69.27957810360282

        fun newInstance(lat: Double? = null, long: Double? = null) = SearchScreen().apply {
            arguments = bundleOf(
                EXTRA_LAT to (lat ?: DEFAULT_LATITUDE),
                EXTRA_LONG to (long ?: DEFAULT_LONGITUDE),
            )
        }
    }
}
