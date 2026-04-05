package com.refreshme.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RoleSelectScreen(
    isLoading: Boolean = false,
    onCustomerSelected: () -> Unit,
    onStylistSelected: () -> Unit,
    onSalonOwnerSelected: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Setting up your account...")
        } else {
            Button(onClick = onCustomerSelected, enabled = !isLoading) {
                Text("Find a stylist now")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onStylistSelected, enabled = !isLoading) {
                Text("Get clients instantly")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSalonOwnerSelected, enabled = !isLoading) {
                Text("Manage my salon")
            }
        }
    }
}