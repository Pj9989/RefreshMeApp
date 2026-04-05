package com.refreshme

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RefreshMeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase before any other services
        FirebaseApp.initializeApp(this)
        
        // Enable Firestore Offline Persistence
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
        
        // Initialize App Check (Debug for local, Play Integrity for release)
        AppCheckInitializer.initialize(this)

        // Initialize Crashlytics
        CrashlyticsInitializer.initialize(this)

        // Seeding is disabled because it violates Firestore security rules (Permission Denied)
        // when logged in as a normal user. Hardcoded stylist IDs cannot be written to.
        /*
        if (BuildConfig.DEBUG && FirebaseAuth.getInstance().currentUser != null) {
            DatabaseSeeder.seedData()
        }
        */
    }
}