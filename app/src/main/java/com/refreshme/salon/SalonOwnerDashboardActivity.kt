package com.refreshme.salon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SalonOwnerDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SalonOwnerDashboardScreen(
                        onLogout = {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            startActivity(com.refreshme.auth.SignInActivity.newIntent(this))
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, SalonOwnerDashboardActivity::class.java)
    }
}