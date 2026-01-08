package com.refreshme.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RoleSelectViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun onRoleSelected(role: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(userId)

        userDoc.set(mapOf("role" to role))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                // TODO: Handle failure to save role.
            }
    }
}