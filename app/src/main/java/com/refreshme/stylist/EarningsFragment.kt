package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.ui.theme.RefreshMeTheme

data class EarningEntry(
    val id: String = "",
    val customerName: String = "",
    val serviceName: String = "",
    val amount: Double = 0.0,
    val date: String = ""
)

/**
 * EarningsFragment — shows the stylist's completed bookings and total earnings.
 */
class EarningsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    EarningsScreen()
                }
            }
        }
    }
}

@Composable
fun EarningsScreen() {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    var earnings by remember { mutableStateOf<List<EarningEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalEarnings by remember { mutableStateOf(0.0) }

    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid
        val registration = if (uid != null) {
            firestore.collection("bookings")
                .whereEqualTo("stylistId", uid)
                .whereEqualTo("status", "COMPLETED")
                .addSnapshotListener { snapshot, _ ->
                    isLoading = false
                    val entries = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val ts = doc.getTimestamp("startTime")
                            val dateStr = ts?.toDate()?.let {
                                java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(it)
                            } ?: ""
                            EarningEntry(
                                id = doc.id,
                                customerName = doc.getString("customerName") ?: "Unknown",
                                serviceName = doc.getString("serviceName") ?: "",
                                amount = doc.getDouble("price") ?: 0.0,
                                date = dateStr
                            )
                        } catch (e: Exception) { null }
                    } ?: emptyList()
                    earnings = entries
                    totalEarnings = entries.sumOf { it.amount }
                }
        } else null

        onDispose { registration?.remove() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Earnings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // Total earnings card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total Earned",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", totalEarnings)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${earnings.size} completed bookings",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "History",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                earnings.isEmpty() -> {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No completed bookings yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(earnings) { entry ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.customerName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${entry.serviceName}  •  ${entry.date}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Text(
                                        text = "+$${String.format("%.2f", entry.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
