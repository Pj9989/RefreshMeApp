package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.R
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StylistHomeFragment : Fragment() {

    private val viewModel: StylistHomeViewModel by viewModels()
    
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (auth.currentUser == null) {
            try {
                findNavController().navigate(R.id.action_global_dashboardFragment)
                return null 
            } catch (e: Exception) {
                // Fallback handled by Activity AuthListener
            }
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val isOnline by viewModel.isOnline.collectAsState()
                        val stylistData by viewModel.stylist.collectAsState()

                        StylistHomeScreen(
                            stylistName = stylistData?.name ?: "Stylist",
                            profileUrl = stylistData?.displayImageUrl,
                            isOnline = isOnline,
                            onToggleOnline = { viewModel.toggleOnlineStatus() },
                            todayEarnings = "$120",
                            nextAppointment = "2:30 PM • Line-up + Beard • Marcus (5-stars)",
                            onManageServices = { 
                                findNavController().navigate(R.id.action_stylistDashboardFragment_to_manageServicesFragment)
                            },
                            onSetAvailability = { 
                                findNavController().navigate(R.id.action_stylistDashboardFragment_to_setAvailabilityFragment)
                            },
                            onViewPayouts = { 
                                findNavController().navigate(R.id.action_stylistDashboardFragment_to_payoutsEarningsFragment)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylistHomeScreen(
    stylistName: String = "Stylist",
    profileUrl: String? = null,
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stylistName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
            }
            
            val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
            AsyncImage(
                model = profileUrl,
                contentDescription = "Profile",
                placeholder = fallbackPainter,
                error = fallbackPainter,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        }

        // Status + Toggle
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isOnline) "You're Online" else "You're Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOnline) "Customers can request you now" else "Go online to get new bookings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Today's Earnings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(todayEarnings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onViewPayouts,
                    shape = RoundedCornerShape(12.dp)
                ) { Text("View Payout History") }
            }
        }

        // Next appointment
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Next Appointment", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(nextAppointment, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Quick actions
        Text(
            "Quick Actions", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionIconCard(
                title = "Services",
                icon = Icons.Default.ContentCut,
                modifier = Modifier.weight(1f),
                onClick = onManageServices
            )
            ActionIconCard(
                title = "Availability",
                icon = Icons.Default.Schedule,
                modifier = Modifier.weight(1f),
                onClick = onSetAvailability
            )
            ActionIconCard(
                title = "Payouts",
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f),
                onClick = onViewPayouts
            )
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ActionIconCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}