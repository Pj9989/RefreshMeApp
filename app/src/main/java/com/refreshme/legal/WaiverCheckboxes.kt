package com.refreshme.legal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small reusable row — checkbox + label. Keeps the waiver dialog readable. */
@Composable
fun LegalCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/** The three required confirmations for the at-home waiver. */
@Composable
fun WaiverCheckboxes(
    acknowledged: Boolean,
    onAcknowledgedChange: (Boolean) -> Unit,
    assumptionOfRisk: Boolean,
    onAssumptionChange: (Boolean) -> Unit,
    allergiesDisclosed: Boolean,
    onAllergiesChange: (Boolean) -> Unit,
) {
    Column {
        LegalCheckRow(
            checked = acknowledged,
            onCheckedChange = onAcknowledgedChange,
            text = "I have read this acknowledgment and I am 18+."
        )
        LegalCheckRow(
            checked = assumptionOfRisk,
            onCheckedChange = onAssumptionChange,
            text = "I accept the assumption of risk and release of RefreshMe."
        )
        LegalCheckRow(
            checked = allergiesDisclosed,
            onCheckedChange = onAllergiesChange,
            text = "I will disclose known allergies/sensitivities to my stylist."
        )
    }
}
