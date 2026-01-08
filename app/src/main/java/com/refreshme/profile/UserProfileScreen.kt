package com.refreshme.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onEditProfile: () -> Unit,
    onManageSubscription: () -> Unit,
    onSignOut: () -> Unit,
    onViewBookings: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName ?: "User"
    val userEmail = user?.email ?: "No email"
    val userPhotoUrl = user?.photoUrl?.toString() ?: "https://via.placeholder.com/150"

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                        title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
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
                    Image(
                            painter = rememberAsyncImagePainter(userPhotoUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
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
                                tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = userEmail, fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                ProfileSectionTitle("Account Settings")
                ProfileMenuItem(Icons.Default.Person, "Edit Profile", "Update your personal info", onEditProfile)
                ProfileMenuItem(Icons.Default.CreditCard, "Manage Subscription", "View or change your plan", onManageSubscription)
                ProfileMenuItem(Icons.Default.DateRange, "My Bookings", "View your appointment history", onViewBookings)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                ProfileSectionTitle("Support & Legal")
                ProfileMenuItem(Icons.Default.Help, "Help Center", "FAQs and support", {})
                ProfileMenuItem(Icons.Default.Info, "About RefreshMe", "Version 1.0.0", {})
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
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}