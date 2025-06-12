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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.permission.PermissionManager
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.log
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenHomeBinding
import uz.alphazet.hoopla.ui.shop_details.ShopDetailBD.Companion.showShopDetailBD


class HomeScreen : BaseFragment(R.layout.screen_home), SwipeRefreshLayout.OnRefreshListener,
    LocationListener {

    private val binding by viewBinding(ScreenHomeBinding::bind)
    private val viewModel: HomeVM by viewModel()

    private val permissionManager: PermissionManager by inject()

    private val adapter = NearShopAdapter()

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

    override fun initialize() {

        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.partnersRv.adapter = adapter

        if (checkPermissionForLocationIsGranted()) {
            runLocationListener()
        } else {
            permissionManager.checkFineCoarseLocationPermission {
                runLocationListener()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionManager.checkPostNotificationPermission {}
        }

        adapter.setOnItemClickListener {
            showShopDetailBD(it.shopId ?: -1)
        }

        binding.turnOnGPSContainer.setOnClickListener {
            turnOnGPS()
        }

        binding.inputSearch.doAfterTextChanged { text ->
            if (!text.isNullOrEmpty() && currentLocation != null) {
                launch {
                    viewModel.getNearShops(
                        currentLocation?.latitude ?: 0.0,
                        currentLocation?.longitude ?: 0.0,
                        text.toString()
                    ).collectLatest(::collectNearShopsData)
                }
            } else if (currentLocation != null) {
                getNearShops(currentLocation!!)
            }
        }

    }

    private fun collectNearShopsData(t: UIResource<List<ShopItemData>>) = t.collect(

    ) {
        adapter.submitList(it)
    }

    private fun getNearShops(location: Location) {
        launch {
            viewModel.getNearShops(location.latitude, location.longitude, null)
                .collectLatest(::collectNearShopsData)
        }
    }

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

            val isNetworkEnabled =
                mLocationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            val isGPSEnabled =
                mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            if (isGPSEnabled) {
                if (!checkPermissionForLocationIsGranted()) {
                    showErrorMessage(getString(uz.alphazet.domain.R.string.location_permission_not_full_granted))
                    return
                }
                binding.turnOnGPSContainer.gone()
                binding.partnersRv.visible()
                showLoading()
                startFusedLocationListener()
            } else {
                showErrorMessage(getString(uz.alphazet.domain.R.string.please_turn_on_gps))
                binding.turnOnGPSContainer.visible()
                binding.partnersRv.gone()
            }

//            if (!isGPSEnabled && !isNetworkEnabled) {
//                showErrorMessage("please_turn_on_gps_or_internet")
//            } else {
//                if (!checkPermissionForLocation()) {
//                    showErrorMessage("location_permission_not_full_granted")
//                    return
//                }
//
//                startFusedLocationListener()
////                if (isGPSEnabled) {
////                    showErrorMessage("gps")
////                    startGPSLocationListener()
////                } else if (isNetworkEnabled) {
////                    showErrorMessage("network")
////                    startNetworkLocationListener()
////                } else {
////                    showErrorMessage("fused location")
////                    startFusedLocationListener()
////                }
//            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startNetworkLocationListener() {
        if (mLocationManager == null) mLocationManager =
            requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager

        mLocationManager!!.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES, this
        )
    }

    @SuppressLint("MissingPermission")
    private fun startGPSLocationListener() {
        if (mLocationManager == null) mLocationManager =
            requireActivity().getSystemService(LOCATION_SERVICE) as LocationManager

        mLocationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES, this
        )
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
        builder.addLocationRequest(locationRequest ?: LocationRequest())
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

    override fun onRefresh() {
        if (currentLocation != null)
            launch {
                viewModel.getNearShops(
                    currentLocation?.latitude ?: 0.0,
                    currentLocation?.longitude ?: 0.0,
                    null
                ).collectLatest(::collectNearShopsData)
            }
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
        const val TIMER_UPDATED = "TIMER_UPDATED"
        const val IS_SERVICE_ACTIVE = "IS_SERVICE_ACTIVE"
        const val TIME_EXTRA = "TIME_EXTRA"
        const val LOCALE_ID = "LOCALE_ID"
        var raidLocalID = -1
    }

}