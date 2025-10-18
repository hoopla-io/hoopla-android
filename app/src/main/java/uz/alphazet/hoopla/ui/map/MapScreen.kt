package uz.alphazet.hoopla.ui.map

import android.content.Intent
import android.location.Location
import android.location.LocationManager
import androidx.activity.result.contract.ActivityResultContracts
import coil3.Bitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.ClusterListener
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
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

class MapScreen : BaseFragment(R.layout.screen_map), MapObjectTapListener,
    UserLocationObjectListener {

    private val binding by viewBinding(ScreenMapBinding::bind)
    private val viewModel: HomeVM by viewModel()

    private val permissionManager: PermissionManager by inject()

    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var mLocationManager: LocationManager? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationRequestBuilder: LocationRequest.Builder? = null

    private var locationResultListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            runLocationListener()
        }

    private val clusterListener = ClusterListener { cluster ->

    }

    private val defaultPoint: Point = Point(41.31125776157484, 69.27957810360282)
    private val defaultCameraPosition = CameraPosition(defaultPoint, 12.0f, 0.0f, 10.0f)

    private val shopListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_ORDER_CREATED -> {
                    (requireActivity() as? MainActivity)?.navigateToQRScreen()
                }
            }
        }

    override fun initialize() {

        launch {
            viewModel.getNearShops(defaultPoint.latitude, defaultPoint.longitude, null)
                .collectLatest(::collectNearShopsData)
        }

        binding.mapView.mapWindow.map.move(defaultCameraPosition)

        binding.mapView.mapWindow.map.mapObjects.addTapListener(this)

        val mapKit = MapKitFactory.getInstance()

        mapKit.createUserLocationLayer(binding.mapView.mapWindow).apply {
            isVisible = true
            isHeadingModeActive = true
            setObjectListener(this@MapScreen)
        }

    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    private fun collectNearShopsData(t: UIResource<List<ShopItemData>>) = t.collect { list ->
        val map = binding.mapView.mapWindow.map
        val pinsCollection = map.mapObjects.addCollection()

        val defaultImageProvider =
            ImageProvider.fromResource(requireContext(), uz.alphazet.domain.R.drawable.location)

        val imageLoader = ImageLoader(requireContext())

        list?.forEachIndexed { index, item ->
            val lat = item.location?.lat ?: 0.0
            val lng = item.location?.lng ?: 0.0
            val point = Point(lat, lng)

            launch {
                val bitmap = loadBitmapWithCoil(imageLoader, item.pictureUrl)
                val imageProvider =
                    bitmap?.let { ImageProvider.fromBitmap(it) } ?: defaultImageProvider

                val placemark = pinsCollection.addPlacemark(point, defaultImageProvider)

                placemark.userData = item
            }

        }
    }

    override fun onMapObjectTap(
        mapObject: MapObject,
        point: Point
    ): Boolean {
        val shopItemData = mapObject.userData as? ShopItemData

        val intent = Intent(requireContext(), ShopDetailActivity::class.java)
        intent.putExtra(SHOP_ID, shopItemData?.shopId)
        intent.putExtra(DISTANCE, shopItemData?.distance)
        shopListener.launch(intent)
        return true
    }

    private suspend fun loadBitmapWithCoil(imageLoader: ImageLoader, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(requireContext())
                    .data(url)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    result.image.toBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {

    }

    override fun onObjectRemoved(p0: UserLocationView) {

    }

    override fun onObjectUpdated(
        p0: UserLocationView,
        p1: ObjectEvent
    ) {

    }
}