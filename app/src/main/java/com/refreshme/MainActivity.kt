package com.refreshme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.auth.RoleSelectActivity
import com.refreshme.databinding.ActivityMainBinding
import com.refreshme.util.RoleBasedNavigationManager
import com.refreshme.util.RoleBasedNavigationManager.UserRole
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightNavigationBars = true

        if (auth.currentUser == null) {
            startActivity(Intent(this, RoleSelectActivity::class.java))
            finish()
            return
        }

        RoleBasedNavigationManager.getUserRole { role ->
            if (isFinishing || role == UserRole.CUSTOMER) return@getUserRole
            if (role == UserRole.UNKNOWN) {
                startActivity(Intent(this, RoleSelectActivity::class.java))
            } else {
                RoleBasedNavigationManager.navigateToDashboard(this, role)
            }
            finish()
        }

        askNotificationPermission()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        
        binding.bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.chatFragment, R.id.bookingFragment, R.id.faceScanFragment, R.id.stylistDetailsFragment -> {
                    binding.bottomNavView.visibility = android.view.View.GONE
                }
                else -> {
                    binding.bottomNavView.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        // Edge-to-edge handling: only apply top inset to root, let bottom nav handle its own bottom inset
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, 0)
            insets
        }
        
        // Handle notification if activity was started from one
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val type = intent?.getStringExtra("notification_type")
        val targetId = if (type == "chat") {
            intent.getStringExtra("other_user_id")
                ?: intent.getStringExtra("sender_id")
                ?: resolveChatPartnerId(intent.getStringExtra("target_id") ?: intent.getStringExtra("chat_id"))
        } else {
            intent?.getStringExtra("target_id") ?: intent?.getStringExtra("chat_id")
        }
        val stylistId = intent?.getStringExtra("stylistId")
        
        Log.d("MainActivity", "Handling intent: type=$type, targetId=$targetId, stylistId=$stylistId")
        
        if (type == "chat" && targetId != null) {
            // Navigate to chat fragment
            navController.navigate(
                R.id.chatFragment,
                bundleOf("otherUserId" to targetId)
            )
        } else if (stylistId != null) {
            // Navigate to stylist details from rebooking reminder
            navController.navigate(
                R.id.stylistDetailsFragment,
                bundleOf("stylistId" to stylistId)
            )
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    companion object {
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
