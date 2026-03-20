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

    /**
     * Returns only stylists who are BOTH currently online AND identity-verified.
     * This is the core query powering "Find a Stylist" — the whole point of RefreshMe
     * is showing who is available *right now*.
     *
     * Firestore requires a composite index on (online ASC, verified ASC).
     * Run: firebase deploy --only firestore:indexes  (index defined in firestore.indexes.json)
     */
    suspend fun getStylists(): List<Stylist> {
        return try {
            val documents = firestore.collection("stylists")
                .whereEqualTo("online", true)
                .whereEqualTo("verified", true)
                .get()
                .await()
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
