package uz.i_tv.qahvazor.ui.auth

import android.os.Bundle
import androidx.fragment.app.commit
import uz.i_tv.domain.ui.BaseActivity
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ActivityAuthBinding

class AuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fragment = AuthScreen()
        supportFragmentManager.commit { replace(R.id.authContainer, fragment) }
        supportFragmentManager.executePendingTransactions()

    }

}