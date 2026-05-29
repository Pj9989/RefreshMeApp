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
import com.refreshme.legal.CustomerTermsAcceptanceScreen
import com.refreshme.legal.FcraConsentScreen
import com.refreshme.legal.StylistContractorAgreementScreen
import com.refreshme.stylist.StylistDashboardActivity
import com.refreshme.ui.theme.RefreshMeTheme

class StylistOnboardingActivity : AppCompatActivity() {

    private val viewModel: StylistOnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefreshMeTheme {
                val onboardingState by viewModel.onboardingState.collectAsState()

                LaunchedEffect(onboardingState) {
                    if (onboardingState is OnboardingState.Error) {
                        Toast.makeText(this@StylistOnboardingActivity, (onboardingState as OnboardingState.Error).message, Toast.LENGTH_LONG).show()
                    }
                }

                // Step machine: profile → Terms/Privacy → contractor agreement → FCRA consent → save & dashboard.
                // Contract first establishes independent-contractor status BEFORE background
                // check; FCRA (15 USC 1681b(b)) requires the background-check disclosure be on
                // a standalone screen with nothing else on it.
                var step by remember { mutableStateOf(OnboardingStep.PROFILE) }
                var pendingProfile by remember {
                    mutableStateOf<Pair<String, String>?>(null)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (step) {
                        OnboardingStep.PROFILE -> StylistOnboardingScreen(
                            isLoading = onboardingState is OnboardingState.Loading,
                            onContinue = { fullName, businessName ->
                                pendingProfile = fullName to businessName
                                step = OnboardingStep.CUSTOMER_TERMS
                            }
                        )
                        OnboardingStep.CUSTOMER_TERMS -> CustomerTermsAcceptanceScreen(
                            onAccepted = { step = OnboardingStep.CONTRACTOR_AGREEMENT },
                            onCancel = {
                                FirebaseAuth.getInstance().signOut()
                                finish()
                            }
                        )
                        OnboardingStep.CONTRACTOR_AGREEMENT -> StylistContractorAgreementScreen(
                            onAccepted = { step = OnboardingStep.FCRA_CONSENT },
                            onCancel = {
                                // Cannot proceed as stylist without the agreement.
                                FirebaseAuth.getInstance().signOut()
                                finish()
                            }
                        )
                        OnboardingStep.FCRA_CONSENT -> FcraConsentScreen(
                            onAccepted = {
                                val (fullName, businessName) = pendingProfile
                                    ?: run { step = OnboardingStep.PROFILE; return@FcraConsentScreen }
                                viewModel.saveStylistData(fullName, businessName) {
                                    val intent = Intent(this, StylistDashboardActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                            },
                            onCancel = {
                                // Cannot be a stylist on the platform without FCRA consent
                                // (required to run a background check on them).
                                FirebaseAuth.getInstance().signOut()
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, StylistOnboardingActivity::class.java)
    }

    private enum class OnboardingStep {
        PROFILE,
        CUSTOMER_TERMS,
        CONTRACTOR_AGREEMENT,
        FCRA_CONSENT,
    }
}
