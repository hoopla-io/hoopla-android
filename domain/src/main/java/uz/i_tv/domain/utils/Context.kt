package uz.i_tv.domain.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment

fun Fragment.intentToCall(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
    })
}

fun Fragment.intentToBrowser(link: String) {
    if (Patterns.WEB_URL.matcher(link).matches()) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }
}

fun Activity.intentToCall(phone: String) {
    startActivity(Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phone")
    })
}

@SuppressLint("HardwareIds")
fun Context.getSecureDeviceId(): String =
    Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)


inline fun <reified T> T.log(tag: String = "QAHVAZOR") {
    Log.d(tag, "${(T::class.java).simpleName} ${this.toString()}")
//    Timber.tag(tag).d("${(T::class.java).simpleName} ${this.toString()}")
}
