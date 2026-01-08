package com.refreshme.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.refreshme.stylist.StylistMainActivity

class StylistOnboardingActivity : AppCompatActivity() {

    private val viewModel: StylistOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StylistOnboardingScreen(
                onContinue = { fullName, businessName ->
                    viewModel.saveStylistData(fullName, businessName) {
                        startActivity(Intent(this, StylistMainActivity::class.java))
                        finish()
                    }
                }
            )
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, StylistOnboardingActivity::class.java)
    }
}