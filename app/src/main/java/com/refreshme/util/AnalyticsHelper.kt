package com.refreshme.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {

    private var analytics: FirebaseAnalytics? = null

    fun initialize(firebaseAnalytics: FirebaseAnalytics) {
        analytics = firebaseAnalytics
    }

    fun logScreenView(screenName: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logViewStylistProfile(stylistId: String) {
        val bundle = Bundle()
        bundle.putString("stylist_id", stylistId)
        analytics?.logEvent("view_stylist_profile", bundle)
    }

    fun logSearchStylists(searchTerm: String) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, searchTerm)
        analytics?.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
    }

    fun logFilterApplied(filterType: String) {
        val bundle = Bundle()
        bundle.putString("filter_type", filterType)
        analytics?.logEvent("filter_applied", bundle)
    }

    fun logFavoriteAdded(stylistId: String) {
        val bundle = Bundle()
        bundle.putString("stylist_id", stylistId)
        analytics?.logEvent("favorite_added", bundle)
    }

    fun logFavoriteRemoved(stylistId: String) {
        val bundle = Bundle()
        bundle.putString("stylist_id", stylistId)
        analytics?.logEvent("favorite_removed", bundle)
    }

    fun setUserProperties(userType: String, favoriteSpecialty: String?) {
        analytics?.setUserProperty("user_type", userType)
        favoriteSpecialty?.let {
            analytics?.setUserProperty("favorite_specialty", it)
        }
    }
}
