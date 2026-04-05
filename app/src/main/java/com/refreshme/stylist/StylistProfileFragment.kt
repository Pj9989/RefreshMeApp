package com.refreshme.stylist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.StylistSubscriptionActivity
import com.refreshme.auth.RoleSelectActivity
import com.refreshme.profile.EditProfileActivity
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StylistProfileFragment : Fragment() {

    @Inject lateinit var auth: FirebaseAuth
    private val viewModel: StylistProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StylistProfileScreen(
                            viewModel = viewModel,
                            onNavigateToEditProfile = {
                                val intent = Intent(requireContext(), EditProfileActivity::class.java)
                                intent.putExtra("IS_STYLIST", true)
                                startActivity(intent)
                            },
                            onNavigateToSubscription = {
                                startActivity(StylistSubscriptionActivity.newIntent(requireContext()))
                            },
                            onOpenUrl = { url ->
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            onSignOut = {
                                viewModel.signOut()
                                val intent = Intent(activity, RoleSelectActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                activity?.finish()
                            },
                            onDeleteAccount = {
                                val user = auth.currentUser
                                user?.delete()?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(activity, RoleSelectActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        }
                                        startActivity(intent)
                                        activity?.finish()
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to delete account. You may need to sign out and sign in again before deleting.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StylistProfileScreen(
    viewModel: StylistProfileViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showDeleteDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Stylist Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val stylist = uiState.stylist

        // Profile Header
        if (stylist != null) {
            val displayName = remember(stylist.name) {
                stylist.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = stylist.displayImageUrl ?: "https://via.placeholder.com/150",
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = displayName.ifEmpty { "Stylist" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (stylist.isVerifiedStylist) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verified Stylist",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StylistProfileStat(title = "Rating", value = String.format(java.util.Locale.US, "%.1f ★", stylist.rating))
                StylistProfileStat(title = "Reviews", value = (stylist.reviewCount ?: 0).toString())
            }
        }

        // Online Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val isOnline = stylist?.isOnline == true
            Text(
                text = if (isOnline) "Online - Accepting Bookings" else "Go Online",
                fontSize = 16.sp,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = isOnline,
                onCheckedChange = { isChecked ->
                    if (isChecked && !uiState.hasActiveSubscriptionOrTrial) {
                        Toast.makeText(context, "Activate subscription to go online", Toast.LENGTH_LONG).show()
                    } else if (stylist?.isOnline != isChecked) {
                        viewModel.toggleOnlineStatus(isChecked)
                    }
                }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Actions
        StylistProfileMenuItem(
            icon = Icons.Default.Edit,
            title = "Edit Profile",
            subtitle = "Update your details and portfolio",
            onClick = onNavigateToEditProfile
        )

        // Payout Account Setup (Stripe Connect)
        val subTitle = "Payout Account"
        val subSubtitle = when {
            uiState.isSubscriptionActive -> "Connected \u2014 payouts enabled"
            uiState.isTrialActive -> "Set up your Stripe payout account"
            else -> "Set up your Stripe payout account"
        }
        
        StylistProfileMenuItem(
            icon = Icons.Default.Payment,
            title = subTitle,
            subtitle = subSubtitle,
            onClick = onNavigateToSubscription
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            text = "Support & Legal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        StylistProfileMenuItem(
            icon = Icons.AutoMirrored.Filled.Help,
            title = "Help Center",
            subtitle = "Stylist support and FAQs",
            onClick = { onOpenUrl("https://refreshme-74f79.web.app/help.html") }
        )
        
        StylistProfileMenuItem(
            icon = Icons.Default.Info,
            title = "About RefreshMe",
            subtitle = "Version 3.0.0",
            onClick = { onOpenUrl("https://refreshme-74f79.web.app/") }
        )

        StylistProfileMenuItem(
            icon = Icons.Default.Policy,
            title = "Privacy Policy",
            subtitle = "Review our data usage policy",
            onClick = { onOpenUrl("https://refreshme-74f79.web.app/privacy.html?v=${System.currentTimeMillis()}") }
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StylistProfileStat(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StylistProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
