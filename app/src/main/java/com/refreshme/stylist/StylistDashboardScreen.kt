package com.refreshme.stylist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StylistDashboardScreen() {
    var isAvailable by remember { mutableStateOf(true) }

    // UI Style: "More structured, tool-based layout", "Stronger contrast, sharper cards"
    val backgroundColor = Color(0xFF121212) // Darker background for a professional feel
    val cardBackgroundColor = Color(0xFF1E1E1E)
    val primaryColor = Color(0xFFBB86FC)
    val textColor = Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                "Stylist Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Status Toggle: "Available Now / Offline (very prominent)"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isAvailable) "Available for Bookings" else "You are Offline",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Placeholder cards for other features
            DashboardCard(title = "Upcoming Appointments", content = "You have 3 appointments today.")
            DashboardCard(title = "Earnings & Payouts", content = "View your recent activity.")
            DashboardCard(title = "Manage Your Services", content = "Update your offerings and prices.")
            DashboardCard(title = "Profile & Verification", content = "Your profile is 80% complete.")
        }
    }
}

@Composable
fun DashboardCard(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp), // Increased spacing between cards (8-12dp more)
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold, // Made headings slightly bolder
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview
@Composable
fun PreviewStylistDashboard() {
    StylistDashboardScreen()
}