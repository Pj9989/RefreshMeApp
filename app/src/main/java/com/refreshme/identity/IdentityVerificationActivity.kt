package com.refreshme.identity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.R
import com.stripe.android.identity.IdentityVerificationSheet
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity that manages the full Stripe Identity verification flow.
 *
 * Production Flow:
 * 1. Load current verification status from Firestore (one-time read on open).
 * 2. If not yet verified, call the `createIdentityVerificationSession` Firebase Function
 *    to obtain a Stripe ephemeral key and session ID.
 * 3. Launch the [IdentityVerificationSheet] with those credentials.
 * 4. On [IdentityVerificationSheet.VerificationFlowResult.Completed], show a "Pending" state
 *    and start a **real-time Firestore listener** on `users/{uid}`.
 * 5. When the Stripe webhook fires (`identity.verification_session.verified`) and the
 *    Firebase Cloud Function writes `verificationStatus = "VERIFIED"` to Firestore,
 *    the listener automatically advances the UI to the "Verified ✓" state — no manual
 *    polling or bypass required.
 *
 * Security: The `verified` flag is ONLY ever written by the server-side Firebase Function
 * that processes the authenticated Stripe webhook. The Android app never writes this field.
 */
class IdentityVerificationActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }

    private lateinit var statusTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var verifyButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var identityVerificationSheet: IdentityVerificationSheet

    /** Holds the real-time Firestore listener so it can be removed on destroy. */
    private var verificationListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_verification)

        statusTextView = findViewById(R.id.verificationStatusText)
        descriptionTextView = findViewById(R.id.verificationDescriptionText)
        verifyButton = findViewById(R.id.startVerificationButton)
        progressBar = findViewById(R.id.verificationProgressBar)

        // Register the Stripe Identity result callback
        val logoUri = Uri.parse("android.resource://$packageName/${R.drawable.ic_launcher_foreground}")
        identityVerificationSheet = IdentityVerificationSheet.create(
            this,
            IdentityVerificationSheet.Configuration(
                brandLogo = logoUri
            ),
            ::onVerificationSheetResult
        )

        verifyButton.setOnClickListener {
            startVerificationSession()
        }

        loadVerificationStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Always clean up the real-time listener to avoid memory leaks.
        verificationListener?.remove()
        verificationListener = null
    }

    // -------------------------------------------------------------------------
    // Load current status from Firestore (one-time read on Activity open)
    // -------------------------------------------------------------------------

    private fun loadVerificationStatus() {
        val uid = auth.currentUser?.uid ?: return
        showLoading(true)

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                showLoading(false)
                val rawStatus = doc.getString("verificationStatus")
                val status = VerificationStatus.fromFirestore(rawStatus)
                updateUiForStatus(status)

                // If already pending (user submitted but webhook hasn't fired yet),
                // immediately attach the real-time listener so the UI auto-updates.
                if (status == VerificationStatus.PENDING) {
                    attachVerificationListener(uid)
                }
            }
            .addOnFailureListener {
                showLoading(false)
                updateUiForStatus(VerificationStatus.NOT_STARTED)
            }
    }

    // -------------------------------------------------------------------------
    // Call Firebase Function to create a verification session
    // -------------------------------------------------------------------------

    private fun startVerificationSession() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "You must be signed in to verify your identity.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        verifyButton.isEnabled = false

        lifecycleScope.launch {
            try {
                // Force a token refresh so the callable function always receives a valid Firebase Auth
                // token. Stale or missing tokens are the most common cause of UNAUTHENTICATED errors.
                user.getIdToken(true).await() // forceRefresh = true

                val result = functions
                    .getHttpsCallable("createIdentityVerificationSession")
                    .call()
                    .await()

                val data = result.data as? Map<*, *>
                val ephemeralKeySecret = data?.get("client_secret") as? String
                val verificationSessionId = data?.get("id") as? String

                if (ephemeralKeySecret != null && verificationSessionId != null) {
                    showLoading(false)
                    // Launch the Stripe Identity SDK sheet
                    identityVerificationSheet.present(
                        verificationSessionId = verificationSessionId,
                        ephemeralKeySecret = ephemeralKeySecret
                    )
                } else {
                    showLoading(false)
                    verifyButton.isEnabled = true
                    Toast.makeText(
                        this@IdentityVerificationActivity,
                        "Failed to start verification. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                verifyButton.isEnabled = true
                Toast.makeText(
                    this@IdentityVerificationActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Handle Stripe Identity SDK result
    // -------------------------------------------------------------------------

    private fun onVerificationSheetResult(result: IdentityVerificationSheet.VerificationFlowResult) {
        when (result) {
            is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                // The user completed the scan. The actual VERIFIED status will be written
                // by the Stripe webhook → Firebase Cloud Function. Show "Pending" and
                // attach a real-time listener so the UI flips automatically when the
                // webhook fires — no polling, no manual refresh needed.
                updateUiForStatus(VerificationStatus.PENDING)
                Toast.makeText(
                    this,
                    "Verification submitted! We will notify you once it is confirmed.",
                    Toast.LENGTH_LONG
                ).show()

                val uid = auth.currentUser?.uid ?: return
                attachVerificationListener(uid)
            }
            is IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                verifyButton.isEnabled = true
                updateUiForStatus(VerificationStatus.CANCELED)
                Toast.makeText(this, "Verification canceled.", Toast.LENGTH_SHORT).show()
            }
            is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                verifyButton.isEnabled = true
                updateUiForStatus(VerificationStatus.FAILED)
                Toast.makeText(
                    this,
                    "Verification failed: ${result.throwable.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Real-time Firestore listener — fires automatically when the webhook writes
    // -------------------------------------------------------------------------

    /**
     * Attaches a real-time snapshot listener to `users/{uid}`.
     *
     * When the Stripe webhook fires and the Firebase Cloud Function writes
     * `verificationStatus = "VERIFIED"` (or any other terminal state) to Firestore,
     * this listener receives the update instantly and advances the UI without any
     * user action or manual polling.
     *
     * The listener is removed when the Activity is destroyed ([onDestroy]).
     */
    private fun attachVerificationListener(uid: String) {
        // Avoid duplicate listeners
        verificationListener?.remove()

        verificationListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val rawStatus = snapshot.getString("verificationStatus")
                val status = VerificationStatus.fromFirestore(rawStatus)

                updateUiForStatus(status)

                // Once we reach a terminal state, stop listening to save bandwidth.
                if (status == VerificationStatus.VERIFIED || status == VerificationStatus.FAILED) {
                    verificationListener?.remove()
                    verificationListener = null

                    if (status == VerificationStatus.VERIFIED) {
                        Toast.makeText(
                            this,
                            "Identity Verified! You can now go online.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private fun updateUiForStatus(status: VerificationStatus) {
        when (status) {
            VerificationStatus.NOT_STARTED -> {
                statusTextView.text = "Not Verified"
                descriptionTextView.text =
                    "Verify your identity to unlock premium features such as going online and offering at-home services."
                verifyButton.visibility = View.VISIBLE
                verifyButton.isEnabled = true
                verifyButton.text = "Verify My Identity"
            }
            VerificationStatus.PENDING -> {
                statusTextView.text = "Pending"
                descriptionTextView.text =
                    "Your verification is being reviewed. This usually takes a few minutes. This screen will update automatically."
                verifyButton.visibility = View.GONE
            }
            VerificationStatus.VERIFIED -> {
                statusTextView.text = "Verified ✓"
                descriptionTextView.text =
                    "Your identity has been verified. You can now go online and accept bookings."
                verifyButton.visibility = View.GONE
            }
            VerificationStatus.FAILED -> {
                statusTextView.text = "Verification Failed"
                descriptionTextView.text =
                    "Your verification attempt failed. Please try again with a clear photo of your document."
                verifyButton.visibility = View.VISIBLE
                verifyButton.isEnabled = true
                verifyButton.text = "Try Again"
            }
            VerificationStatus.CANCELED -> {
                statusTextView.text = "Canceled"
                descriptionTextView.text =
                    "You canceled the verification. You can start again at any time."
                verifyButton.visibility = View.VISIBLE
                verifyButton.isEnabled = true
                verifyButton.text = "Verify My Identity"
            }
            VerificationStatus.EXPIRED -> {
                statusTextView.text = "Session Expired"
                descriptionTextView.text =
                    "Your verification session expired. Please start a new one."
                verifyButton.visibility = View.VISIBLE
                verifyButton.isEnabled = true
                verifyButton.text = "Verify My Identity"
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) verifyButton.isEnabled = false
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, IdentityVerificationActivity::class.java)
    }
}
