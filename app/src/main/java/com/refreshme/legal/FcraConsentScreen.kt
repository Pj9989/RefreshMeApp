package com.refreshme.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * FCRA-compliant Background Check Disclosure & Consent screen.
 *
 * IMPORTANT (legal): The federal Fair Credit Reporting Act (15 USC 1681b(b))
 * requires the disclosure and the authorization to be in a standalone
 * document \u2014 not bundled with other terms. Keep this screen dedicated to
 * the FCRA disclosure. Do NOT add other acknowledgments to this screen.
 *
 * Shown during stylist onboarding AFTER the Contractor Agreement but BEFORE
 * the stylist\u2019s first booking. Also shown again on version bumps or when
 * RefreshMe needs to re-verify.
 */

@Composable
fun FcraConsentScreen(
    onAccepted: () -> Unit,
    onCancel: () -> Unit,
    repository: LegalRepository = remember { LegalRepository() },
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var consent by remember { mutableStateOf(false) }
    var signedName by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = consent && signedName.trim().length >= 3 && !submitting

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "Background Check Disclosure",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Version ${LegalVersions.FCRA_BACKGROUND_CHECK_CONSENT}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "DISCLOSURE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "RefreshMe App LLC (\u201CRefreshMe\u201D) may obtain a consumer " +
                            "report and/or investigative consumer report about " +
                            "you from a consumer reporting agency (\u201CCRA\u201D) as part " +
                            "of the stylist onboarding process, and from time to " +
                            "time thereafter for re-verification. The report may " +
                            "include: identity verification, cosmetology/barber " +
                            "license status, criminal history (county, state, " +
                            "and federal), sex offender registry, address history, " +
                            "driving record (for mobile stylists), and prior " +
                            "marketplace bans related to safety.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Under the FCRA (15 U.S.C. \u00a7 1681 et seq.) you have the " +
                            "right to receive a copy of any consumer report " +
                            "obtained about you, to dispute its accuracy, and to " +
                            "receive a \u201CSummary of Your Rights Under the Fair " +
                            "Credit Reporting Act\u201D from the CFPB.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(LegalUrls.FCRA_SUMMARY_OF_RIGHTS)))
                    }) {
                        Text("Read Summary of Rights", fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("AUTHORIZATION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "By checking the box and typing my full legal name below, I " +
                    "authorize RefreshMe and its designated CRA to obtain and " +
                    "review the information described above. This authorization " +
                    "is ongoing throughout my participation on the platform. " +
                    "I understand that an adverse report or license loss may " +
                    "result in removal from the platform.",
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            LegalCheckRow(
                checked = consent,
                onCheckedChange = { consent = it },
                text = "I authorize this background check and re-verification."
            )

            OutlinedTextField(
                value = signedName,
                onValueChange = { signedName = it },
                label = { Text("Type your full legal name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                    docKey = LegalDocKeys.FCRA_BACKGROUND_CHECK_CONSENT,
                                    docVersion = LegalVersions.FCRA_BACKGROUND_CHECK_CONSENT,
                                    signedName = signedName.trim(),
                                )
                                onAccepted()
                            } catch (e: Exception) {
                                error = "Could not save your authorization. " +
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
                        Text("Authorize", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
