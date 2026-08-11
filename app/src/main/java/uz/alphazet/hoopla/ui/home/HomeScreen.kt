package uz.alphazet.hoopla.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.CategoryData
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.StoryItemData
import uz.alphazet.data.models.UserData
import uz.alphazet.hoopla.ui.home.FeedbackBD.Companion.showFeedbackBD
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.domain.permission.PermissionManager
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.log
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenHomeBinding
import uz.alphazet.hoopla.ui.MainActivity
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.orders.ActiveOrderAdapter
import uz.alphazet.hoopla.ui.orders.ActiveOrderStage
import uz.alphazet.hoopla.ui.orders.OrderInfoScreen
import uz.alphazet.hoopla.ui.orders.OrderQrCodeBD.Companion.showOrderQrCodeBD
import uz.alphazet.hoopla.ui.search.SearchScreen
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DISTANCE
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID


class HomeScreen : BaseFragment(R.layout.screen_home), SwipeRefreshLayout.OnRefreshListener,
    LocationListener {

    private val binding by viewBinding(ScreenHomeBinding::bind)
    private val viewModel: HomeVM by viewModel()

    private val permissionManager: PermissionManager by inject()

    private val adapter = NearShopAdapter()
    private val loyaltyAdapter = LoyaltyAdapter()
    private val categoryAdapter = CategoryAdapter()
    private val storyAdapter = StoryAdapter()
    private val activeOrderAdapter = ActiveOrderAdapter()

    private var selectedCategoryId: Int? = null

    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var mLocationManager: LocationManager? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationRequestBuilder: LocationRequest.Builder? = null

    private var forActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            runLocationListener()
        }

    private val shopListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_ORDER_CREATED -> {
                    (requireActivity() as? MainActivity)?.navigateToOrdersScreen()
                }
            }
        }

    private val notificationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.getUser()
        }

    private val orderInfoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.getActiveOrders()
        }

    private val storyViewerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
            val viewedIds = result.data?.getIntArrayExtra(StoryViewerActivity.VIEWED_IDS)
                ?: return@registerForActivityResult
            if (viewedIds.isEmpty()) return@registerForActivityResult
            val viewedSet = viewedIds.toHashSet()
            val updated = storyAdapter.currentList.map { story ->
                if (story.id != null && story.id in viewedSet && story.isSeen != true) {
                    story.copy(isSeen = true)
                } else story
            }
            storyAdapter.submitList(updated)
        }

    private val scanQrCodeLauncher = registerForActivityResult(ScanQRCode()) { result ->
        when (result) {
            is QRResult.QRSuccess -> {
                val rawValue = result.content.rawValue ?: ""
                requireActivity().intentToBrowser(rawValue)
//                launch {
//                    viewModel.scanQRCode(rawValue).collectLatest(::collectQRCodeScanData)
//                }
            }

            is QRResult.QRError -> {
                showErrorMessage(result.exception.message)
            }

            is QRResult.QRUserCanceled -> {}
            is QRResult.QRMissingPermission -> {}
        }
    }

    override fun initialize() {

        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.partnersRv.adapter = adapter
        binding.categoriesRv.adapter = categoryAdapter
        binding.storiesRv.adapter = storyAdapter
        binding.activeOrdersRv.adapter = activeOrderAdapter
        // Horizontal carousel: one full-width order card per swipe
        PagerSnapHelper().attachToRecyclerView(binding.activeOrdersRv)
//        binding.loyaltyRv.adapter = loyaltyAdapter

//        launch {
//            viewModel.getLoyaltyCard().collectLatest(::collectLoyalty)
//        }

        viewModel.getPendingFeedbacks()
        viewModel.getUser()
        viewModel.getCategories()
        viewModel.getStories()

        activeOrderAdapter.setOnItemClickListener(::openOrderInfo)
        activeOrderAdapter.setOnActionClickListener(::onActiveOrderAction)

        categoryAdapter.setOnItemClickListener { category ->
            val newId = if (selectedCategoryId == category.id) null else category.id
            selectedCategoryId = newId
            categoryAdapter.setSelectedId(newId)
            currentLocation?.let { reloadNearShops(it) }
        }

        launch {
            viewModel.pendingFeedbackFlow.collectLatest(::collectPendingFeedback)
        }

        launch {
            viewModel.userDataFlow.collectLatest(::collectUserData)
        }

        launch {
            viewModel.categoriesFlow.collectLatest(::collectCategories)
        }

        launch {
            viewModel.storiesFlow.collectLatest(::collectStories)
        }

        launch {
            viewModel.activeOrdersFlow.collectLatest(::collectActiveOrders)
        }

        storyAdapter.setOnItemClickListener { story ->
            val storyId = story.id ?: return@setOnItemClickListener
            if (story.isSeen != true) {
                val updated = storyAdapter.currentList.map {
                    if (it.id == storyId) it.copy(isSeen = true) else it
                }
                storyAdapter.submitList(updated)
            }
            val orderedIds = storyAdapter.currentList.mapNotNull { it.id }.toIntArray()
            val startIndex = orderedIds.indexOf(storyId).coerceAtLeast(0)
            val intent = Intent(requireContext(), StoryViewerActivity::class.java)
            intent.putExtra(StoryViewerActivity.STORY_IDS, orderedIds)
            intent.putExtra(StoryViewerActivity.START_INDEX, startIndex)
            storyViewerLauncher.launch(intent)
        }

        if (checkPermissionForLocationIsGranted()) {
            runLocationListener()
        } else {
            permissionManager.checkFineCoarseLocationPermission(
                onDeny = {
                    showErrorMessage(getString(uz.alphazet.domain.R.string.location_permission_not_full_granted))
                    loadDefaultLocationShops()
                },
                onAllow = { runLocationListener() }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionManager.checkPostNotificationPermission {}
        }

        adapter.setOnItemClickListener {
            val intent = Intent(requireContext(), ShopDetailActivity::class.java)
            intent.putExtra(SHOP_ID, it.shopId)
            intent.putExtra(DISTANCE, it.distance)
            shopListener.launch(intent)
        }

        binding.turnOnGPSContainer.setOnClickListener {
            turnOnGPS()
        }

        binding.scan.setOnClickListener {
            scanQrCodeLauncher.launch(null)
        }

        binding.notification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsScreen::class.java)
            notificationLauncher.launch(intent)
        }

        binding.search.setOnClickListener {
            val intent = Intent(requireContext(), SearchScreen::class.java)
            currentLocation?.let {
                intent.putExtra(SearchScreen.EXTRA_LAT, it.latitude)
                intent.putExtra(SearchScreen.EXTRA_LONG, it.longitude)
            }
            val options = ActivityOptionsCompat.makeCustomAnimation(
                requireContext(),
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            shopListener.launch(intent, options)
        }

        animateEntrance()
    }

    private fun animateEntrance() {
        val interpolator = DecelerateInterpolator(1.5f)
        val duration = 450L

        binding.logo.alpha = 0f
        binding.logo.translationY = -20f
        binding.logo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        binding.notificationCard.alpha = 0f
        binding.notificationCard.scaleX = 0.8f
        binding.notificationCard.scaleY = 0.8f
        binding.notificationCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setStartDelay(100)
            .setInterpolator(interpolator)
            .start()

        binding.searchCard.alpha = 0f
        binding.searchCard.scaleX = 0.8f
        binding.searchCard.scaleY = 0.8f
        binding.searchCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setStartDelay(50)
            .setInterpolator(interpolator)
            .start()

        binding.nearlyTitle.alpha = 0f
        binding.nearlyTitle.translationY = 20f
        binding.nearlyTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(duration)
            .setStartDelay(250)
            .setInterpolator(interpolator)
            .start()

        binding.scan.alpha = 0f
        binding.scan.translationY = 60f
        binding.scan.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(400)
            .setInterpolator(interpolator)
            .start()
    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect(onLoading = {}) { data ->
        val unreadCount = data?.unreadNotifications ?: 0
        if (unreadCount > 0) {
            binding.notificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            binding.notificationBadge.visible()
        } else {
            binding.notificationBadge.gone()
        }
    }

    private fun collectCategories(t: UIResource<List<CategoryData>>) = t.collect(onLoading = {}) { data ->
        if (!data.isNullOrEmpty()) {
            categoryAdapter.submitList(data)
            binding.categoriesRv.visible()
        }
    }

    private fun collectStories(t: UIResource<List<StoryItemData>>) = t.collect(onLoading = {}) { data ->
        if (!data.isNullOrEmpty()) {
            storyAdapter.submitList(data)
            binding.storiesRv.visible()
        } else {
            binding.storiesRv.gone()
        }
    }

    private fun collectActiveOrders(t: UIResource<List<OrderItemData>>) =
        t.collect(onLoading = {}, onError = {}) { data ->
            if (!data.isNullOrEmpty()) {
                activeOrderAdapter.submitList(data)
                applyActiveOrderPeek(data.size > 1)
                binding.activeOrdersRv.visible()
            } else {
                binding.activeOrdersRv.gone()
            }
        }

    private fun openOrderInfo(order: OrderItemData) {
        val intent = Intent(requireActivity(), OrderInfoScreen::class.java)
        intent.putExtra(Constants.ID, order.id ?: -1)
        orderInfoLauncher.launch(intent)
    }

    /**
     * The one thing the order's current stage asks of the customer: pay for it, or show the QR
     * that hands it over. Anything else — a cancelled or failed order — has no single next step,
     * so it opens the details where the reason and the retry live.
     */
    private fun onActiveOrderAction(order: OrderItemData) {
        when (ActiveOrderStage.of(order.orderStatus)) {
            ActiveOrderStage.AwaitingPayment -> {
                val checkoutUrl = order.checkoutUrl
                // A payment link can expire or never arrive; the details screen can still cancel
                // the order or start the payment again, so it is the safe place to land.
                if (checkoutUrl.isNullOrBlank()) openOrderInfo(order)
                else requireActivity().intentToBrowser(checkoutUrl)
            }

            ActiveOrderStage.NeedsAttention -> openOrderInfo(order)

            else -> order.id?.let { showOrderQrCodeBD(it) } ?: openOrderInfo(order)
        }
    }

    /**
     * The cards are match_parent wide, so their width is the list width minus its horizontal
     * padding. [PagerSnapHelper] centers on the list bounds (padding is not clipped), so widening
     * the padding shrinks every card and lets the neighbouring order peek in on both sides — the
     * hint that the carousel is swipeable. With a single order there is nothing to peek at, so the
     * card keeps the full width the rest of the screen uses.
     */
    private fun applyActiveOrderPeek(peek: Boolean) {
        val density = resources.displayMetrics.density
        val padding = ((if (peek) PEEK_PADDING_DP else NO_PEEK_PADDING_DP) * density).toInt()
        binding.activeOrdersRv.updatePadding(left = padding, right = padding)
    }

    private fun collectPendingFeedback(t: UIResource<FeedbackDetail>) = t.collect { data ->
        if (data != null) {
            showFeedbackBD(data) { viewModel.getPendingFeedbacks() }
        }
    }

    private fun collectNearShopsData(t: UIResource<List<ShopItemData>>) = t.collect {
        adapter.submitList(it)
    }

    private fun getNearShops(location: Location) {
        launch {
            viewModel.getNearShops(location.latitude, location.longitude, null, selectedCategoryId)
                .collectLatest(::collectNearShopsData)
        }
    }

    private fun reloadNearShops(location: Location) {
        launch {
            viewModel.getNearShops(location.latitude, location.longitude, null, selectedCategoryId)
                .collectLatest(::collectNearShopsData)
        }
    }

    /**
     * Fallback used when device GPS is off or location permission is denied:
     * load nearby shops around a default location and keep the screen usable
     * (category filter / pull-to-refresh rely on [currentLocation]).
     */
    private fun loadDefaultLocationShops() {
        binding.turnOnGPSContainer.gone()
        binding.partnersRv.visible()
        adapter.showDistance = false
        showLoading()
        val defaultLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = DEFAULT_LATITUDE
            longitude = DEFAULT_LONGITUDE
        }
        currentLocation = defaultLocation
        getNearShops(defaultLocation)
    }

    override fun onUnauthorizedException(message: String?, code: Int) {}

    private fun checkPermissionForLocationIsGranted(): Boolean = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
            &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun runLocationListener() {
        try {
            if (mLocationManager == null) mLocationManager =
                requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager

            val isGPSEnabled =
                mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (isGPSEnabled) {
                if (!checkPermissionForLocationIsGranted()) {
                    showErrorMessage(getString(uz.alphazet.domain.R.string.location_permission_not_full_granted))
                    loadDefaultLocationShops()
                    return
                }
                binding.turnOnGPSContainer.gone()
                binding.partnersRv.visible()
                adapter.showDistance = true
                showLoading()
                startFusedLocationListener()
            } else {
                showErrorMessage(getString(uz.alphazet.domain.R.string.please_turn_on_gps))
                loadDefaultLocationShops()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFusedLocationListener() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        locationRequestBuilder = LocationRequest.Builder(MIN_TIME_BW_UPDATES)
        locationRequestBuilder?.setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE_FOR_UPDATES)
        locationRequestBuilder?.setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        locationRequestBuilder?.setWaitForAccurateLocation(true)
        locationRequest = locationRequestBuilder?.build()

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest!!)
        val locationSettingsRequest = builder.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentLocation = locationResult.lastLocation
                currentLocation?.let { getNearShops(it) }
            }
        }

        if (locationRequest != null) {
            settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener { _: LocationSettingsResponse? ->
                    fusedLocationProviderClient!!
                        .requestLocationUpdates(
                            locationRequest!!,
                            locationCallback!!,
                            Looper.getMainLooper()
                        )
                }
                .addOnFailureListener { t: java.lang.Exception ->
                    t.log("Exception Catch")
                }
        }
    }

    private fun turnOnGPS() {
        val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        forActivityResult.launch(intent)
    }

    override fun onLocationChanged(location: Location) {
        showErrorMessage("${location.latitude}${location.longitude}")
        getNearShops(location)
    }

    override fun showLoading() {
        if (isResumed)
            binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun hideLoading() {
        if (isResumed)
            binding.swipeRefreshLayout.isRefreshing = false
    }

    // Refresh active orders every time the home screen is shown: onResume covers the
    // first open and returning from background; onHiddenChanged covers bottom-nav tab
    // switches, where the fragment is shown/hidden without an onResume.
    override fun onResume() {
        super.onResume()
        viewModel.getActiveOrders()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) viewModel.getActiveOrders()
    }

    override fun onRefresh() {
        viewModel.getCategories()
        viewModel.getActiveOrders()
        if (currentLocation != null)
            launch {
                viewModel.getNearShops(
                    currentLocation?.latitude ?: 0.0,
                    currentLocation?.longitude ?: 0.0,
                    null,
                    selectedCategoryId
                ).collectLatest(::collectNearShopsData)
            }
//        launch {
//            viewModel.getLoyaltyCard().collectLatest(::collectLoyalty)
//        }
    }

    override fun onDestroy() {
        if (mLocationManager != null) {
            mLocationManager?.removeUpdates(this)
            mLocationManager = null
        }

        if (locationCallback != null) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
        }

        super.onDestroy()
    }

    override fun toString(): String {
        return TAG
    }

    companion object {
        const val TAG = "HOME_SCREEN"

        const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10 meters
        const val MIN_TIME_BW_UPDATES = 3 * 1000L // 3 sec

        // Horizontal padding of the active-order carousel: matches the 16dp screen margin once the
        // card's own 6dp margin is added, or widens to let the next card peek in.
        private const val NO_PEEK_PADDING_DP = 10f
        private const val PEEK_PADDING_DP = 30f

        // Fallback when device GPS is off: Tashkent center
        // (same coords used by MapScreen / SearchScreen).
        const val DEFAULT_LATITUDE = 41.31125776157484
        const val DEFAULT_LONGITUDE = 69.27957810360282
    }

}