package com.refreshme.identity

/**
 * Represents the possible states of a user's Stripe Identity verification.
 *
 * These values mirror the `verificationStatus` field stored in Firestore
 * (written by the Firebase Function webhook) and the `verified` boolean flag.
 */
enum class VerificationStatus {
    /** The user has not yet started or requested verification. */
    NOT_STARTED,

    /** A verification session has been created and is awaiting the user to complete it. */
    PENDING,

    /** Stripe has confirmed the user's identity documents are valid. */
    VERIFIED,

    /** The verification attempt failed (e.g., blurry document, mismatch). */
    FAILED,

    /** The user canceled the verification flow before completing it. */
    CANCELED,

    /** The verification session has expired and a new one must be created. */
    EXPIRED;

    companion object {
        /**
         * Converts the raw string value stored in Firestore to a [VerificationStatus].
         * Defaults to [NOT_STARTED] for any unknown or null value.
         */
        fun fromFirestore(value: String?): VerificationStatus {
            return when (value?.lowercase()) {
                "pending"  -> PENDING
                "verified" -> VERIFIED
                "failed"   -> FAILED
                "canceled" -> CANCELED
                "expired"  -> EXPIRED
                else       -> NOT_STARTED
            }
        }
    }
}
