package com.refreshme.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FixFirestoreData {
    
    private const val TAG = "FixFirestoreData"
    
    suspend fun fixPortfolioImagesFormat() {
        val firestore = FirebaseFirestore.getInstance()
        val stylistsCollection = firestore.collection("stylists")
        
        Log.d(TAG, "Starting Firestore cleanup...")
        
        try {
            val snapshot = stylistsCollection.get().await()
            var fixedCount = 0
            var skippedCount = 0
            
            for (document in snapshot.documents) {
                try {
                    val portfolioImages = document.get("portfolioImages")
                    
                    if (portfolioImages is Map<*, *>) {
                        val imagesList = portfolioImages.values.filterIsInstance<String>()
                        document.reference.update("portfolioImages", imagesList).await()
                        fixedCount++
                        Log.d(TAG, "Fixed document: ${document.id}")
                    } else {
                        skippedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fixing document ${document.id}", e)
                }
            }
            
            Log.d(TAG, "Cleanup complete! Fixed: $fixedCount, Skipped: $skippedCount")
            
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during cleanup", e)
        }
    }
}
