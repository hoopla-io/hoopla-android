package uz.alphazet.hoopla.ui.cart

import android.os.Bundle
import androidx.fragment.app.commit
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_NAME

/**
 * Hosts [CartScreen] as a pushed screen, for opening the cart from a shop's menu.
 *
 * The cart also lives as a bottom-navigation tab, but the nav bar is not on screen here — from a
 * shop the customer expects to look at their order and come straight back to the menu, which is
 * a push, not a tab switch. Both routes run the same fragment; only the chrome differs.
 */
class CartActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(
                    R.id.cart_container,
                    CartScreen.newInstance(
                        intent.getIntExtra(SHOP_ID, -1),
                        intent.getStringExtra(SHOP_NAME)
                    )
                )
            }
        }
    }

}
