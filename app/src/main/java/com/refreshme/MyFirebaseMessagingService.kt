package com.refreshme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.refreshme.stylist.StylistDashboardActivity
import java.net.HttpURLConnection
import java.net.URL

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val title = data["title"] ?: remoteMessage.notification?.title
            val body = data["body"] ?: remoteMessage.notification?.body
            val type = data["type"] 
            val targetId = data["targetId"] 
            val imageUrl = data["imageUrl"]

            sendNotification(title, body, type, targetId, imageUrl)
        } else {
            remoteMessage.notification?.let {
                sendNotification(it.title, it.body, null, null, null)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE).edit {
            putString("fcm_token", token)
        }
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && token != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).update("fcmToken", token)
                .addOnFailureListener { Log.e(TAG, "Failed to update token in users collection") }
            db.collection("stylists").document(user.uid).update("fcmToken", token)
                .addOnFailureListener { Log.e(TAG, "Failed to update token in stylists collection") }
        }
    }

    private fun sendNotification(title: String?, messageBody: String?, type: String?, targetId: String?, imageUrl: String?) {
        val isStylistNotif = type == "booking_request" || type == "new_booking" || type == "payout_ready"
        val targetActivity = if (isStylistNotif) StylistDashboardActivity::class.java else MainActivity::class.java

        val intent = Intent(this, targetActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
            putExtra("target_id", targetId)
            if (type == "chat") {
                putExtra("chat_id", targetId)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (type) {
            "chat" -> CHANNEL_CHAT
            "booking_request", "asap_request" -> CHANNEL_URGENT
            else -> CHANNEL_DEFAULT
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title ?: "RefreshMe")
            .setContentText(messageBody ?: "New update available")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#D4AF37"))
            .setCategory(if (type == "chat") NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_EVENT)
            .setPriority(if (channelId == CHANNEL_URGENT) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)

        imageUrl?.let { url ->
            val bitmap = getBitmapFromUrl(url)
            if (bitmap != null) {
                notificationBuilder.setLargeIcon(bitmap)
                if (type != "chat") {
                    notificationBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                }
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(CHANNEL_URGENT, "Urgent Appointments", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_CHAT, "Messages", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_DEFAULT, "General Updates", NotificationManager.IMPORTANCE_LOW)
            )
            notificationManager.createNotificationChannels(channels)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun getBitmapFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            BitmapFactory.decodeStream(connection.inputStream)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val CHANNEL_URGENT = "urgent_channel"
        const val CHANNEL_CHAT = "chat_channel"
        const val CHANNEL_DEFAULT = "default_channel"
    }
}
