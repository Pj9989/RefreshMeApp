package com.refreshme.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties


/**
 * Modal waiver shown BEFORE [NewBookingViewModel.createBooking] when the
 * customer has toggled "At Home Service" on.
 *
 * Returns the waiver version via [onAccepted] so the caller can persist it on
 * the booking doc as `waiverAcceptedVersion`.
 */
@Composable
fun AtHomeWaiverDialog(
    onAccepted: (waiverVersion: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var acknowledged by remember { mutableStateOf(false) }
    var assumptionOfRisk by remember { mutableStateOf(false) }
    var allergiesDisclosed by remember { mutableStateOf(false) }
    val allChecked = acknowledged && assumptionOfRisk && allergiesDisclosed

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    "At-Home Service Acknowledgment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Required for at-home / mobile bookings",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                WaiverBody(Modifier.weight(1f).verticalScroll(rememberScrollState()))
                Spacer(Modifier.height(12.dp))
                WaiverCheckboxes(
                    acknowledged = acknowledged,
                    onAcknowledgedChange = { acknowledged = it },
                    assumptionOfRisk = assumptionOfRisk,
                    onAssumptionChange = { assumptionOfRisk = it },
                    allergiesDisclosed = allergiesDisclosed,
                    onAllergiesChange = { allergiesDisclosed = it },
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { onAccepted(LegalVersions.AT_HOME_LIABILITY_WAIVER) },
                        enabled = allChecked,
                        modifier = Modifier.weight(1f)
                    ) { Text("I Agree") }
                }
            }
        }
    }
}


@Composable
private fun WaiverBody(modifier: Modifier = Modifier) {
    Column(modifier) {
        WaiverSection(
            title = "1. RefreshMe is a platform, not a service provider",
            body = "Hair services are performed by independent, licensed " +
                "stylists \u2014 not by RefreshMe App LLC. RefreshMe connects you " +
                "with a stylist but is not responsible for how they perform the " +
                "service."
        )
        WaiverSection(
            title = "2. Inherent risks of at-home service",
            body = "I understand risks include, without limitation: cuts from " +
                "scissors, clippers, or razors; chemical burns or allergic " +
                "reactions; hair breakage or unintended color results; burns " +
                "from hot tools; slip/trip hazards; and property damage from " +
                "product spills or stains."
        )
        WaiverSection(
            title = "3. Allergies and patch tests",
            body = "For any chemical service I will disclose known allergies or " +
                "sensitivities to my stylist before the service begins. If I " +
                "decline a recommended patch test, I assume responsibility for " +
                "any resulting reaction."
        )
        WaiverSection(
            title = "4. My home environment",
            body = "I agree to provide a safe, well-lit workspace with access " +
                "to water and electricity, to protect my own flooring and " +
                "furniture, to keep pets and children out of the work area, " +
                "and to treat my stylist professionally. My stylist may leave " +
                "if they feel unsafe."
        )
        WaiverSection(
            title = "5. Release of RefreshMe",
            body = "To the maximum extent permitted by law I release RefreshMe " +
                "App LLC and its officers, members, employees, and agents from " +
                "any claim arising out of the service, the stylist\u2019s acts or " +
                "omissions, or my interaction with the stylist. This release " +
                "does not waive claims for RefreshMe\u2019s own gross negligence " +
                "or intentional misconduct."
        )
        WaiverSection(
            title = "6. Disputes",
            body = "Disputes are governed by the RefreshMe Terms of Service, " +
                "including binding arbitration and class-action waiver. " +
                "Contact admin@refreshmeapp.com within 48 hours of the service."
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "By checking the boxes below and tapping I Agree, I confirm I am " +
                "at least 18 years old, I have read this acknowledgment, and I " +
                "am signing it voluntarily. Version ${LegalVersions.AT_HOME_LIABILITY_WAIVER}.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WaiverSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
