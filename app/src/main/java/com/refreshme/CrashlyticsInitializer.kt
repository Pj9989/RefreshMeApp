package com.refreshme

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Initializes Firebase Crashlytics for crash reporting
 * 
 * Crashlytics helps you track, prioritize, and fix stability issues that erode app quality.
 */
object CrashlyticsInitializer {
    
    fun initialize(context: Context) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        // Enable Crashlytics collection
        crashlytics.setCrashlyticsCollectionEnabled(true)
        
        // Set custom keys for better debugging
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
    }
    
    /**
     * Set user identifier for crash reports
     */
    fun setUserId(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }
    
    /**
     * Log custom message to crash reports
     */
    fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }
    
    /**
     * Record non-fatal exception
     */
    fun recordException(exception: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(exception)
    }
    
    /**
     * Set custom key-value pair for crash reports
     */
    fun setCustomKey(key: String, value: String) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value)
    }
}