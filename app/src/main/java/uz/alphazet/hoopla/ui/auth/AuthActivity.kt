package uz.alphazet.hoopla.ui.auth

import android.os.Bundle
import androidx.fragment.app.commit
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ActivityAuthBinding

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