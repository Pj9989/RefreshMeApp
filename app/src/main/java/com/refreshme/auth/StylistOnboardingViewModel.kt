package com.refreshme.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StylistOnboardingViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun saveStylistData(
        fullName: String,
        businessName: String,
        onSuccess: () -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(userId)

        val userData = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "businessName" to businessName,
            "isVerified" to false
        )

        userDoc.update(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                // TODO: Handle failure to save data.
            }
    }
}