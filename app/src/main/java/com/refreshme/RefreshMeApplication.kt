package com.refreshme

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RefreshMeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Workaround for emulator issue with TRuntime.CctTransportBackend
        // dropping connections (Software caused connection abort)
        System.setProperty("http.keepAlive", "false")
        
        // Initialize Firebase before any other services
        FirebaseApp.initializeApp(this)
        
        // App Check initialization
        if (!BuildConfig.DEBUG && !BuildConfig.IS_INTERNAL_TESTING) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        } else {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
        }
        
        // Enable Firestore Offline Persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        // Initialize Crashlytics
        CrashlyticsInitializer.initialize(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channels = listOf(
                NotificationChannel(
                    "urgent_channel",
                    "Urgent Appointments",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Used for new booking requests and urgent updates"
                },
                NotificationChannel(
                    "chat_channel",
                    "Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "New chat messages"
                },
                NotificationChannel(
                    "default_channel",
                    "General Updates",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "General notifications and news"
                },
                NotificationChannel(
                    "smart_rebook_reminders",
                    "Rebooking Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Smart reminders for when it's time for another haircut"
                }
            )
            notificationManager.createNotificationChannels(channels)
        }
    }
}