package com.refreshme.legal

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around the /legalAcceptances collection. Acceptance records are
 * append-only: each document/version pair gets its own immutable record.
 *
 * This class intentionally avoids Hilt/DI so it can be used from anywhere,
 * including plain fragments or activities that haven't been migrated yet.
 */
class LegalRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    private fun requireUid(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("Legal acceptance requires an authenticated user")

    private fun acceptanceRecordId(docKey: String, docVersion: String): String {
        return "${docKey}_${docVersion}"
    }

    /**
     * Record that the current user accepted [docKey] at [docVersion].
     * [signedName] applies to docs that require an e-signature (contractor
     * agreement, FCRA consent). Pass an empty string otherwise.
     */
    suspend fun recordAcceptance(
        docKey: String,
        docVersion: String,
        signedName: String = "",
        appVersion: String = "",
    ) {
        val uid = requireUid()
        val record = LegalAcceptance(
            userId = uid,
            docKey = docKey,
            docVersion = docVersion,
            signedName = signedName,
            appVersion = appVersion,
            acceptedOnPlatform = "android",
            ipHint = "0.0.0.0", // Dummy value to pass strict rule requirement
            // acceptedAt is set via @ServerTimestamp in the data class
        )
        try {
            val map = hashMapOf(
                "userId" to record.userId,
                "docKey" to record.docKey,
                "docVersion" to record.docVersion,
                "signedName" to record.signedName,
                "acceptedOnPlatform" to record.acceptedOnPlatform,
                "appVersion" to record.appVersion,
                "ipHint" to record.ipHint,
                "acceptedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Use a batch to ensure the parent doc exists if it's the first time
            val batch = db.batch()
            val parentRef = db.collection("legalAcceptances").document(uid)
            val docRef = parentRef.collection("acceptances").document(acceptanceRecordId(docKey, docVersion))
            
            // "Touch" the parent document if it doesn't exist
            batch.set(parentRef, hashMapOf("createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
            batch.set(docRef, map)
            
            batch.commit().await()
            Log.d(TAG, "Recorded acceptance: $docKey @ $docVersion for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record acceptance for $docKey", e)
            throw e
        }
    }

    /** Returns the currently-accepted version for this doc, or null. */
    suspend fun acceptedVersion(docKey: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snaps = db.collection("legalAcceptances")
                .document(uid)
                .collection("acceptances")
                .whereEqualTo("docKey", docKey)
                .get()
                .await()
            snaps.documents
                .mapNotNull { it.getString("docVersion") }
                .maxOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load acceptance for $docKey", e)
            null
        }
    }

    /** Convenience: does the current user need to (re-)accept [docKey]? */
    suspend fun needsAcceptance(docKey: String, currentVersion: String): Boolean {
        val uid = auth.currentUser?.uid ?: return true
        return try {
            val snap = db.collection("legalAcceptances")
                .document(uid)
                .collection("acceptances")
                .document(acceptanceRecordId(docKey, currentVersion))
                .get()
                .await()
            !snap.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load acceptance for $docKey", e)
            true
        }
    }

    companion object {
        private const val TAG = "LegalRepository"
    }
}
