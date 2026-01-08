package com.refreshme.stylist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.refreshme.R
import com.refreshme.databinding.ActivityStylistMainBinding

class StylistMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStylistMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStylistMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userRole = intent.getStringExtra(EXTRA_USER_ROLE)

        // This activity is only for STYLISTS. If the role is not correct,
        // finish the activity. This is a safeguard.
        if (userRole != "STYLIST") {
            finish()
            return
        }

        // Setup Navigation
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.stylist_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.stylistBottomNavView.setupWithNavController(navController)
    }

    companion object {
        const val EXTRA_USER_ROLE = "extra_user_role"

        fun newIntent(context: Context, userRole: String): Intent {
            return Intent(context, StylistMainActivity::class.java).apply {
                putExtra(EXTRA_USER_ROLE, userRole)
            }
        }
    }
}