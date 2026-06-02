package com.refreshme.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Stylist onboarding screen. BLOCKS access to the stylist dashboard until
 * the stylist has agreed to the Independent Contractor Agreement and typed
 * their full legal name as an e-signature.
 *
 * UX: the whole page scrolls as a single column. The agreement body sits
 * at the top; checkboxes and signature appear below it so the user has to
 * physically scroll past the body to reach the signature form. The
 * "Sign & Continue" button is pinned to the bottom as a Scaffold bottomBar
 * so it's always reachable.
 *
 * Writes a single /legalAcceptances/{uid}/acceptances/stylist_contractor_agreement
 * record with the typed name in `signedName`. Under the ESIGN Act that
 * typed-name-plus-checkbox pattern constitutes a legally-binding electronic
 * signature as long as intent is clear (which the "Sign & Continue" button
 * provides).
 */

@Composable
fun StylistContractorAgreementScreen(
    onAccepted: () -> Unit,
    onCancel: () -> Unit,
    repository: LegalRepository = remember { LegalRepository() },
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var contractorAck by remember { mutableStateOf(false) }
    var licenseAck by remember { mutableStateOf(false) }
    var insuranceAck by remember { mutableStateOf(false) }
    var esignAck by remember { mutableStateOf(false) }
    var signedName by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()

    // Gate: all four checkboxes + a typed-name e-signature of at least 3 chars.
    // The scroll-to-end gate was removed because the body and the signature
    // form live in a single scrollable column — reaching the signature field
    // physically requires scrolling past the body.
    val canSubmit = contractorAck && licenseAck && insuranceAck &&
        esignAck && signedName.trim().length >= 3 && !submitting

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Cancel") }
                    Button(
                        enabled = canSubmit,
                        onClick = {
                            submitting = true
                            error = null
                            scope.launch {
                                try {
                                    repository.recordAcceptance(
                                        docKey = LegalDocKeys.STYLIST_CONTRACTOR_AGREEMENT,
                                        docVersion = LegalVersions.STYLIST_CONTRACTOR_AGREEMENT,
                                        signedName = signedName.trim(),
                                    )
                                    onAccepted()
                                } catch (e: Exception) {
                                    error = "Could not record your signature. " +
                                        "Please try again."
                                    submitting = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Sign & Continue", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Stylist Agreement",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version ${LegalVersions.STYLIST_CONTRACTOR_AGREEMENT}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Agreement body. Lives in the same scrollable column as the
            // checkboxes + signature — no inner scrollable surface, so the
            // user only has one thing to scroll.
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    AgreementBody(onOpenFull = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(LegalUrls.CONTRACTOR_AGREEMENT)))
                    })
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Confirm each of the following:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))

            LegalCheckRow(
                checked = contractorAck,
                onCheckedChange = { contractorAck = it },
                text = "I am an independent contractor, not an employee of RefreshMe."
            )
            LegalCheckRow(
                checked = licenseAck,
                onCheckedChange = { licenseAck = it },
                text = "I hold a valid, unrestricted cosmetology or barber license."
            )
            LegalCheckRow(
                checked = insuranceAck,
                onCheckedChange = { insuranceAck = it },
                text = "I will maintain my own general + professional liability insurance."
            )
            LegalCheckRow(
                checked = esignAck,
                onCheckedChange = { esignAck = it },
                text = "I consent to sign this agreement electronically (ESIGN Act)."
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = signedName,
                onValueChange = { signedName = it },
                label = { Text("Type your full legal name to sign") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // Trailing spacer so the last field isn't flush against the
            // bottom bar when the user scrolls all the way down.
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AgreementBody(onOpenFull: () -> Unit) {
    Text("STYLIST INDEPENDENT CONTRACTOR AGREEMENT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    val body = listOf(
        "1. Independent Contractor Status. You are an independent contractor, not an employee, partner, or agent of RefreshMe App LLC. You set your own schedule, use your own tools, and may work for other platforms.",
        "2. Licensing. You hold a current, unrestricted cosmetology or barber license. You\u2019ll notify RefreshMe immediately if your license is suspended or revoked.",
        "3. Platform Fee. Customers pay the full service price. RefreshMe retains a 10% platform fee. Tips are passed through 100%. Stripe handles payouts.",
        "4. Your Responsibilities. You supply your own tools and products, provide services in a safe and sanitary manner, carry general and professional liability insurance, and if you do mobile service, carry auto insurance for business use.",
        "5. Prohibited Conduct. No off-platform solicitation of RefreshMe customers for 12 months. No harassment, intoxication on the job, or weapons during services. No misrepresenting credentials.",
        "6. Verification. You consent to identity, license, and background checks via RefreshMe\u2019s verification providers.",
        "7. Indemnification. You will indemnify RefreshMe for claims arising out of services you perform, including injuries, reactions, and property damage.",
        "8. Limitation of Liability. RefreshMe\u2019s liability to you is capped at the platform fees retained in the prior 3 months.",
        "9. Arbitration & Class Waiver. Disputes go to binding AAA arbitration conducted virtually or in the contractor\u2019s home state, under AAA\u2019s commercial arbitration rules. No class actions.",
        "10. Governing Law. North Carolina, where RefreshMe App LLC is organized.",
        "11. Taxes. You\u2019re responsible for your own self-employment taxes. RefreshMe issues 1099-NEC/1099-K as applicable.",
        "12. Termination. Either party may terminate for any reason. Suspension is at RefreshMe\u2019s discretion for safety, fraud, license loss, or material breach."
    )
    body.forEach {
        Text(it, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onOpenFull) {
        Text("Read the full agreement on the web", fontSize = 12.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "\u2014 End of summary. Sign below. \u2014",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
