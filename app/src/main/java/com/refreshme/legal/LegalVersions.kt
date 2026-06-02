package com.refreshme.legal

/**
 * Central registry of every legal document the app enforces, and the version
 * string the user must have accepted. Bumping a version forces the user to
 * re-accept on next app launch (see LegalAcceptanceViewModel).
 *
 * Date-style versions (yyyy-MM-dd) make audits easy.
 */
object LegalVersions {
    const val CUSTOMER_TOS = "2026-04-22"
    const val PRIVACY_POLICY = "2026-04-22"
    const val STYLIST_CONTRACTOR_AGREEMENT = "2026-04-22"
    const val FCRA_BACKGROUND_CHECK_CONSENT = "2026-04-22"
    const val AT_HOME_LIABILITY_WAIVER = "2026-04-22"
    const val ALLERGY_DISCLOSURE = "2026-04-22"
}

/**
 * Firestore doc-type keys used inside /legalAcceptances/{uid}/acceptances/{docKey}.
 * Keep in sync with Cloud Functions if you add server-side checks.
 */
object LegalDocKeys {
    const val CUSTOMER_TOS = "customer_tos"
    const val PRIVACY_POLICY = "privacy_policy"
    const val STYLIST_CONTRACTOR_AGREEMENT = "stylist_contractor_agreement"
    const val FCRA_BACKGROUND_CHECK_CONSENT = "fcra_background_check_consent"
    const val AT_HOME_LIABILITY_WAIVER = "at_home_liability_waiver"
    const val ALLERGY_DISCLOSURE = "allergy_disclosure"
}

/**
 * Public URLs for each long-form legal doc hosted on the RefreshMe website.
 * These are linked from the click-wrap screens so the user can read the full
 * text in a browser instead of reading it inline.
 */
object LegalUrls {
    private const val BASE = "https://refreshme-74f79.web.app"
    const val TERMS = "$BASE/terms.html"
    const val PRIVACY = "$BASE/privacy.html"
    const val SAFETY = "$BASE/safety-center.html"
    const val ABOUT = "$BASE/about.html"
    // Update these when you upload the contractor / FCRA / waiver pages.
    const val CONTRACTOR_AGREEMENT = "$BASE/stylist-agreement.html"
    const val FCRA_SUMMARY_OF_RIGHTS = "$BASE/fcra-summary.html"
    const val AT_HOME_WAIVER = "$BASE/at-home-waiver.html"
}
