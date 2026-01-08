package com.refreshme.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.refreshme.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val stylistName = inputData.getString(KEY_STYLIST_NAME) ?: return@withContext Result.failure()
        val appointmentTime = inputData.getString(KEY_APPOINTMENT_TIME) ?: return@withContext Result.failure()

        showNotification(stylistName, appointmentTime)

        return@withContext Result.success()
    }

    private fun showNotification(stylistName: String, appointmentTime: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Upcoming Appointment")
            .setContentText("You have an appointment with $stylistName at $appointmentTime.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val KEY_STYLIST_NAME = "stylist_name"
        const val KEY_APPOINTMENT_TIME = "appointment_time"
        const val CHANNEL_ID = "appointment_reminders"
        const val NOTIFICATION_ID = 1
    }
}