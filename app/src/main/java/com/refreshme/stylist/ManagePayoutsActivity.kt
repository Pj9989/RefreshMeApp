package com.refreshme.stylist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.R

/**
 * Activity for stylists to set up and manage their Stripe Connect Express payout account.
 * Calls the createConnectAccount Cloud Function and opens the returned onboarding URL.
 */
class ManagePayoutsActivity : AppCompatActivity() {

    private lateinit var btnSetupStripe: Button
    private lateinit var tvStripeStatus: TextView
    private lateinit var progressBar: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_payouts)

        supportActionBar?.apply {
            title = getString(R.string.manage_payouts)
            setDisplayHomeAsUpEnabled(true)
        }

        btnSetupStripe = findViewById(R.id.btnSetupStripe)
        tvStripeStatus = findViewById(R.id.tvStripeStatus)
        progressBar = findViewById(R.id.progressBar)

        loadStripeStatus()

        btnSetupStripe.setOnClickListener {
            openStripeOnboarding()
        }
    }

    private fun loadStripeStatus() {
        val uid = auth.currentUser?.uid ?: return
        setLoading(true)

        db.collection("stylists").document(uid).get()
            .addOnSuccessListener { doc ->
                setLoading(false)
                val stripeAccountId = doc.getString("stripeAccountId")
                val onboardingComplete = doc.getBoolean("stripeOnboardingComplete") ?: false
                val chargesEnabled = doc.getBoolean("stripeChargesEnabled") ?: false

                if (!stripeAccountId.isNullOrBlank()) {
                    btnSetupStripe.text = getString(R.string.manage_stripe_account)
                    tvStripeStatus.visibility = View.VISIBLE
                    tvStripeStatus.text = when {
                        chargesEnabled && onboardingComplete -> "✅ Stripe account connected — payouts enabled"
                        else -> "⏳ Stripe account pending — tap to finish onboarding"
                    }
                } else {
                    btnSetupStripe.text = getString(R.string.setup_stripe_account)
                    tvStripeStatus.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                setLoading(false)
            }
    }

    private fun openStripeOnboarding() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please sign in first.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Call the createConnectAccount Firebase function to get the Stripe onboarding URL
        functions.getHttpsCallable("createConnectAccount")
            .call()
            .addOnSuccessListener { result ->
                setLoading(false)
                @Suppress("UNCHECKED_CAST")
                val data = result.getData() as? Map<String, Any>
                val url = data?.get("url") as? String
                if (!url.isNullOrBlank()) {
                    openUrl(url)
                    // Refresh status when user returns from Stripe
                    loadStripeStatus()
                } else {
                    Toast.makeText(this, "Could not get Stripe onboarding link. Try again.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                val msg = e.message ?: "Unknown error"
                Toast.makeText(this, "Error: $msg", Toast.LENGTH_LONG).show()
            }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No browser available to open link.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSetupStripe.isEnabled = !loading
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Refresh status every time user returns to this screen (e.g. after Stripe onboarding)
        loadStripeStatus()
    }
}
