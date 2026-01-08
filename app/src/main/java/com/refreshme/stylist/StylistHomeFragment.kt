package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.refreshme.ui.theme.RefreshMeTheme

class StylistHomeFragment : Fragment() {

    private val viewModel: StylistHomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    val isOnline by viewModel.isOnline.collectAsState()

                    StylistHomeScreen(
                        stylistName = "PJ",
                        isOnline = isOnline,
                        onToggleOnline = { viewModel.toggleOnlineStatus() },
                        todayEarnings = "$120",
                        nextAppointment = "2:30 PM • Line-up + Beard • Marcus (5-stars)",
                        onManageServices = { /* nav to ManageServices */ },
                        onSetAvailability = { /* nav to Availability */ },
                        onViewPayouts = { /* nav to Earnings */ }
                    )
                }
            }
        }
    }
}

@Composable
fun StylistHomeScreen(
    stylistName: String = "Stylist",
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    todayEarnings: String = "$0",
    nextAppointment: String = "No upcoming appointments",
    onManageServices: () -> Unit,
    onSetAvailability: () -> Unit,
    onViewPayouts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Welcome, $stylistName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        // Status + Toggle
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isOnline) "You're Online" else "You're Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isOnline) "Customers can request you now" else "Go online to get new bookings",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = isOnline,
                    onCheckedChange = { onToggleOnline() }
                )
            }
        }

        // Today's Earnings
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Earnings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(todayEarnings, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onViewPayouts) { Text("View payouts") }
            }
        }

        // Next appointment
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Next Appointment", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(nextAppointment, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Quick actions
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = onManageServices,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Manage Services & Prices") }
                Button(
                    onClick = onSetAvailability,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Set Availability") }
                OutlinedButton(
                    onClick = onViewPayouts,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Payouts & Earnings") }
            }
        }
    }
}