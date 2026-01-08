package com.refreshme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.refreshme.data.ServiceType
import com.refreshme.data.Stylist

@Composable
fun StylistListItem(
    stylist: Stylist,
    onClick: () -> Unit
) {
    // A modern, card-based design with a gradient background
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4D3D2B), Color(0xFF2C251C))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.background(gradient)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stylist Image
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(stylist.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Stylist profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Stylist Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stylist.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = stylist.specialty ?: "Specialty not available",
                        fontSize = 14.sp,
                        color = Color.LightGray
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107), // A gold color for the star
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stylist.rating.toString(),
                            color = Color.White
                        )
                    }
                }
            }

            // Service Type Icon
            val serviceIcon = when (stylist.serviceType) {
                ServiceType.AT_HOME -> R.drawable.ic_house
                ServiceType.IN_SALON -> R.drawable.ic_store
                ServiceType.ALL_HOURS -> R.drawable.ic_24_7
            }

            Icon(
                painter = painterResource(id = serviceIcon),
                contentDescription = "Service Type",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            )
        }
    }
}