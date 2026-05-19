package uz.alphazet.hoopla.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.permission.PermissionManager
import uz.alphazet.domain.ui.BaseFragment
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
        enableMyLocation(map)

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
        manager.renderer = ShopClusterRenderer(requireContext(), map, manager)

        map.setOnCameraIdleListener(manager)
        map.setOnMarkerClickListener(manager)

        manager.setOnClusterItemClickListener { item ->
            openShopDetail(item.shop)
            true
        }
        manager.setOnClusterClickListener { cluster ->
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

    override fun onDestroy() {
        binding.mapView.onDestroy()
        googleMap = null
        clusterManager = null
        super.onDestroy()
    }

    /**
     * A nearby shop rendered as a clusterable map item. Carries the original
     * [ShopItemData] so a marker tap can open [ShopDetailActivity].
     */
    private class ShopClusterItem(val shop: ShopItemData) : ClusterItem {
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
     * Renders individual shop markers with the shared `location` pin (parity
     * with the previous Yandex placemark icon); clusters keep the default
     * bubble.
     */
    private class ShopClusterRenderer(
        context: Context,
        map: GoogleMap,
        clusterManager: ClusterManager<ShopClusterItem>
    ) : DefaultClusterRenderer<ShopClusterItem>(context, map, clusterManager) {

        private val pinIcon by lazy {
            BitmapDescriptorFactory.fromResource(uz.alphazet.domain.R.drawable.location)
        }

        override fun onBeforeClusterItemRendered(
            item: ShopClusterItem,
            markerOptions: MarkerOptions
        ) {
            markerOptions.icon(pinIcon).title(item.title)
        }
    }
}