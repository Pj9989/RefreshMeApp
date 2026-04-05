package com.refreshme.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.refreshme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onViewBookings: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()

    val userName = userProfile?.name ?: "User"
    val userEmail = userProfile?.email ?: "No email"
    val userPhotoUrl = userProfile?.displayImageUrl

    val context = LocalContext.current
    val privacyPolicyUrl = "https://refreshme-74f79.web.app/privacy.html?v=${System.currentTimeMillis()}"
    val helpCenterUrl = "https://refreshme-74f79.web.app/help.html"
    val aboutUrl = "https://refreshme-74f79.web.app/"

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
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
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = userPhotoUrl ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile Picture",
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
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                ProfileSectionTitle("Account Settings")
                ProfileMenuItem(Icons.Default.Person, "Edit Profile", "Update your personal info", onEditProfile)
                ProfileMenuItem(Icons.Default.DateRange, "My Bookings", "View your appointment history", onViewBookings)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                ProfileSectionTitle("Support & Legal")
                ProfileMenuItem(Icons.Default.Help, "Help Center", "FAQs and support", { openUrl(helpCenterUrl) })
                ProfileMenuItem(Icons.Default.Policy, "Privacy Policy", "Review our data usage policy", { openUrl(privacyPolicyUrl) })
                ProfileMenuItem(Icons.Default.Info, "About RefreshMe", "Version 1.0.0", { openUrl(aboutUrl) })
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
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
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}