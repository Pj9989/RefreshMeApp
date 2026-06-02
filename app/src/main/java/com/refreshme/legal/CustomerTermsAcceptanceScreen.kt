package com.refreshme.legal

import android.content.Intent
import android.net.Uri
import android.util.Log
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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.launch

private const val TAG_CONSENT = "CustomerConsent"

/**
 * Signup-flow click-wrap screen. Shown AFTER email/phone verification but
 * BEFORE the user can browse stylists or book. Gathers:
 *   - 18+ age confirmation
 *   - Terms of Service acceptance
 *   - Privacy Policy acknowledgment
 *
 * Writes TWO separate acceptance records (ToS and Privacy) so audit logs
 * can distinguish which version the user agreed to for each.
 */

@Composable
fun CustomerTermsAcceptanceScreen(
    onAccepted: () -> Unit,
    onCancel: (() -> Unit)? = null,
    repository: LegalRepository = remember { LegalRepository() },
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var is18 by remember { mutableStateOf(false) }
    var tosAgreed by remember { mutableStateOf(false) }
    var privacyAgreed by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSubmit = is18 && tosAgreed && privacyAgreed && !submitting

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "Before we get started",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "A few quick confirmations to use RefreshMe.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            LegalCheckRow(
                checked = is18,
                onCheckedChange = { is18 = it },
                text = "I confirm I am at least 18 years old (or the age of " +
                    "majority in my state)."
            )
            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    LegalCheckRow(
                        checked = tosAgreed,
                        onCheckedChange = { tosAgreed = it },
                        text = "I have read and accept the RefreshMe Terms of " +
                            "Service (v ${LegalVersions.CUSTOMER_TOS})."
                    )
                    TextButton(onClick = { openUrl(LegalUrls.TERMS) }) {
                        Text("Open Terms of Service")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    LegalCheckRow(
                        checked = privacyAgreed,
                        onCheckedChange = { privacyAgreed = it },
                        text = "I acknowledge the RefreshMe Privacy Policy " +
                            "(v ${LegalVersions.PRIVACY_POLICY})."
                    )
                    TextButton(onClick = { openUrl(LegalUrls.PRIVACY) }) {
                        Text("Open Privacy Policy")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                enabled = canSubmit,
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        // Pre-flight: make sure we actually have an authenticated user
                        // before even trying to write. Previously this would throw
                        // IllegalStateException from requireUid() which got silently
                        // mapped to a generic "check your connection" message.
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        if (currentUser == null) {
                            Log.e(TAG_CONSENT, "Consent tap with null currentUser")
                            runCatching {
                                FirebaseCrashlytics.getInstance()
                                    .recordException(IllegalStateException("Consent tap with null currentUser"))
                            }
                            error = "Your session expired. Please sign in again."
                            submitting = false
                            return@launch
                        }

                        val docsToAccept = listOf(
                            LegalDocKeys.CUSTOMER_TOS to LegalVersions.CUSTOMER_TOS,
                            LegalDocKeys.PRIVACY_POLICY to LegalVersions.PRIVACY_POLICY,
                        )

                        for ((docKey, docVersion) in docsToAccept) {
                            try {
                                repository.recordAcceptance(
                                    docKey = docKey,
                                    docVersion = docVersion,
                                )
                            } catch (e: Exception) {
                                Log.e(TAG_CONSENT, "recordAcceptance failed for $docKey", e)
                                // Report every failure to Crashlytics so we get production
                                // data across all users, not just one tester's screenshot.
                                runCatching {
                                    FirebaseCrashlytics.getInstance().apply {
                                        setCustomKey("consent_doc_key", docKey)
                                        setCustomKey("consent_doc_version", docVersion)
                                        setCustomKey("consent_uid", currentUser.uid)
                                        recordException(e)
                                    }
                                }

                                val fsCode = (e as? FirebaseFirestoreException)?.code
                                error = when {
                                    fsCode == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                        "Server rejected your acceptance. If this keeps happening please contact support."
                                    fsCode == FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                                        "Your session expired. Please sign in again."
                                    fsCode == FirebaseFirestoreException.Code.UNAVAILABLE ->
                                        "Couldn't reach the server. Check your connection and try again."
                                    e is FirebaseNetworkException ->
                                        "No internet connection. Please reconnect and try again."
                                    e is IllegalStateException ->
                                        "Your session expired. Please sign in again."
                                    else ->
                                        "Could not record your acceptance: ${e.javaClass.simpleName}: ${e.message ?: "(no message)"}"
                                }
                                submitting = false
                                return@launch
                            }
                        }
                        onAccepted()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            }
            if (onCancel != null) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Cancel") }
            }
        }
    }
}
