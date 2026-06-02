package com.refreshme.legal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.ui.theme.RefreshMeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Re-acceptance gate for EXISTING users. Launched on app start
 * (MainActivity / StylistDashboardActivity) to force any user whose
 * stored acceptance version is behind [LegalVersions] to accept the
 * current text before continuing to use the app.
 *
 * - Customers must have current CUSTOMER_TOS + PRIVACY_POLICY.
 * - Stylists additionally must have STYLIST_CONTRACTOR_AGREEMENT + FCRA_CONSENT.
 *
 * On decline, the user is signed out. The hosting activity handles
 * routing back to the role-select screen by observing activity result.
 */
class LegalGateActivity : AppCompatActivity() {

    private data class Required(
        val customerToS: Boolean,
        val privacy: Boolean,
        val contractor: Boolean,
        val fcra: Boolean,
    ) {
        val any get() = customerToS || privacy || contractor || fcra
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RefreshMeTheme { GateRoot() } }
    }

    @androidx.compose.runtime.Composable
    private fun GateRoot() {
        val repo = remember { LegalRepository() }
        var required by remember { mutableStateOf<Required?>(null) }

        // First: figure out what's missing. Null = still loading.
        LaunchedEffect(Unit) {
            required = computeRequired(repo)
            if (required?.any == false) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        val req = required
        if (req == null || !req.any) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return
        }

        // Show screens one at a time, in required order.
        var step by remember {
            mutableStateOf(firstStep(req))
        }

        val advance: () -> Unit = {
            val next = nextStep(step, req)
            if (next == null) {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                step = next
            }
        }
        val decline: () -> Unit = {
            FirebaseAuth.getInstance().signOut()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (step) {
                Step.CUSTOMER_TERMS -> CustomerTermsAcceptanceScreen(
                    onAccepted = advance,
                    onCancel = decline,
                    repository = repo,
                )
                Step.CONTRACTOR -> StylistContractorAgreementScreen(
                    onAccepted = advance,
                    onCancel = decline,
                    repository = repo,
                )
                Step.FCRA -> FcraConsentScreen(
                    onAccepted = advance,
                    onCancel = decline,
                    repository = repo,
                )
            }
        }
    }

    private enum class Step { CUSTOMER_TERMS, CONTRACTOR, FCRA }

    private fun firstStep(r: Required): Step = when {
        r.customerToS || r.privacy -> Step.CUSTOMER_TERMS
        r.contractor               -> Step.CONTRACTOR
        else                       -> Step.FCRA
    }

    private fun nextStep(current: Step, r: Required): Step? = when (current) {
        Step.CUSTOMER_TERMS -> when {
            r.contractor -> Step.CONTRACTOR
            r.fcra       -> Step.FCRA
            else         -> null
        }
        Step.CONTRACTOR -> if (r.fcra) Step.FCRA else null
        Step.FCRA       -> null
    }

    private suspend fun computeRequired(repo: LegalRepository): Required = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Required(false, false, false, false)

        val role = runCatching {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid).get().await()
                .getString("role")?.uppercase()
        }.getOrNull() ?: ""

        val isStylist = role == "STYLIST"
        Required(
            customerToS = repo.needsAcceptance(LegalDocKeys.CUSTOMER_TOS,  LegalVersions.CUSTOMER_TOS),
            privacy     = repo.needsAcceptance(LegalDocKeys.PRIVACY_POLICY, LegalVersions.PRIVACY_POLICY),
            contractor  = isStylist && repo.needsAcceptance(
                LegalDocKeys.STYLIST_CONTRACTOR_AGREEMENT,
                LegalVersions.STYLIST_CONTRACTOR_AGREEMENT
            ),
            fcra        = isStylist && repo.needsAcceptance(
                LegalDocKeys.FCRA_BACKGROUND_CHECK_CONSENT,
                LegalVersions.FCRA_BACKGROUND_CHECK_CONSENT
            ),
        )
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, LegalGateActivity::class.java)
    }
}
