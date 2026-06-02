package com.refreshme.legal

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * One acceptance record. Written to:
 *   /legalAcceptances/{uid}/acceptances/{docKey}
 *
 * Also mirrored on per-booking documents when the acceptance is per-booking
 * (e.g. the at-home waiver): bookings/{bookingId}.waiverAccepted = { ... }
 *
 * KEEP fields flat and non-nullable where possible so Firestore rules can
 * validate them cleanly.
 */
data class LegalAcceptance(
    val userId: String = "",
    val docKey: String = "",
    val docVersion: String = "",
    val signedName: String = "",
    val acceptedOnPlatform: String = "android",
    val appVersion: String = "",
    val ipHint: String = "", // optional, best-effort from client — server can overwrite
    @ServerTimestamp
    val acceptedAt: Date? = null,
)

/**
 * Compact summary of a user's acceptance for a single doc. Safe to pass
 * around the UI layer without leaking extra fields.
 */
data class AcceptanceStatus(
    val docKey: String,
    val acceptedVersion: String?,
    val currentVersion: String,
) {
    val isCurrent: Boolean
        get() = acceptedVersion == currentVersion
    val needsAcceptance: Boolean
        get() = acceptedVersion == null || acceptedVersion != currentVersion
}
