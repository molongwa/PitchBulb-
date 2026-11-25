package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PitchPulseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token received")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "PitchBulb Match Update"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Check live scores and match stats."
        NotificationHelper.showMatchAlertNotification(
            context = applicationContext,
            notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title = title,
            body = body
        )
    }

    private companion object {
        const val TAG = "PitchPulseFCM"
    }
}
