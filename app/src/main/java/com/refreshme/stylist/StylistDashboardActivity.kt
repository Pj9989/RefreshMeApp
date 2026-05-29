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
import androidx.navigation.navOptions
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

        askNotificationPermission()

        setupAuthStateListener()
        
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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavStylist.visibility =
                if (destination.id == R.id.chatFragment) View.GONE else View.VISIBLE
        }

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Keep the bottom navigation fully above Android's gesture/navigation area.
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
        
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Handle Stripe Connect return deep link: refreshme://dashboard
        if (intent?.action == Intent.ACTION_VIEW && intent.data?.scheme == "refreshme") {
            Log.d(TAG, "Deep link from Stripe onboarding return")
            Toast.makeText(this, "✅ Stripe setup complete! Refreshing your account status…", Toast.LENGTH_LONG).show()
            val tabNavOptions = navOptions {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            // Navigate to payouts tab so the stylist can see their updated status
            navController.navigate(R.id.payoutsEarningsFragment, null, tabNavOptions)
            return
        }

        val type = intent?.getStringExtra("notification_type")
        val targetId = if (type == "chat") {
            intent.getStringExtra("other_user_id")
                ?: intent.getStringExtra("sender_id")
                ?: resolveChatPartnerId(intent.getStringExtra("target_id") ?: intent.getStringExtra("chat_id"))
        } else {
            intent?.getStringExtra("target_id") ?: intent?.getStringExtra("chat_id")
        }
        
        Log.d(TAG, "Handling intent: type=$type, targetId=$targetId")
        
        // Tab destinations use saveState/restoreState to keep back stacks clean
        val tabNavOptions = navOptions {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        
        when (type) {
            "chat" -> if (targetId != null) {
                navController.navigate(R.id.chatFragment, bundleOf("otherUserId" to targetId))
            }
            "booking_request", "new_booking" -> {
                navController.navigate(R.id.stylistBookingsFragment, null, tabNavOptions)
            }
        }
    }

    private fun resolveChatPartnerId(chatId: String?): String? {
        val currentUserId = auth.currentUser?.uid ?: return chatId
        val parts = chatId?.split("_") ?: return null
        return parts.firstOrNull { it.isNotBlank() && it != currentUserId } ?: chatId
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
        // Disabled logic related to displaying trial status
        // as trial subscriptions are no longer used.
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
                if (!documentSnapshot.exists()) {
                    val initialData = hashMapOf<String, Any>(
                        "name" to (auth.currentUser?.displayName ?: getString(R.string.default_stylist_name)),
                        "email" to (auth.currentUser?.email ?: ""),
                        "role" to "STYLIST",
                        "online" to false,
                        "availableNow" to false,
                        "available" to false,
                        "subscriptionActive" to false,
                        "services" to emptyList<Map<String, Any>>(),
                        "categories" to listOf("hair"),
                        "servesGender" to listOf("Men", "Women", "Non-binary"),
                        "serviceLocationType" to "mobile",
                        "offersAtHomeService" to true,
                        "atHomeServiceFee" to 20.0,
                        "maxTravelRangeKm" to 15,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("stylists").document(uid)
                        .set(initialData, SetOptions.merge())
                        .addOnFailureListener { e ->
                            // Surface the error instead of leaving the dashboard
                            // blank if the starter-profile create fails (permission,
                            // network, etc.). Without this the UI silently hangs.
                            Log.e(TAG, "Error creating starter stylist profile", e)
                            Toast.makeText(
                                this,
                                "Couldn't set up your stylist profile. Please check your connection and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching stylist data", e)
                Toast.makeText(
                    this,
                    "Couldn't load your stylist profile. Please check your connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    companion object {
        const val EXTRA_PURCHASE_SUCCESS = "EXTRA_PURCHASE_SUCCESS"
    }
}
