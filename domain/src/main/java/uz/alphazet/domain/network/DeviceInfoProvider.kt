package uz.alphazet.domain.network

import android.content.Context
import android.os.Build
import uz.alphazet.domain.utils.getSecureDeviceId

/**
 * Single source of truth for the device identity attached to every request
 * (X-* headers in [uz.alphazet.domain.network.interceptor.NetworkConnectionInterceptor])
 * and sent on `/auth/confirm-sms` so the backend can label the session in
 * `/user/devices`. Values are stable for the install lifetime, so they are
 * resolved once.
 */
class DeviceInfoProvider(context: Context) {

    /** e.g. "Samsung SM-G991B" */
    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    val platform: String = "android"

    /** Settings.Secure.ANDROID_ID */
    val deviceId: String = context.getSecureDeviceId().orEmpty()

    /** App versionName, e.g. "1.3.11" */
    val appVersion: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
}
