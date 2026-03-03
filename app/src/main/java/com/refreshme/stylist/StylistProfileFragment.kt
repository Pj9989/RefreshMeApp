package com.refreshme.stylist

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R
import com.refreshme.identity.IdentityVerificationActivity
import com.refreshme.identity.VerificationStatus

/**
 * Stylist profile fragment that manages the online/offline toggle and
 * surfaces the identity verification status to the user.
 *
 * Gate logic:
 * - A stylist must have `verified == true` in Firestore before they can go online.
 * - If not verified, tapping the toggle shows a prompt to complete verification.
 * - A "Verify Identity" button is shown when the stylist is not yet verified.
 */
class StylistProfileFragment : Fragment() {

    private lateinit var onlineToggle: Switch
    private lateinit var verificationStatusText: TextView
    private lateinit var verifyIdentityButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val stylistUid = auth.currentUser?.uid
    private val stylistRef = stylistUid?.let { firestore.collection("stylists").document(it) }

    private var hasActiveSubscription = true
    private var isVerified = false
    private var verificationStatus = VerificationStatus.NOT_STARTED

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stylist_profile, container, false)

        onlineToggle = view.findViewById(R.id.onlineToggle)
        verificationStatusText = view.findViewById(R.id.verificationStatusText)
        verifyIdentityButton = view.findViewById(R.id.verifyIdentityButton)

        if (stylistRef == null) {
            Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_LONG).show()
        } else {
            loadProfileData()
        }

        verifyIdentityButton.setOnClickListener {
            startActivity(IdentityVerificationActivity.newIntent(requireContext()))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Reload when returning from IdentityVerificationActivity
        if (stylistRef != null) {
            loadProfileData()
        }
    }

    // -------------------------------------------------------------------------
    // Load profile data (online status + verification status)
    // -------------------------------------------------------------------------

    private fun loadProfileData() {
        // Load from the users collection for verification status
        val uid = stylistUid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                isVerified = userDoc.getBoolean("verified") ?: false
                val rawStatus = userDoc.getString("verificationStatus")
                verificationStatus = VerificationStatus.fromFirestore(rawStatus)
                updateVerificationUi()
            }

        // Load stylist-specific data (online status, subscription)
        stylistRef?.get()
            ?.addOnSuccessListener { document ->
                val isOnline = document.getBoolean("online") ?: false
                hasActiveSubscription = document.getBoolean("hasActiveSubscription") ?: true

                onlineToggle.isChecked = isOnline
                onlineToggle.text = if (isOnline) "Online – Receiving Requests" else "Go Online"
                onlineToggle.setTextColor(if (isOnline) Color.GREEN else Color.BLACK)

                setupToggleListener()
            }
            ?.addOnFailureListener {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    // -------------------------------------------------------------------------
    // Verification status UI
    // -------------------------------------------------------------------------

    private fun updateVerificationUi() {
        when (verificationStatus) {
            VerificationStatus.VERIFIED -> {
                verificationStatusText.text = "Identity Verified ✓"
                verificationStatusText.setTextColor(Color.GREEN)
                verifyIdentityButton.visibility = View.GONE
            }
            VerificationStatus.PENDING -> {
                verificationStatusText.text = "Verification Pending…"
                verificationStatusText.setTextColor(Color.YELLOW)
                verifyIdentityButton.visibility = View.GONE
            }
            VerificationStatus.FAILED -> {
                verificationStatusText.text = "Verification Failed – Tap to retry"
                verificationStatusText.setTextColor(Color.RED)
                verifyIdentityButton.visibility = View.VISIBLE
                verifyIdentityButton.text = "Retry Verification"
            }
            else -> {
                verificationStatusText.text = "Identity Not Verified"
                verificationStatusText.setTextColor(Color.GRAY)
                verifyIdentityButton.visibility = View.VISIBLE
                verifyIdentityButton.text = "Verify Identity"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Online toggle
    // -------------------------------------------------------------------------

    private fun setupToggleListener() {
        onlineToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasActiveSubscription) {
                onlineToggle.isChecked = false
                Toast.makeText(context, "Activate your subscription to go online.", Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }

            if (isChecked && !isVerified) {
                onlineToggle.isChecked = false
                Toast.makeText(
                    context,
                    "You must verify your identity before going online.",
                    Toast.LENGTH_LONG
                ).show()
                // Prompt the user to start verification
                startActivity(IdentityVerificationActivity.newIntent(requireContext()))
                return@setOnCheckedChangeListener
            }

            updateOnlineStatus(isChecked)
        }
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val uid = stylistUid ?: return

        val updates = mutableMapOf<String, Any>(
            "online" to isOnline,
            "availableNow" to isOnline
        )

        if (isOnline) {
            updates["lastOnlineAt"] = Timestamp.now()
        }

        firestore.collection("stylists").document(uid)
            .update(updates)
            .addOnSuccessListener {
                onlineToggle.text = if (isOnline) "Online – Receiving Requests" else "Go Online"
                onlineToggle.setTextColor(if (isOnline) Color.GREEN else Color.BLACK)
                Toast.makeText(
                    context,
                    if (isOnline) "You are now online" else "You are now offline",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                onlineToggle.isChecked = !isOnline
                onlineToggle.setTextColor(if (!isOnline) Color.GREEN else Color.BLACK)
                Toast.makeText(context, "Failed to update online status", Toast.LENGTH_SHORT).show()
            }
    }
}
