package uz.alphazet.hoopla.ui.profile.settings

import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import uz.alphazet.domain.theme.ThemeModes
import uz.alphazet.domain.theme.toNightMode
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogSelectThemeBinding

class SelectThemeBD(private val onThemeSelected: (String) -> Unit = {}) :
    BaseBottomSheetDF(R.layout.dialog_select_theme) {

    private val binding by viewBinding(DialogSelectThemeBinding::bind)

    override fun initialize() {
        binding.themeLight.isChecked = true

        binding.themeSystem.setOnClickListener(this)
        binding.themeLight.setOnClickListener(this)
        binding.themeDark.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val mode = when (view.id) {
            R.id.theme_system -> ThemeModes.SYSTEM
            R.id.theme_light -> ThemeModes.LIGHT
            R.id.theme_dark -> ThemeModes.DARK
            else -> return
        }
        if (mode != cache.themeMode) {
            cache.themeMode = mode
            AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
            onThemeSelected(mode)
        }
        dismiss()
    }

    companion object {
        private const val TAG = "SelectThemeBD"

        fun BaseFragment.showSelectThemeBD(
            onThemeSelected: (String) -> Unit = {}
        ) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                SelectThemeBD(onThemeSelected).show(
                    childFragmentManager,
                    TAG
                )
            }
        }
    }
}