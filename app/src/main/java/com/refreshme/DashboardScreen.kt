package com.refreshme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshme.data.Stylist


@Composable
fun DashboardScreen(
    onFindStylist: () -> Unit,
    onMyBookings: () -> Unit,
    onStylistClick: (Stylist) -> Unit
) {
    // Using a softer background color as per the UI style guide
    val backgroundColor = Color(0xFF1C1B1F)
    val primaryButtonColor = Color(0xFFD0BCFF)
    val secondaryButtonColor = Color(0xFF4A4458)
    val textColor = Color.White

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome back!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ready for your next refresh?",
                fontSize = 18.sp,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Primary CTA: "Find a Stylist"
            Button(
                onClick = onFindStylist,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = "Find a Stylist",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Secondary CTA: "My Bookings"
            Button(
                onClick = onMyBookings,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = secondaryButtonColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "My Bookings",
                    fontSize = 16.sp,
                    color = textColor
                )
            }
        }
    }
}