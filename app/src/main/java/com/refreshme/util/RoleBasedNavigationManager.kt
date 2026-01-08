package com.refreshme.util

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.CustomerDashboardActivity
import com.refreshme.stylist.StylistDashboardActivity

/**
 * Manages role-based navigation and feature visibility
 * Ensures users only see features appropriate for their role
 */
object RoleBasedNavigationManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * User roles
     */
    enum class UserRole {
        CUSTOMER,
        STYLIST,
        UNKNOWN
    }

    /**
     * Get the current user's role from Firestore
     */
    fun getUserRole(callback: (UserRole) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(UserRole.UNKNOWN)
            return
        }

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")?.uppercase()
                callback(
                    when (role) {
                        "STYLIST" -> UserRole.STYLIST
                        "CUSTOMER" -> UserRole.CUSTOMER
                        else -> UserRole.UNKNOWN
                    }
                )
            }
            .addOnFailureListener {
                callback(UserRole.UNKNOWN)
            }
    }

    /**
     * Navigate to the appropriate dashboard based on user role
     */
    fun navigateToDashboard(context: Context, role: UserRole) {
        val intent = when (role) {
            UserRole.STYLIST -> Intent(context, StylistDashboardActivity::class.java)
            UserRole.CUSTOMER -> Intent(context, CustomerDashboardActivity::class.java)
            UserRole.UNKNOWN -> return
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    /**
     * Hide customer-only menu items for stylists
     */
    fun filterMenuForRole(menu: Menu, role: UserRole) {
        when (role) {
            UserRole.STYLIST -> {
                // Hide customer-only menu items
                // Example: menu.findItem(R.id.menu_find_stylist)?.isVisible = false
                // Example: menu.findItem(R.id.menu_browse_stylists)?.isVisible = false
            }
            UserRole.CUSTOMER -> {
                // Hide stylist-only menu items
                // Example: menu.findItem(R.id.menu_go_online)?.isVisible = false
                // Example: menu.findItem(R.id.menu_manage_services)?.isVisible = false
            }
            UserRole.UNKNOWN -> {
                // Hide all role-specific items
            }
        }
    }

    /**
     * Hide customer-only views for stylists
     */
    fun hideCustomerOnlyViews(vararg views: View) {
        views.forEach { it.visibility = View.GONE }
    }

    /**
     * Hide stylist-only views for customers
     */
    fun hideStylistOnlyViews(vararg views: View) {
        views.forEach { it.visibility = View.GONE }
    }

    /**
     * Check if user has permission to access a feature
     */
    fun hasPermission(userRole: UserRole, requiredRole: UserRole): Boolean {
        return userRole == requiredRole
    }

    /**
     * Features that are customer-only
     */
    object CustomerOnlyFeatures {
        const val FIND_STYLIST = "find_stylist"
        const val BROWSE_STYLISTS = "browse_stylists"
        const val FAVORITE_STYLISTS = "favorite_stylists"
        const val BOOK_APPOINTMENT = "book_appointment"
    }

    /**
     * Features that are stylist-only
     */
    object StylistOnlyFeatures {
        const val GO_ONLINE = "go_online"
        const val MANAGE_SERVICES = "manage_services"
        const val SET_AVAILABILITY = "set_availability"
        const val VIEW_EARNINGS = "view_earnings"
        const val MANAGE_BOOKINGS = "manage_bookings"
    }

    /**
     * Check if a feature is available for the user's role
     */
    fun isFeatureAvailable(feature: String, userRole: UserRole): Boolean {
        return when (userRole) {
            UserRole.STYLIST -> {
                feature in listOf(
                    StylistOnlyFeatures.GO_ONLINE,
                    StylistOnlyFeatures.MANAGE_SERVICES,
                    StylistOnlyFeatures.SET_AVAILABILITY,
                    StylistOnlyFeatures.VIEW_EARNINGS,
                    StylistOnlyFeatures.MANAGE_BOOKINGS
                )
            }
            UserRole.CUSTOMER -> {
                feature in listOf(
                    CustomerOnlyFeatures.FIND_STYLIST,
                    CustomerOnlyFeatures.BROWSE_STYLISTS,
                    CustomerOnlyFeatures.FAVORITE_STYLISTS,
                    CustomerOnlyFeatures.BOOK_APPOINTMENT
                )
            }
            UserRole.UNKNOWN -> false
        }
    }
}
