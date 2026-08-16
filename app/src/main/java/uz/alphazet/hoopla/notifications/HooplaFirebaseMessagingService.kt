package uz.alphazet.hoopla.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.repositories.PushTokenRepo
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.log
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.ui.orders.OrderInfoScreen

/**
 * Receives FCM data/notification messages describing order status changes
 * (see server contract: data = {type: "order_status", orderId, status}, plus
 * a localized notification title/body) and mirrors token lifecycle into
 * [AppCache.fcmToken] + the `/v1/user/push-token` endpoint via [PushTokenRepo].
 */
class HooplaFirebaseMessagingService : FirebaseMessagingService() {

    private val appCache: AppCache by inject()
    private val pushTokenRepo: PushTokenRepo by inject()

    // Service has no lifecycleScope of its own; use a SupervisorJob-backed scope
    // scoped to the service instance and cancel it in onDestroy.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        token.log(TAG)

        appCache.fcmToken = token

        if (!appCache.accessToken.isNullOrEmpty()) {
            serviceScope.launch {
                runCatching { pushTokenRepo.registerPushToken(token) }
                    .onFailure { it.log(TAG) }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data[KEY_TYPE]
        // Ignore push types other than order-status pushes (future-proofing);
        // messages with no "type" at all (e.g. pure notification payloads) still show.
        if (type != null && type != TYPE_ORDER_STATUS) return

        val orderId = remoteMessage.data[KEY_ORDER_ID]?.toIntOrNull()
        val status = remoteMessage.data[KEY_STATUS]

        val title = remoteMessage.notification?.title
            ?: getString(uz.alphazet.domain.R.string.order_status_notification_default_title)
        val body = remoteMessage.notification?.body
            ?: status?.let {
                getString(uz.alphazet.domain.R.string.order_status_notification_default_body, it)
            }
            ?: getString(uz.alphazet.domain.R.string.order_status_notification_default_title)

        showOrderStatusNotification(title, body, orderId)
    }

    private fun showOrderStatusNotification(title: String, body: String, orderId: Int?) {
        createNotificationChannelIfNeeded()

        val intent = Intent(this, OrderInfoScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (orderId != null) putExtra(Constants.ID, orderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ORDER_STATUS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.logo_hoopla_new)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notificationId = orderId ?: System.currentTimeMillis().toInt()
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ORDER_STATUS_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            ORDER_STATUS_CHANNEL_ID,
            getString(uz.alphazet.domain.R.string.order_status_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "HooplaFCM"
        private const val KEY_TYPE = "type"
        private const val KEY_ORDER_ID = "orderId"
        private const val KEY_STATUS = "status"
        private const val TYPE_ORDER_STATUS = "order_status"
        const val ORDER_STATUS_CHANNEL_ID = "order_status_notification_channel"
    }

}
