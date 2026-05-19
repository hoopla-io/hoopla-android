package uz.alphazet.hoopla.ui.search

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.R
import uz.alphazet.domain.utils.showKeyboard
import uz.alphazet.hoopla.databinding.ScreenSearchBinding
import uz.alphazet.hoopla.ui.home.HomeVM
import uz.alphazet.hoopla.ui.home.NearShopAdapter
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DISTANCE
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID

class SearchScreen : BaseActivity() {

    private lateinit var binding: ScreenSearchBinding
    private val viewModel: HomeVM by viewModel()

    private val adapter = NearShopAdapter(false)

    private val defaultPoint: LatLng = LatLng(41.31125776157484, 69.27957810360282)

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
            finish()
        }

        binding.root.post {
            binding.inputSearch.requestFocus()
            showKeyboard()
        }

        binding.itemsRv.adapter = adapter

        adapter.setOnItemClickListener {
            val intent = Intent(this, ShopDetailActivity::class.java)
            intent.putExtra(SHOP_ID, it.shopId)
            intent.putExtra(DISTANCE, it.distance)
            shopListener.launch(intent)
        }

        binding.inputSearch.doAfterTextChanged { text ->
            if (!text.isNullOrEmpty()) {
                launch {
                    viewModel.getNearShops(
                        defaultPoint.latitude,
                        defaultPoint.longitude,
                        text.toString()
                    ).collectLatest(::collectNearShopsData)
                }
            } else {
                getNearShops(defaultPoint)
            }
        }

    }

    private fun collectNearShopsData(t: UIResource<List<ShopItemData>>) = t.collect {
        adapter.submitList(it)
    }

    private fun getNearShops(location: LatLng) {
        launch {
            viewModel.getNearShops(location.latitude, location.longitude, null)
                .collectLatest(::collectNearShopsData)
        }
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
}