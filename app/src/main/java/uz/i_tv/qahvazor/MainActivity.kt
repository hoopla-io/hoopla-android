package uz.i_tv.qahvazor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.qahvazor.databinding.ActivityMainBinding
import uz.i_tv.qahvazor.ui.home.HomeScreen
import uz.i_tv.qahvazor.ui.profile.ProfileScreen
import uz.i_tv.qahvazor.ui.qr_code.QRCodeScreen

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnNavigationItemSelectedListener(navListener)
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, HomeScreen())
            .commit()
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        // By using switch we can easily get the
        // selected fragment by using there id
        lateinit var selectedFragment: BaseFragment
        when (item.itemId) {
            R.id.home -> {
                selectedFragment = HomeScreen()
            }

            R.id.qr_code -> {
                selectedFragment = QRCodeScreen()
            }

            R.id.profile -> {
                selectedFragment = ProfileScreen()
            }
        }
        // It will help to replace the
        // one fragment to other.
        supportFragmentManager.beginTransaction().replace(R.id.main_container, selectedFragment)
            .commit()
        true
    }
}