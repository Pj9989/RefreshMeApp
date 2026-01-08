package com.refreshme.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.refreshme.StylistProfileViewModel
import com.refreshme.data.Service

@Composable
fun StylistDetailScreen(
    stylistId: String,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    onChatClick: (String) -> Unit
) {
    val viewModel: StylistProfileViewModel = viewModel()
    LaunchedEffect(stylistId) {
        viewModel.fetchStylist(stylistId)
    }
    val stylist by viewModel.stylist.collectAsState()
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (stylist != null) {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onChatClick(stylistId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Chat")
                        }
                        Button(
                            onClick = { onBookClick(stylistId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Book Now")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (stylist == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val s = stylist!!
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                // Header with Back Button
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(s.profileImageUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                }

                // Stylist Info
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(s.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(s.specialty ?: "", fontSize = 16.sp, color = Color.Gray)
                        OnlineOfflineStatus(isOnline = s.isOnline)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                            Text(" ${s.rating} • Verified Stylist", fontWeight = FontWeight.SemiBold)
                        }
                        Text(s.bio ?: "", fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // Portfolio Gallery
                if (s.portfolioImages?.isNotEmpty() == true) {
                    item {
                        Text("Portfolio", modifier = Modifier.padding(horizontal = 20.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        LazyRow(contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(s.portfolioImages!!.values.toList()) { img ->
                                Image(
                                    painter = rememberAsyncImagePainter(img),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Map Section
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Location", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(s.address ?: "", fontSize = 14.sp, color = Color.Gray)
                        Spacer(Modifier.height(12.dp))

                        val stylistLocation = LatLng(s.location?.latitude ?: 0.0, s.location?.longitude ?: 0.0)
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(stylistLocation, 15f)
                        }

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = cameraPositionState
                            ) {
                                Marker(
                                    state = MarkerState(position = stylistLocation),
                                    title = s.name,
                                    snippet = s.specialty
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=${s.location?.latitude},${s.location?.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Get Directions")
                        }
                    }
                }

                // Services List
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Services", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        s.services?.forEach { service ->
                            ListItem(
                                headlineContent = { Text(service.name, fontWeight = FontWeight.SemiBold) },
                                supportingContent = { Text("${service.duration} mins") },
                                trailingContent = { Text("$${service.price}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineOfflineStatus(isOnline: Boolean) {
    val backgroundColor = if (isOnline) Color(0xFFD1FAE5) else Color(0xFFF3F4F6)
    val textColor = if (isOnline) Color(0xFF065F46) else Color(0xFF4B5563)
    val subtext = if (isOnline) "Accepting new bookings" else "You won't receive bookings"
    val text = if (isOnline) "ONLINE" else "OFFLINE"
    val glowColor = if (isOnline) Color(0xFF10B981) else Color.Transparent

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .then(
                    if (isOnline) {
                        Modifier.drawBehind {
                            drawRoundRect(
                                color = glowColor,
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                style = Stroke(width = 4.dp.toPx()),
                                alpha = 0.5f
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(text, color = textColor, fontWeight = FontWeight.Bold)
        }
        Text(subtext, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}