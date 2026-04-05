package com.refreshme.stylist

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*

// Data model for transactions
data class TransactionItem(
    val id: String,
    val title: String, 
    val amount: Double, 
    val date: String, 
    val isPayout: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutsEarningsScreen(
    onBack: () -> Unit,
    viewModel: StylistDashboardViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current
    
    // In a real app, these would come from the ViewModel based on Firestore data
    val weeklyData = listOf(150f, 320f, 450f, 280f, 600f, 890f, 420f)
    val transactions = listOf(
        TransactionItem("1", "Payout to Bank", -450.00, "Feb 12", true),
        TransactionItem("2", "Booking - Marcus J.", 55.00, "Feb 11", false),
        TransactionItem("3", "Booking - Sarah L.", 120.00, "Feb 11", false),
        TransactionItem("4", "Booking - David K.", 35.00, "Feb 10", false),
        TransactionItem("5", "Booking - Emily R.", 85.00, "Feb 09", false)
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revenue & Payouts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Total Balance Card
            item {
                BalanceHeroCard(
                    amount = stats.totalEarnings,
                    onRequestPayout = {
                        viewModel.requestPayout()
                        Toast.makeText(context, "Payout request submitted to processing queue!", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // 2. Revenue Chart Section
            item {
                Text(
                    text = "Weekly Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                SimpleRevenueChart(weeklyData)
            }

            // 3. Service Popularity
            item {
                Text(
                    text = "Top Performing Services",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ServicePopularityRow(name = "Classic Fade", percentage = 0.75f, color = Color(0xFFBB86FC))
                    ServicePopularityRow(name = "Royal Shave", percentage = 0.45f, color = Color(0xFF03DAC5))
                    ServicePopularityRow(name = "Beard Sculpt", percentage = 0.30f, color = Color(0xFF4CAF50))
                }
            }

            // 4. Recent Transactions
            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
            }

            items(transactions) { transaction ->
                TransactionRow(transaction)
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun BalanceHeroCard(amount: Double, onRequestPayout: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Earnings", 
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$${String.format(Locale.US, "%,.2f", amount)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRequestPayout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, 
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request Payout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimpleRevenueChart(data: List<Float>) {
    val max = data.maxOrNull() ?: 1f
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .height(150.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { value ->
                val heightFraction = if (max > 0) value / max else 0f
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary, 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun ServicePopularityRow(name: String, percentage: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${(percentage * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        )
    }
}

@Composable
fun TransactionRow(item: TransactionItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isPayout) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) 
                        else Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isPayout) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (item.isPayout) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                Text(item.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = (if (item.isPayout) "-" else "+") + "$${String.format(Locale.US, "%.2f", Math.abs(item.amount))}", 
                fontWeight = FontWeight.Bold, 
                color = if (item.isPayout) MaterialTheme.colorScheme.onSurface else Color(0xFF4CAF50),
                fontSize = 15.sp
            )
        }
    }
}