package com.refreshme.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Short disclosure for bookings that include chemical services (color,
 * bleach, relaxer, perm, keratin). Shown inline during booking before
 * AtHomeWaiverDialog (or instead of it, for shop-based chemical bookings).
 *
 * Detection helper: [serviceRequiresAllergyDisclosure] (below) — call it
 * with the resolved service name; if true, surface this dialog.
 */
@Composable
fun AllergyDisclosureDialog(
    onAccepted: (disclosureVersion: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var patchTestAck by remember { mutableStateOf(false) }
    var disclosureAck by remember { mutableStateOf(false) }
    val enabled = patchTestAck && disclosureAck

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Chemical Service Disclosure",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Chemical services (color, bleach, relaxer, perm, keratin) " +
                        "carry risks of scalp irritation, chemical burns, hair " +
                        "damage, and allergic reactions. Industry best practice " +
                        "is a patch test 24\u201348 hours before the service.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                LegalCheckRow(
                    checked = disclosureAck,
                    onCheckedChange = { disclosureAck = it },
                    text = "I will disclose known allergies and recent " +
                        "chemical services to my stylist before the service."
                )
                LegalCheckRow(
                    checked = patchTestAck,
                    onCheckedChange = { patchTestAck = it },
                    text = "I understand a patch test may be recommended; if " +
                        "I decline it, I assume the risk of reaction."
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Version ${LegalVersions.ALLERGY_DISCLOSURE}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = enabled,
                onClick = { onAccepted(LegalVersions.ALLERGY_DISCLOSURE) }
            ) { Text("I Agree") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Returns true if the service name looks like a chemical service. */
fun serviceRequiresAllergyDisclosure(serviceName: String?): Boolean {
    if (serviceName.isNullOrBlank()) return false
    val s = serviceName.lowercase()
    return listOf(
        "color", "dye", "bleach", "highlight", "lowlight", "balayage",
        "ombre", "relaxer", "perm", "keratin", "toner", "tint"
    ).any { it in s }
}
