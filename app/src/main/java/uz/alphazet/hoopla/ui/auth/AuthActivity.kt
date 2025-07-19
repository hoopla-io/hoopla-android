package uz.alphazet.hoopla.ui.auth

import android.os.Bundle
import androidx.fragment.app.commit
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ActivityAuthBinding

class AuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding

    private val currentFragment: BaseFragment?
        get() = supportFragmentManager.fragments.firstOrNull { it.isVisible } as? BaseFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = AuthScreen()
        supportFragmentManager.commit { replace(R.id.authContainer, fragment) }
        supportFragmentManager.executePendingTransactions()

    }

    fun navigateTo(fragment: BaseFragment) {
        val newFragment = supportFragmentManager.findFragmentByTag(fragment.tag)
        if (currentFragment != null && newFragment != null && newFragment === currentFragment) return

        supportFragmentManager.beginTransaction().apply {
            if (newFragment == null) {
                add(
                    R.id.authContainer,
                    fragment,
                    fragment.tag
                )
            }
            currentFragment?.let { hide(it) }
            if (newFragment != null) show(newFragment)
        }.commit()
    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

}