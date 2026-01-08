package com.refreshme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    onDismiss: () -> Unit,
    onApplyFilters: (Float, Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var rating by remember { mutableStateOf(0f) }
    var price by remember { mutableStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Filter by Rating")
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..5f,
                steps = 9
            )
            Text(text = "%.1f".format(rating))

            Spacer(modifier = Modifier.height(16.dp))

            Text("Filter by Price")
            Slider(
                value = price,
                onValueChange = { price = it },
                valueRange = 0f..500f,
                steps = 10
            )
            Text(text = "$%.0f".format(price))

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onApplyFilters(rating, price) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
        }
    }
}