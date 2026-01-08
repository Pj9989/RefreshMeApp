package com.refreshme.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StylistRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getStylist(stylistId: String): Stylist? {
        return try {
            val document = firestore.collection("stylists").document(stylistId).get().await()
            if (document != null && document.exists()) {
                document.toObject(Stylist::class.java)?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStylists(): List<Stylist> {
        return try {
            val documents = firestore.collection("stylists").whereEqualTo("isVerified", true).get().await()
            if (documents.isEmpty) {
                emptyList()
            } else {
                documents.map { document ->
                    document.toObject(Stylist::class.java).copy(id = document.id)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}