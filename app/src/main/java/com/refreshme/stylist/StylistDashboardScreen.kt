package com.refreshme.stylist

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.details.ReviewItem
import java.util.Locale

enum class StylistDashboardEvent {
    UpcomingAppointments,
    EarningsPayouts,
    ManageServices,
    ProfileVerification,
    Availability,
    Analytics,
    Waitlist
}

@Composable
fun StylistDashboardScreen(
    viewModel: StylistDashboardViewModel = viewModel(),
    trialStatus: String? = null,
    onNavigate: (StylistDashboardEvent) -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isMobile by viewModel.isMobile.collectAsState()
    val currentFlashDeal by viewModel.currentFlashDeal.collectAsState()
    val stylistName by viewModel.stylistName.collectAsState()
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val rating by viewModel.rating.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showFlashDealDialog by remember { mutableStateOf(false) }
    var showReviewsDialog by remember { mutableStateOf(false) }

    val accentColor = MaterialTheme.colorScheme.primary
    val flashColor = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = profileImageUrl ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Welcome back, $stylistName",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Dashboard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Surface(
                    color = if (isOnline) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isOnline) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Flash Deal Active Card
            AnimatedVisibility(
                visible = currentFlashDeal != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    color = flashColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, flashColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = flashColor)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active Flash Deal", color = flashColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(currentFlashDeal?.title ?: "", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { viewModel.clearFlashDeal() }) {
                            Text("STOP", color = flashColor)
                        }
                    }
                }
            }

            // Earnings Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Earnings", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Surface(
                            color = accentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Monthly",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        "$${String.format(Locale.US, "%.2f", stats.totalEarnings)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 1f)
                        heights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(h)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(accentColor, accentColor.copy(alpha = 0.3f))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick Stats Row 1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatSmallCard(
                    label = "Rating",
                    value = String.format(Locale.US, "%.1f", rating),
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFC107),
                    modifier = Modifier.weight(1f).clickable { showReviewsDialog = true }
                )
                StatSmallCard(
                    label = "Reviews",
                    value = reviews.size.toString(),
                    icon = Icons.AutoMirrored.Filled.Comment,
                    modifier = Modifier.weight(1f).clickable { showReviewsDialog = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Quick Stats Row 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatSmallCard(
                    label = "Pending",
                    value = stats.pendingRequests.toString(),
                    icon = Icons.Default.PendingActions,
                    modifier = Modifier.weight(1f)
                )
                StatSmallCard(
                    label = "Upcoming",
                    value = stats.upcomingBookings.toString(),
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Controls
            DashboardCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = accentColor)
                            Spacer(Modifier.width(12.dp))
                            Text("Available Now", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { viewModel.toggleOnlineStatus(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor)
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF64B5F6))
                            Spacer(Modifier.width(12.dp))
                            Text("Mobile Services", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = isMobile,
                            onCheckedChange = { viewModel.toggleMobileStatus(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF64B5F6))
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Navigation Options
            MenuRow("Manage Services", Icons.Default.ContentCut) { onNavigate(StylistDashboardEvent.ManageServices) }
            
            MenuRow("Cancellation Waitlist", Icons.Default.HourglassTop) { onNavigate(StylistDashboardEvent.Waitlist) }
            
            MenuRow("Create Flash Deal", Icons.Default.ElectricBolt) {
                showFlashDealDialog = true
            }

            MenuRow("Share My Profile", Icons.Default.Share) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Book me on RefreshMe")
                    putExtra(Intent.EXTRA_TEXT, "Book your next appointment with me on RefreshMe!\n\nhttps://refreshme-74f79.web.app/stylist/$uid")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share My Profile"))
            }

            MenuRow("Availability Settings", Icons.Default.Schedule) { onNavigate(StylistDashboardEvent.Availability) }
            MenuRow("Earnings & Payouts", Icons.Default.Payments) { onNavigate(StylistDashboardEvent.EarningsPayouts) }
            
            Spacer(Modifier.height(40.dp))
        }

        if (showFlashDealDialog) {
            FlashDealDialog(
                onDismiss = { showFlashDealDialog = false },
                onConfirm = { title, discount, hours ->
                    viewModel.createFlashDeal(title, discount, hours)
                    showFlashDealDialog = false
                }
            )
        }

        if (showReviewsDialog) {
            ReviewsDialog(
                reviews = reviews,
                onDismiss = { showReviewsDialog = false }
            )
        }
    }
}

@Composable
fun ReviewsDialog(
    reviews: List<com.refreshme.data.Review>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("My Reviews") },
        text = {
            if (reviews.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No reviews yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reviews) { review ->
                        ReviewItem(review)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun FlashDealDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("20") }
    var duration by remember { mutableStateOf("2") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("New Flash Deal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Broadcast a limited-time offer to nearby customers.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deal Title (e.g. Rainy Day Special)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { if (it.length <= 2) discount = it },
                        label = { Text("Discount %") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.length <= 1) duration = it },
                        label = { Text("Hours") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = discount.toIntOrNull() ?: 20
                    val h = duration.toIntOrNull() ?: 2
                    if (title.isNotBlank()) {
                        onConfirm(title, d, h)
                    }
                }
            ) {
                Text("Go Live")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

@Composable
fun StatSmallCard(
    label: String, 
    value: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
fun MenuRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DashboardCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        content = content
    )
}