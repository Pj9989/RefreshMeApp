package com.refreshme.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.refreshme.MainActivity
import com.refreshme.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartRebookWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val stylistName = inputData.getString(KEY_STYLIST_NAME) ?: "your stylist"
        val stylistId = inputData.getString(KEY_STYLIST_ID)
        val serviceName = inputData.getString(KEY_SERVICE_NAME) ?: "haircut"

        showNotification(stylistName, stylistId, serviceName)

        return@withContext Result.success()
    }

    private fun showNotification(stylistName: String, stylistId: String?, serviceName: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rebooking Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart reminders for when it's time for another haircut"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link to the app (can be handled in MainActivity to jump to stylist profile)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("stylistId", stylistId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Make the message smart based on the service name
        val serviceLower = serviceName.lowercase()
        val title: String
        val message: String
        
        when {
            serviceLower.contains("fade") || serviceLower.contains("taper") -> {
                title = "Time for a clean-up!"
                message = "That fade is growing out. Tap here to book another session with $stylistName."
            }
            serviceLower.contains("line") || serviceLower.contains("edge") || serviceLower.contains("beard") -> {
                title = "Keep it sharp 🔪"
                message = "Time to get that line up fresh again with $stylistName."
            }
            serviceLower.contains("color") || serviceLower.contains("dye") -> {
                title = "Roots showing?"
                message = "It's been a while since your color appointment. See $stylistName to touch it up!"
            }
            else -> {
                title = "Time for a Refresh ✂️"
                message = "It's been a few weeks! Ready to get a fresh $serviceName from $stylistName?"
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Make sure you have this icon or replace it with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val KEY_STYLIST_NAME = "stylist_name"
        const val KEY_STYLIST_ID = "stylist_id"
        const val KEY_SERVICE_NAME = "service_name"
        const val CHANNEL_ID = "smart_rebook_reminders"
    }
}
