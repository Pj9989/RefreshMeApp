package com.refreshme

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.refreshme.auth.RoleSelectActivity
import com.refreshme.auth.SignInActivity
import com.refreshme.util.RoleBasedNavigationManager
import com.refreshme.util.RoleBasedNavigationManager.UserRole
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerDashboardActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private val isStylistBrowseMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_ALLOW_STYLIST_BROWSE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_customer_dashboard)
        val root = findViewById<View>(R.id.root)
        window.statusBarColor = Color.parseColor("#0D0D17")
        window.navigationBarColor = Color.parseColor("#11111B")
        WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, root).isAppearanceLightNavigationBars = false

        if (!isStylistBrowseMode) {
            RoleBasedNavigationManager.getUserRole { role ->
                if (isFinishing || role == UserRole.CUSTOMER) return@getUserRole
                if (role == UserRole.UNKNOWN) {
                    startActivity(Intent(this, RoleSelectActivity::class.java))
                } else {
                    RoleBasedNavigationManager.navigateToDashboard(this, role)
                }
                finish()
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavView)
        bottomNavView.setupWithNavController(navController)
        bottomNavView.isItemActiveIndicatorEnabled = false

        if (isStylistBrowseMode) {
            bottomNavView.menu
                .findItem(R.id.userProfileFragment)
                ?.title = "Stylist"
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavView.visibility = when (destination.id) {
                R.id.chatFragment,
                R.id.bookingFragment,
                R.id.activeMobileBookingFragment,
                R.id.stylistDetailsFragment,
                R.id.virtualTryOnFragment,
                R.id.faceScanFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return

        if (uri.scheme == "https" && uri.host == "refreshme-74f79.web.app") {
            val stylistId = uri.pathSegments
                .takeIf { it.size >= 2 && it[0] == "stylist" }
                ?.get(1)
            if (!stylistId.isNullOrBlank()) {
                navController.navigate(R.id.stylistDetailsFragment, Bundle().apply {
                    putString("stylistId", stylistId)
                })
            }
            return
        }

        if (uri.scheme != "refreshme") return

        when (uri.host) {
            "home" -> {
                navController.navigate(R.id.homeFragment)
            }
            "stylist" -> {
                val stylistId = uri.pathSegments.firstOrNull()
                if (!stylistId.isNullOrBlank()) {
                    navController.navigate(R.id.stylistDetailsFragment, Bundle().apply {
                        putString("stylistId", stylistId)
                    })
                }
            }
            "payment-success" -> {
                Toast.makeText(this, "Payment confirmed. Your booking is being updated.", Toast.LENGTH_LONG).show()
                navController.navigate(R.id.bookingsFragment)
            }
            "payment-cancelled" -> {
                Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_LONG).show()
                navController.navigate(R.id.bookingsFragment)
            }
        }
    }

    companion object {
        const val EXTRA_ALLOW_STYLIST_BROWSE = "extra_allow_stylist_browse"

        fun newIntent(context: Context, allowStylistBrowse: Boolean = false): Intent {
            return Intent(context, CustomerDashboardActivity::class.java).apply {
                putExtra(EXTRA_ALLOW_STYLIST_BROWSE, allowStylistBrowse)
                if (!allowStylistBrowse) {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        }
    }
}
