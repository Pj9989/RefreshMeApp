package com.refreshme.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserManager {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var userRole: String? = null

    suspend fun getUserRole(): String? {
        if (userRole == null) {
            val userId = auth.currentUser?.uid ?: return null
            val document = firestore.collection("users").document(userId).get().await()
            userRole = document.getString("role")
        }
        return userRole
    }

    fun clear() {
        userRole = null
    }
}