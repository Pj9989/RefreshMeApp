package com.refreshme

import android.app.Application

class RefreshMeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            DatabaseSeeder.seedData()
        }
    }
}