package uz.alphazet.domain.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

fun Context.intentToBrowser(link: String) {
    if (Patterns.WEB_URL.matcher(link).matches()) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }
}

fun Context.intentToCall(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
    })
}

@SuppressLint("HardwareIds")
fun Context.getSecureDeviceId(): String =
    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

fun Context.showKeyboard() {
    val inputMethodManager =
        this.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.toggleSoftInput(
        InputMethodManager.SHOW_FORCED,
        InputMethodManager.HIDE_IMPLICIT_ONLY
    )
}

fun Fragment.hideKeyboard() {
    val manager: InputMethodManager =
        requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (view != null)
        manager.hideSoftInputFromWindow(view?.windowToken, 0)
}

fun BottomSheetDialogFragment.hideKeyboard() {
    val manager: InputMethodManager =
        requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    if (view != null)
        manager.hideSoftInputFromWindow(view?.windowToken, 0)
}


inline fun <reified T> T.log(tag: String = "QAHVAZOR") {
    Log.d(tag, "${(T::class.java).simpleName} ${this.toString()}")
//    Timber.tag(tag).d("${(T::class.java).simpleName} ${this.toString()}")
}
