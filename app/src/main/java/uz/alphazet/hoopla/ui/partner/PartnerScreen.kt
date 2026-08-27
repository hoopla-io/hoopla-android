package uz.alphazet.hoopla.ui.partner

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenPartnerBinding
import uz.alphazet.hoopla.ui.home.NearShopAdapter
import uz.alphazet.hoopla.ui.navigateTo
import uz.alphazet.hoopla.ui.popScreen
import uz.alphazet.hoopla.ui.shop_details.ShopDetailScreen
import uz.alphazet.domain.R as DomainR

/**
 * One brand and the coffee shops it runs. Reached from a story's `partner` link — which carries
 * a partner id and nothing else — and from anywhere else already holding a [PartnerItemData].
 *
 * There is no partner-detail endpoint, so an id-only entry resolves its header out of the
 * partners list. That lookup runs beside the shop request rather than in front of it: the shops
 * are the point of the screen, and a header that never arrives must not cost them.
 */
class PartnerScreen : BaseFragment(R.layout.screen_partner),
    SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenPartnerBinding::bind)
    private val viewModel: PartnerVM by viewModel()

    /** A distance is only meaningful when the caller actually had a fix — see [EXTRA_HAS_LOCATION]. */
    private val shopAdapter by lazy { NearShopAdapter(showDistance = hasLocation) }

    private val partnerId: Int by lazy { arguments?.getInt(EXTRA_PARTNER_ID, -1) ?: -1 }
    private val hasLocation: Boolean by lazy {
        arguments?.getBoolean(EXTRA_HAS_LOCATION, false) ?: false
    }
    private val lat: Double by lazy {
        arguments?.getDouble(EXTRA_LAT, DEFAULT_LATITUDE) ?: DEFAULT_LATITUDE
    }
    private val long: Double by lazy {
        arguments?.getDouble(EXTRA_LONG, DEFAULT_LONGITUDE) ?: DEFAULT_LONGITUDE
    }

    private var partnerName: String? = null
    private var partnerLogo: String? = null
    private var shopsCount: Int? = null

    /** True once a list has rendered, so later refreshes never blank the screen out. */
    private var hasLoaded = false

    override fun initialize() {
        // A link can only have been authored wrong; there is no screen to show without an id.
        if (partnerId <= 0) {
            popScreen()
            return
        }

        binding.toolbar.setNavigationOnClickListener { popScreen() }
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.shopsRv.adapter = shopAdapter
        binding.retry.setOnClickListener { loadShops() }

        shopAdapter.setOnItemClickListener {
            navigateTo(ShopDetailScreen.newInstance(it.shopId))
        }

        partnerName = arguments?.getString(EXTRA_PARTNER_NAME)
        partnerLogo = arguments?.getString(EXTRA_PARTNER_LOGO)
        renderHeader()

        if (partnerName.isNullOrBlank()) loadPartner()
        loadShops()
    }

    override fun onRefresh() = loadShops()

    private fun loadPartner() {
        launch {
            viewModel.getPartner(partnerId).collectLatest(::collectPartner)
        }
    }

    private fun loadShops() {
        // `BaseRepo.handleFlow` emits only its terminal result — never [UIResource.Loading] — so
        // the spinner is raised here rather than from `collect`'s onLoading, which never fires.
        if (hasLoaded) binding.swipeRefreshLayout.isRefreshing = true else binding.progress.visible()
        launch {
            viewModel.getPartnerShops(partnerId, lat, long).collectLatest(::collectShops)
        }
    }

    /**
     * The header is a nicety: it must not drive the list's spinner, and a brand that cannot be
     * resolved — deactivated since the story was authored — is not worth a toast over a screen
     * whose shops loaded fine.
     */
    private fun collectPartner(t: UIResource<PartnerItemData>) {
        if (view == null) return
        t.collect(onLoading = {}, onError = {}) { partner ->
            partnerName = partner?.name
            partnerLogo = partner?.logoUrl
            renderHeader()
        }
    }

    private fun collectShops(t: UIResource<List<ShopItemData>>) {
        if (view == null) return
        t.collect(onLoading = {}, onError = ::showLoadFailure) { data ->
            hideProgress()
            hasLoaded = true

            val shops = data.orEmpty()
            shopsCount = shops.size
            renderHeader()
            shopAdapter.submitList(shops)

            binding.errorState.gone()
            binding.shopsRv.isVisible = shops.isNotEmpty()
            binding.emptyState.isVisible = shops.isEmpty()
        }
    }

    private fun renderHeader() {
        val name = partnerName?.takeIf { it.isNotBlank() }
        binding.header.isVisible = name != null
        binding.name.text = name

        val logo = partnerLogo
        binding.logo.isVisible = !logo.isNullOrBlank()
        if (!logo.isNullOrBlank()) binding.logo.load(logo)

        val count = shopsCount
        binding.shopsCount.isVisible = count != null && count > 0
        if (count != null && count > 0) binding.shopsCount.text =
            resources.getQuantityString(DomainR.plurals.partner_shops_count, count, count)
    }

    /**
     * A first load that fails owns the screen; a refresh that fails does not — replacing a list
     * the customer is already reading with an error page loses their place over a blip.
     */
    private fun showLoadFailure(throwable: Throwable) {
        if (view == null) return
        hideProgress()

        val message = throwable.message?.takeIf { it.isNotBlank() }
            ?: getString(DomainR.string.partner_load_failed)

        if (hasLoaded) {
            showErrorMessage(message)
            return
        }
        binding.shopsRv.gone()
        binding.emptyState.gone()
        binding.errorMessage.text = message
        binding.errorState.visible()
    }

    private fun hideProgress() {
        binding.progress.gone()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    companion object {
        const val EXTRA_PARTNER_ID = "extra_partner_id"
        const val EXTRA_PARTNER_NAME = "extra_partner_name"
        const val EXTRA_PARTNER_LOGO = "extra_partner_logo"
        const val EXTRA_HAS_LOCATION = "extra_has_location"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LONG = "extra_long"

        // Fallback when no location is passed: Tashkent center — the same coordinates
        // HomeScreen, SearchScreen and MapScreen fall back to.
        private const val DEFAULT_LATITUDE = 41.31125776157484
        private const val DEFAULT_LONGITUDE = 69.27957810360282

        fun newInstance(
            partnerId: Int,
            name: String? = null,
            logoUrl: String? = null,
            lat: Double? = null,
            long: Double? = null,
        ) = PartnerScreen().apply {
            arguments = bundleOf(
                EXTRA_PARTNER_ID to partnerId,
                EXTRA_PARTNER_NAME to name,
                EXTRA_PARTNER_LOGO to logoUrl,
                EXTRA_HAS_LOCATION to (lat != null && long != null),
                EXTRA_LAT to (lat ?: DEFAULT_LATITUDE),
                EXTRA_LONG to (long ?: DEFAULT_LONGITUDE),
            )
        }

        /** The caller already holds the brand: the header paints without a lookup. */
        fun newInstance(partner: PartnerItemData, lat: Double? = null, long: Double? = null) =
            newInstance(partner.id ?: -1, partner.name, partner.logoUrl, lat, long)
    }
}
