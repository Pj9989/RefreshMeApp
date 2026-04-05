package com.refreshme.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.refreshme.MainActivity
import com.refreshme.ui.theme.RefreshMeTheme

class CustomerOnboardingActivity : AppCompatActivity() {

    private val viewModel: CustomerOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshMeTheme {
                val onboardingState by viewModel.onboardingState.collectAsState()
                
                LaunchedEffect(onboardingState) {
                    if (onboardingState is OnboardingState.Error) {
                        Toast.makeText(this@CustomerOnboardingActivity, (onboardingState as OnboardingState.Error).message, Toast.LENGTH_LONG).show()
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CustomerOnboardingScreen(
                        isLoading = onboardingState is OnboardingState.Loading,
                        onContinue = { name, email, photoUri ->
                            viewModel.saveCustomerData(name, email, photoUri) {
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