package uz.alphazet.hoopla.ui.profile.settings

import android.os.Build
import android.view.View
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogSelectLanguageBinding
import java.util.Locale

class SelectLanguageBD(private val onLocaleSelected: (Locale) -> Unit = {}) :
    BaseBottomSheetDF(R.layout.dialog_select_language) {

    private val binding by viewBinding(DialogSelectLanguageBinding::bind)

    override fun initialize() {
        val currentLocale = getCurrentLocale()
        when (currentLocale.language) {
            "uz" -> binding.uzbek.isChecked = true
            "ru" -> binding.russian.isChecked = true
            "en" -> binding.english.isChecked = true
        }

        binding.uzbek.setOnClickListener(this)
        binding.english.setOnClickListener(this)
        binding.russian.setOnClickListener(this)

    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.uzbek -> {
                cache.lang = "uz"
                onLocaleSelected(Locale("uz"))
                dismiss()
            }

            R.id.english -> {
                cache.lang = "en"
                onLocaleSelected(Locale("en"))
                dismiss()
            }

            R.id.russian -> {
                cache.lang = "ru"
                onLocaleSelected(Locale("ru"))
                dismiss()
            }
        }
    }

    private fun getCurrentLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requireContext().resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            requireContext().resources.configuration.locale
        }
    }


    companion object {
        private const val TAG = "SelectLanguageBD"

        fun BaseFragment.showSelectLanguageBD(
            onLocaleSelected: (Locale) -> Unit = {}
        ) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                SelectLanguageBD(onLocaleSelected).show(
                    childFragmentManager,
                    TAG
                )
            }
        }
    }

}