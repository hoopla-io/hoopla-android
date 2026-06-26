package uz.alphazet.hoopla.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.hideKeyboard
import uz.alphazet.domain.utils.showKeyboard
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenSearchBinding
import uz.alphazet.hoopla.ui.home.NearShopAdapter
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DISTANCE
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID

/**
 * Two-step partner search backed by the public partner endpoints:
 *   1. Partners mode — search/list brands via GET /v1/partners/list.
 *   2. Shops mode — after tapping a partner, list its active shops via
 *      GET /v1/partners/shops (nearest-first when a location is passed).
 * Tapping a shop opens its detail screen.
 */
class SearchScreen : BaseActivity() {

    private lateinit var binding: ScreenSearchBinding
    private val viewModel: SearchVM by viewModel()

    private val partnerAdapter = PartnerAdapter()
    private val shopAdapter = NearShopAdapter(showDistance = true)

    private var selectedPartner: PartnerItemData? = null

    private val lat: Double by lazy { intent.getDoubleExtra(EXTRA_LAT, DEFAULT_LATITUDE) }
    private val long: Double by lazy { intent.getDoubleExtra(EXTRA_LONG, DEFAULT_LONGITUDE) }

    private val shopListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_ORDER_CREATED -> {
                    setResult(result.resultCode)
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedPartner != null) showPartners() else finish()
            }
        })

        partnerAdapter.setOnItemClickListener { showPartnerShops(it) }

        shopAdapter.setOnItemClickListener {
            val intent = Intent(this, ShopDetailActivity::class.java)
            intent.putExtra(SHOP_ID, it.shopId)
            intent.putExtra(DISTANCE, it.distance)
            shopListener.launch(intent)
        }

        binding.inputSearch.doAfterTextChanged { text ->
            if (selectedPartner == null) loadPartners(text?.toString())
        }

        showPartners()
    }

    private fun showPartners() {
        selectedPartner = null
        binding.toolbar.setTitle(uz.alphazet.domain.R.string.search)
        binding.inputSearchLayout.visible()
        binding.itemsRv.adapter = partnerAdapter

        binding.root.post {
            binding.inputSearch.requestFocus()
            showKeyboard()
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

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LONG = "extra_long"

        // Fallback when no location is passed: Tashkent center.
        private const val DEFAULT_LATITUDE = 41.31125776157484
        private const val DEFAULT_LONGITUDE = 69.27957810360282
    }
}