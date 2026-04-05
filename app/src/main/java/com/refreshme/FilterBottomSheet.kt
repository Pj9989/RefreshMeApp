package com.refreshme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    initialRating: Float,
    initialPrice: Float,
    initialIsMobile: Boolean,
    onDismiss: () -> Unit,
    onApplyFilters: (Float, Float, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var rating by remember { mutableStateOf(initialRating) }
    var price by remember { mutableStateOf(initialPrice) }
    var isMobile by remember { mutableStateOf(initialIsMobile) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    rating = 0f
                    price = 1000f
                    isMobile = false
                }) {
                    Text("Reset")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("Minimum Rating: ${"%.1f".format(rating)}")
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..5f,
                steps = 9
            )

            Spacer(modifier = Modifier.height(16.dp))

            val priceText = if (price >= 1000f) "Any" else "$${"%.0f".format(price)}"
            Text("Maximum Price: $priceText")
            Slider(
                value = price,
                onValueChange = { price = it },
                valueRange = 0f..1000f,
                steps = 19
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mobile Services Only", style = MaterialTheme.typography.bodyLarge)
                    Text("Only show stylists who come to you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isMobile,
                    onCheckedChange = { isMobile = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onApplyFilters(rating, price, isMobile) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
        }
    }
}