package uz.alphazet.domain.utils

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Callback-to-suspend wrapper around [FirebaseMessaging.getToken]. The
 * `kotlinx-coroutines-play-services` artifact (which would provide `Task.await()`)
 * is not in the version catalog, so this bridges the Play Services callback API
 * to coroutines manually.
 */
suspend fun FirebaseMessaging.awaitToken(): String =
    suspendCancellableCoroutine { continuation ->
        token
            .addOnSuccessListener { token ->
                if (continuation.isActive) continuation.resume(token)
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) continuation.resumeWithException(e)
            }
    }
