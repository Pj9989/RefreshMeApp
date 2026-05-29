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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.MainActivity
import com.refreshme.legal.CustomerTermsAcceptanceScreen
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
                
                // Pending profile captured before terms acceptance.
                // Once the user accepts ToS + Privacy, we commit the profile
                // and navigate to MainActivity. Declining returns them to
                // the profile screen so they can exit without an account.
                var pendingProfile by remember {
                    mutableStateOf<Triple<String, String, android.net.Uri?>?>(null)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pending = pendingProfile
                    if (pending == null) {
                        CustomerOnboardingScreen(
                            isLoading = onboardingState is OnboardingState.Loading,
                            onContinue = { name, email, photoUri ->
                                pendingProfile = Triple(name, email, photoUri)
                            }
                        )
                    } else {
                        CustomerTermsAcceptanceScreen(
                            onAccepted = {
                                val (name, email, photoUri) = pending
                                viewModel.saveCustomerData(name, email, photoUri) {
                                    startActivity(MainActivity.newIntent(this))
                                    finish()
                                }
                            },
                            onCancel = {
                                // User declined: sign them out so they can't
                                // reach the app without accepting the terms.
                                FirebaseAuth.getInstance().signOut()
                                pendingProfile = null
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, CustomerOnboardingActivity::class.java)
    }
}