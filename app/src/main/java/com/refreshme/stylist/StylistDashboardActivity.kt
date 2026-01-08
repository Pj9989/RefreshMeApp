package com.refreshme.stylist

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R
import com.refreshme.databinding.ActivityStylistDashboardBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StylistDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStylistDashboardBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStylistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_stylist) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavStylist.setupWithNavController(navController)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    override fun onPause() {
        super.onPause()
        // When app goes to background, set stylist offline
        if (lifecycle.currentState == Lifecycle.State.STARTED) {
            setOfflineStatus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // When activity is destroyed, set stylist offline
        setOfflineStatus()
    }

    /**
     * Set the stylist's online status to false in Firestore
     */
    private fun setOfflineStatus() {
        lifecycleScope.launch {
            auth.currentUser?.uid?.let { uid ->
                try {
                    val updates = hashMapOf<String, Any>(
                        "online" to false,
                        "availableNow" to false,
                        "lastOnlineAt" to FieldValue.serverTimestamp()
                    )
                    
                    firestore.collection("stylists").document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    android.util.Log.e("StylistDashboard", "Failed to set offline status", e)
                }
            }
        }
    }
}
