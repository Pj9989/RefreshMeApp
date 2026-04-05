package com.refreshme.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.refreshme.Role
import com.refreshme.StyleProfile
import com.refreshme.User
import kotlinx.coroutines.tasks.await

object UserManager {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var userRole: String? = null
    private var currentUser: User? = null

    suspend fun getCurrentUser(forceRefresh: Boolean = false): User? {
        val userId = auth.currentUser?.uid ?: return null
        
        if (currentUser == null || currentUser?.uid != userId || forceRefresh) {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)?.copy(uid = userId)
                } else {
                    currentUser = null
                }
            } catch (e: Exception) {
                Log.e("UserManager", "Error fetching user", e)
                currentUser = null
            }
        }
        return currentUser
    }

    suspend fun updateStyleProfile(profile: StyleProfile): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return try {
            // Use set with merge instead of update for robustness
            val data = mapOf("styleProfile" to profile)
            firestore.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .await()
            // Invalidate cache
            currentUser = null
            true
        } catch (e: Exception) {
            Log.e("UserManager", "Error updating style profile", e)
            false
        }
    }

    suspend fun getUserRole(forceRefresh: Boolean = false): String? {
        if (userRole == null || forceRefresh) {
            userRole = getCurrentUser(forceRefresh)?.role?.name
        }
        return userRole
    }

    fun clear() {
        userRole = null
        currentUser = null
    }
}