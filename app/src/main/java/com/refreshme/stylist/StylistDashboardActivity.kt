package com.refreshme.stylist

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.R
import com.refreshme.StylistSubscriptionActivity
import com.refreshme.auth.SignInActivity 
import com.refreshme.databinding.ActivityStylistDashboardBinding
import java.util.concurrent.TimeUnit
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StylistDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStylistDashboardBinding
    private lateinit var navController: NavController
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private val TAG = "StylistDashboard"
    private val TRIAL_DURATION_MS = TimeUnit.DAYS.toMillis(30)
    private var isInitialCheckComplete = false 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            val intent = Intent(this, SignInActivity::class.java) 
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() 
            return 
        }

        binding = ActivityStylistDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupAuthStateListener()
        askNotificationPermission()
        
        val isFreshlySubscribed = intent.getBooleanExtra(EXTRA_PURCHASE_SUCCESS, false)
        
        if (!isFreshlySubscribed && !isInitialCheckComplete) {
            checkSubscriptionAndRedirect()
        } else {
            isInitialCheckComplete = true
        }
        
        if (isFreshlySubscribed) {
            calculateAndDisplayTrialStatus() 
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_stylist) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavStylist.setupWithNavController(navController)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Only apply top inset to root, letting bottom nav handle its own bottom inset
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, 0)
            insets
        }
        
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val type = intent?.getStringExtra("notification_type")
        val targetId = intent?.getStringExtra("target_id") ?: intent?.getStringExtra("chat_id")
        
        Log.d(TAG, "Handling intent: type=$type, targetId=$targetId")
        
        when (type) {
            "chat" -> if (targetId != null) {
                navController.navigate(R.id.chatFragment, bundleOf("otherUserId" to targetId))
            }
            "booking_request", "new_booking" -> {
                navController.navigate(R.id.stylistBookingsFragment)
            }
        }
    }
    
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun calculateAndDisplayTrialStatus() {
        val uid = auth.currentUser?.uid ?: return
        
        firestore.collection("stylists").document(uid).get()
            .addOnSuccessListener { document ->
                val isSubActive = document.getBoolean("subscriptionActive") == true
                if (isSubActive) return@addOnSuccessListener

                val snapshot = document.get("trialStartTime")
                val trialStartTimeMillis = when (snapshot) {
                    is Timestamp -> snapshot.toDate().time
                    is Long -> snapshot
                    is java.util.Date -> snapshot.time
                    else -> 0L
                }

                if (trialStartTimeMillis > 0) {
                    val timeElapsed = System.currentTimeMillis() - trialStartTimeMillis
                    val timeLeft = TRIAL_DURATION_MS - timeElapsed
                    
                    if (timeLeft > 0) {
                        val daysLeft = TimeUnit.MILLISECONDS.toDays(timeLeft)
                        Toast.makeText(this, getString(R.string.trial_active_message, daysLeft.toInt()), Toast.LENGTH_LONG).show()
                    }
                }
            }
    }


    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                auth.removeAuthStateListener(authStateListener)
                val intent = Intent(this, SignInActivity::class.java) 
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun checkSubscriptionAndRedirect() {
        if (isInitialCheckComplete) return 
        isInitialCheckComplete = true 

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("stylists").document(uid).get(Source.SERVER)
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    if (!isSubscriptionOrTrialActive(documentSnapshot)) {
                        if (!isFinishing && !isDestroyed) {
                            startActivity(StylistSubscriptionActivity.newIntent(this))
                            finish()
                        }
                    }
                    calculateAndDisplayTrialStatus() 
                } else {
                    grantTrialIfNecessary(uid) 
                }
            }
            .addOnFailureListener { e ->
                if (!isFinishing && !isDestroyed) {
                    startActivity(StylistSubscriptionActivity.newIntent(this))
                    finish()
                }
            }
    }
    
    private fun grantTrialIfNecessary(uid: String) {
        val initialData = hashMapOf<String, Any>(
            "trialStartTime" to FieldValue.serverTimestamp(),
            "subscriptionActive" to false,
            "name" to (auth.currentUser?.displayName ?: getString(R.string.default_stylist_name))
        )

        firestore.collection("stylists").document(uid)
            .set(initialData, SetOptions.merge())
            .addOnSuccessListener {
                calculateAndDisplayTrialStatus()
                Toast.makeText(this, getString(R.string.welcome_trial_started), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                if (!isFinishing && !isDestroyed) {
                    startActivity(StylistSubscriptionActivity.newIntent(this))
                    finish()
                }
            }
    }

    private fun isSubscriptionOrTrialActive(snapshot: DocumentSnapshot): Boolean {
        val isSubActive = snapshot.getBoolean("subscriptionActive") == true
        val trialStartTime = when (val time = snapshot.get("trialStartTime")) {
             is Timestamp -> time.toDate().time
             is Long -> time
             is java.util.Date -> time.time
             else -> 0L
        }
        
        val isTrialActive = if (trialStartTime > 0) {
            val trialEndMillis = trialStartTime + TRIAL_DURATION_MS
            System.currentTimeMillis() < trialEndMillis
        } else {
            false
        }
        return isSubActive || isTrialActive
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    companion object {
        const val EXTRA_PURCHASE_SUCCESS = "EXTRA_PURCHASE_SUCCESS"
    }
}
