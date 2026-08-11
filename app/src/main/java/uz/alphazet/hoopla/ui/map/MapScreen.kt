package uz.alphazet.hoopla.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.permission.PermissionManager
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.formatDistance
import uz.alphazet.domain.utils.formatRating
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenMapBinding
import uz.alphazet.hoopla.ui.MainActivity
import uz.alphazet.hoopla.ui.home.HomeVM
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DISTANCE
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID

class MapScreen : BaseFragment(R.layout.screen_map), OnMapReadyCallback {

    private val binding by viewBinding(ScreenMapBinding::bind)
    private val viewModel: HomeVM by viewModel()

    private val permissionManager: PermissionManager by inject()

    private var googleMap: GoogleMap? = null
    private var clusterManager: ClusterManager<ShopClusterItem>? = null

    // Café currently previewed in the bottom info card, if any.
    private var selectedShop: ShopItemData? = null

    private val markerIconFactory by lazy { ShopMarkerIconFactory(requireContext()) }

    private val defaultPoint = LatLng(41.31125776157484, 69.27957810360282)
    private val defaultZoom = 12f

    private val shopListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_ORDER_CREATED -> {
                    (requireActivity() as? MainActivity)?.navigateToOrdersScreen()
                }
            }
        }

    override fun initialize() {
        binding.mapView.onCreate(null)
        binding.mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPoint, defaultZoom))

        setupClusterManager(map)
        setupShopCard()
        enableMyLocation(map)

        // Tapping empty map space dismisses the café info card.
        map.setOnMapClickListener { hideShopCard() }

        // Built-in Google place (POI) click listener.
        map.setOnPoiClickListener { poi ->
            Toast.makeText(requireContext(), poi.name, Toast.LENGTH_SHORT).show()
        }

        launch {
            viewModel.getNearShops(defaultPoint.latitude, defaultPoint.longitude, null)
                .collectLatest(::collectNearShopsData)
        }
    }

    private fun setupClusterManager(map: GoogleMap) {
        val manager = ClusterManager<ShopClusterItem>(requireContext(), map)
        manager.renderer = ShopClusterRenderer(
            requireContext(),
            map,
            manager,
            markerIconFactory,
            viewLifecycleOwner.lifecycleScope
        )

        map.setOnCameraIdleListener(manager)
        map.setOnMarkerClickListener(manager)

        manager.setOnClusterItemClickListener { item ->
            showShopCard(item.shop)
            map.animateCamera(CameraUpdateFactory.newLatLng(item.position))
            true
        }
        manager.setOnClusterClickListener { cluster ->
            hideShopCard()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    cluster.position,
                    map.cameraPosition.zoom + 1f
                )
            )
            true
        }

        clusterManager = manager
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation(map: GoogleMap) {
        permissionManager.checkFineCoarseLocationPermission(onAllow = {
            if (isAdded) {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
            }
        })
    }

    private fun collectNearShopsData(t: UIResource<List<ShopItemData>>) = t.collect { list ->
        val manager = clusterManager ?: return@collect
        manager.clearItems()
        list?.forEach { item ->
            if (item.location != null) manager.addItem(ShopClusterItem(item))
        }
        manager.cluster()
    }

    private fun openShopDetail(shop: ShopItemData) {
        val intent = Intent(requireContext(), ShopDetailActivity::class.java)
        intent.putExtra(SHOP_ID, shop.shopId)
        intent.putExtra(DISTANCE, shop.distance)
        shopListener.launch(intent)
    }

    /** Wires the info card's static click targets once; content is bound per tap. */
    private fun setupShopCard() = with(binding) {
        shopCard.setOnClickListener { selectedShop?.let(::openShopDetail) }
        cardOrder.setOnClickListener { selectedShop?.let(::openShopDetail) }
        cardClose.setOnClickListener { hideShopCard() }
    }

    /** Binds [shop] into the bottom info card and slides it into view. */
    private fun showShopCard(shop: ShopItemData) = with(binding) {
        selectedShop = shop

        cardName.text = shop.name
        cardImage.load(shop.pictureUrl)

        // Shop rating: "★ 4.7". Hidden only if the field is somehow absent.
        val rating = shop.rating
        cardRatingGroup.isVisible = rating != null
        if (rating != null) cardRating.text = rating.formatRating()

        val distance = shop.distance
        cardDistanceGroup.isVisible = distance != null
        if (distance != null) {
            cardDistance.text = requireContext().formatDistance(distance)
        }

        // The whole meta row goes away when neither value is known.
        cardMeta.isVisible = rating != null || distance != null

        // Shop is paused only when explicitly not accepting orders.
        val paused = shop.acceptingOrders == false
        cardPausedBadge.isVisible = paused
        cardOrder.isEnabled = !paused
        cardOrder.alpha = if (paused) 0.5f else 1f

        if (shopCard.isVisible) return@with

        shopCard.visibility = View.VISIBLE
        shopCard.alpha = 0f
        shopCard.post {
            shopCard.translationY = shopCard.height.toFloat()
            shopCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    /** Slides the info card out of view and clears the current selection. */
    private fun hideShopCard() = with(binding) {
        selectedShop = null
        if (!shopCard.isVisible) return@with

        shopCard.animate()
            .translationY(shopCard.height.toFloat())
            .alpha(0f)
            .setDuration(180)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                shopCard.visibility = View.GONE
                shopCard.translationY = 0f
            }
            .start()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        // MapView is part of the fragment's view hierarchy, so its lifecycle must
        // be tied to the view, not the fragment. Destroying it here (paired with
        // onCreate() in initialize()) keeps binding valid; doing it in onDestroy()
        // crashes because the view — and thus requireView() — is already gone.
        binding.mapView.onDestroy()
        googleMap = null
        clusterManager = null
        super.onDestroyView()
    }

    /**
     * A nearby shop rendered as a clusterable map item. Carries the original
     * [ShopItemData] so a marker tap can open [ShopDetailActivity].
     */
    private class ShopClusterItem(val shop: ShopItemData) : ClusterItem {
        // Paused only when explicitly false — matches the list overlay semantics.
        val paused: Boolean = shop.acceptingOrders == false

        private val latLng = LatLng(
            shop.location?.lat ?: 0.0,
            shop.location?.lng ?: 0.0
        )

        override fun getPosition(): LatLng = latLng
        override fun getTitle(): String? = shop.name
        override fun getSnippet(): String? = null
        override fun getZIndex(): Float? = null
    }

    /**
     * Renders each shop marker as a pin showing the café's logo. The branded
     * placeholder is shown immediately, then upgraded to the loaded logo once
     * [ShopMarkerIconFactory] resolves it. Built icons are cached per logo
     * URL so re-clustering on zoom neither reloads nor flickers. Clusters keep
     * the default bubble.
     */
    private class ShopClusterRenderer(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<ShopClusterItem>,
        private val iconFactory: ShopMarkerIconFactory,
        private val scope: CoroutineScope
    ) : DefaultClusterRenderer<ShopClusterItem>(context, map, clusterManager) {

        private val iconCache = mutableMapOf<String, BitmapDescriptor>()

        // Icons are cached per logo + paused state, since the same logo renders
        // differently (dimmed) when the shop is not accepting orders.
        private fun cacheKey(item: ShopClusterItem): String? {
            val url = item.shop.logoUrl
            if (url.isNullOrBlank()) return null
            return "$url|${item.paused}"
        }

        override fun onBeforeClusterItemRendered(
            item: ShopClusterItem,
            markerOptions: MarkerOptions
        ) {
            val icon = cacheKey(item)?.let(iconCache::get) ?: iconFactory.placeholderFor(item.paused)
            markerOptions.icon(icon).anchor(0.5f, 1f).title(item.title)
        }

        override fun onClusterItemRendered(item: ShopClusterItem, marker: Marker) {
            val key = cacheKey(item) ?: return
            if (iconCache.containsKey(key)) return
            scope.launch {
                val descriptor = iconFactory.create(item.shop.logoUrl, item.paused)
                iconCache[key] = descriptor
                runCatching { marker.setIcon(descriptor) }
            }
        }
    }
}