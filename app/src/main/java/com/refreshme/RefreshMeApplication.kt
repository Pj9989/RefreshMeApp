package com.refreshme

import android.app.Application
import com.google.firebase.FirebaseApp
// Temporarily commented out App Check to fix 403 error
// import com.google.firebase.appcheck.FirebaseAppCheck
// import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
// import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class RefreshMeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Temporarily disabled App Check to fix 403 error during testing
        // TODO: Re-enable App Check after registering debug token in Firebase Console
        /*
        // Initialize Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // Use debug provider for debug builds, Play Integrity for release
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        */
        
        if (BuildConfig.DEBUG) {
            DatabaseSeeder.seedData()
        }
    }
}
