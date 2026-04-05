package com.refreshme.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.refreshme.MainActivity
import com.refreshme.ui.theme.RefreshMeTheme

class CustomerOnboardingActivity : AppCompatActivity() {

    private val viewModel: CustomerOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CustomerOnboardingScreen(
                        onContinue = { name, email ->
                            viewModel.saveCustomerData(name, email) {
                                startActivity(MainActivity.newIntent(this))
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, CustomerOnboardingActivity::class.java)
    }
}