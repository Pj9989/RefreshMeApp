package com.refreshme.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

object SubscriptionManager {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    suspend fun isSubscriptionActive(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        val document = firestore.collection("users").document(userId).get().await()

        val status = document.getString("subscriptionStatus")
        val endDate = document.getDate("subscriptionEndDate")

        return if (status != null && endDate != null) {
            status in listOf("trial", "active") && endDate.after(Date())
        } else {
            false
        }
    }
}