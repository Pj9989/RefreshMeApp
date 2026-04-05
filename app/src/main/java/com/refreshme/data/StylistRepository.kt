package com.refreshme.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StylistRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getStylist(stylistId: String): Result<Stylist?> {
        return try {
            val document = firestore.collection("stylists").document(stylistId).get().await()
            if (document != null && document.exists()) {
                val stylist = try {
                    document.toObject(Stylist::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    Log.e("StylistRepository", "Error parsing stylist $stylistId", e)
                    null
                }
                Result.success(stylist)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVerifiedStylists(): Result<List<Stylist>> {
        return try {
            val snapshot = firestore.collection("stylists")
                .whereEqualTo("verified", true)
                .get()
                .await()
            
            val stylists = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Stylist::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("StylistRepository", "Skipping stylist ${doc.id} due to parsing error", e)
                    null
                }
            }
            Result.success(stylists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}