package com.refreshme.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CustomerOnboardingViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun saveCustomerData(name: String, email: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(userId)

        val userData = mutableMapOf<String, Any>(
            "email" to email
        )
        if (name.isNotBlank()) {
            userData["name"] = name
        }

        userDoc.update(userData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                // TODO: Handle failure to save data.
            }
    }
}