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
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.R
import com.stripe.android.identity.IdentityVerificationSheet
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Activity that manages the full Stripe Identity verification flow.
 *
 * Flow:
 * 1. Load current verification status from Firestore.
 * 2. If not yet verified, call the `createIdentityVerificationSession` Firebase Function.
 * 3. Launch the [IdentityVerificationSheet] with the returned ephemeral key secret.
 * 4. Handle the result and update the UI accordingly.
 *
 * The actual Firestore `verified` flag is written by the Firebase Function webhook
 * (`identity.verification_session.verified`) — this Activity only initiates the flow
 * and reflects the current status to the user.
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

    // -------------------------------------------------------------------------
    // Load current status from Firestore
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
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "You must be signed in to verify your identity.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        verifyButton.isEnabled = false

        lifecycleScope.launch {
            try {
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
                // The user completed the flow. The actual verified status will be
                // set by the Stripe webhook → Firebase Function. Show a pending state.
                updateUiForStatus(VerificationStatus.PENDING)
                Toast.makeText(
                    this,
                    "Verification submitted! We will notify you once it is confirmed.",
                    Toast.LENGTH_LONG
                ).show()
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
                    "Your verification is being reviewed. This usually takes a few minutes."
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