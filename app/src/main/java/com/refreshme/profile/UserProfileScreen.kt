package com.refreshme.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onViewBookings: () -> Unit,
    onViewSavedStylists: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    val userName = userProfile?.name ?: "User"
    val userEmail = userProfile?.email ?: "No email"
    val userPhotoUrl = userProfile?.displayImageUrl
    val userRating = userProfile?.rating ?: 0.0
    val userReviewCount = userProfile?.reviewCount ?: 0L
    val refreshPoints = userProfile?.refreshPoints ?: 0L

    val context = LocalContext.current
    val privacyPolicyUrl = "https://refreshme-74f79.web.app/privacy.html"
    val helpCenterUrl = "https://refreshme-74f79.web.app/help.html"
    val safetyCenterUrl = "https://refreshme-74f79.web.app/safety-center.html"
    val aboutUrl = "https://refreshme-74f79.web.app/"

    var showDeleteDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
    
    fun onReferFriend() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join RefreshMe")
            putExtra(Intent.EXTRA_TEXT, "Hey! Check out RefreshMe, it's the easiest way to find and book top stylists in your area. Use my link to join: https://refreshme-74f79.web.app/invite")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Refer a Friend"))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                )
            }
    ) { padding ->
        LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Profile Image
                val fallbackPainter = rememberVectorPainter(Icons.Default.Person)
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile Picture",
                        placeholder = fallbackPainter,
                        error = fallbackPainter,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                            modifier = Modifier.size(32.dp).clickable { onEditProfile() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = 4.dp
                    ) {
                        Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.padding(6.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = userEmail, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                if (userReviewCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", userRating),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " ($userReviewCount reviews)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                            modifier = Modifier.weight(1f).height(100.dp).clickable { onViewBookings() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                    ) {
                        Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bookings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Surface(
                            modifier = Modifier.weight(1f).height(100.dp).clickable { onViewSavedStylists() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 2.dp
                    ) {
                        Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Saved Stylists", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Refresh Points Card - BUG FIX 7: Prevent text wrapping mid-word
                Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF3E0),
                        tonalElevation = 2.dp
                ) {
                    Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Refresh Points", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 18.sp, 
                                color = Color(0xFFE65100),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("Earn points on every cut · $refreshPoints pt", fontSize = 12.sp, color = Color(0xFFEF6C00))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                ProfileSectionTitle("Trust & Safety")
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Safety Guarantee", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("RefreshMe Safety Guarantee", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("All Stylists on our platform have been verified and passed basic identity checks.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
                ProfileMenuItem(Icons.Default.VerifiedUser, "Safety Center", "Guidelines, tips, and reporting", { openUrl(safetyCenterUrl) })
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                ProfileSectionTitle("Rewards & Growth")
                ProfileMenuItem(Icons.Default.CardGiftcard, "Refer a Friend", "Get 500 points when they book their first cut", ::onReferFriend)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                ProfileSectionTitle("Support & Legal")
                ProfileMenuItem(Icons.Default.Help, "Help Center", "FAQs and support", { openUrl(helpCenterUrl) })
                ProfileMenuItem(Icons.Default.Policy, "Privacy Policy", "Review our data usage policy", { openUrl(privacyPolicyUrl) })
                ProfileMenuItem(Icons.Default.Info, "About RefreshMe", "Version 3.0.0", { openUrl(aboutUrl) })
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Account", color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ProfileMenuItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
) {
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
        )
    }
}