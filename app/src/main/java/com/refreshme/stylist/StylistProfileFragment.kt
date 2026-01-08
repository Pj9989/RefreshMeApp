package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import android.graphics.Color // Added for color change animation
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R

class StylistProfileFragment : Fragment() {

    private lateinit var onlineToggle: Switch
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val stylistUid = auth.currentUser?.uid
    private val stylistRef = stylistUid?.let { firestore.collection("stylists").document(it) }

    // State variable for subscription status
    private var hasActiveSubscription = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stylist_profile, container, false)
        
        onlineToggle = view.findViewById(R.id.onlineToggle)
        
        if (stylistRef == null) {
            Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_LONG).show()
        } else {
            loadOnlineStatus()
            // setupToggleListener() is now called from loadOnlineStatus
        }
        
        return view
    }
    
    private fun loadOnlineStatus() {
        stylistRef?.get()
            ?.addOnSuccessListener { document ->
                // Fetch status and subscription
                val isOnline = document.getBoolean("online") ?: false
                hasActiveSubscription = document.getBoolean("hasActiveSubscription") ?: true // Task 1: Check subscription status
                
                // Set initial state and visual cue
                onlineToggle.isChecked = isOnline
                onlineToggle.text = if (isOnline) "Online - Receiving Requests" else "Go Online"
                onlineToggle.setTextColor(if (isOnline) Color.GREEN else Color.BLACK) // Task 2: Initial color
                
                setupToggleListener()
            }
            ?.addOnFailureListener {
                Toast.makeText(context, "Failed to load online status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupToggleListener() {
        onlineToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasActiveSubscription) {
                // Task 1: Disable Online toggle if subscription inactive
                onlineToggle.isChecked = false // Revert state
                Toast.makeText(context, "Activate subscription to go online", Toast.LENGTH_LONG).show()
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
                onlineToggle.text = if (isOnline) "Online - Receiving Requests" else "Go Online"
                onlineToggle.setTextColor(if (isOnline) Color.GREEN else Color.BLACK) // Task 2: Color transition
                Toast.makeText(context, if (isOnline) "You are now online" else "You are now offline", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Revert toggle state on failure and show error
                onlineToggle.isChecked = !isOnline
                onlineToggle.setTextColor(if (!isOnline) Color.GREEN else Color.BLACK) // Revert color
                Toast.makeText(context, "Failed to update online status", Toast.LENGTH_SHORT).show()
            }
    }
}