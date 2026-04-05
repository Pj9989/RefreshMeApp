package com.refreshme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.refreshme.ui.theme.RefreshMeTheme

class SupportFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SupportScreen(
                            onBackClick = { findNavController().popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val supportEmail = "admin@refreshmeapp.com"
    val faqUrl = "https://refreshme-74f79.web.app/help.html"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support & Contact") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Need assistance? We're here to help!",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
            
            item {
                SupportItem(
                    title = "Visit our FAQ",
                    subtitle = "Find answers to common questions.",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(faqUrl))
                        context.startActivity(intent)
                    }
                )
            }
            
            item {
                HorizontalDivider()
                SupportItem(
                    title = "Send us an Email",
                    subtitle = supportEmail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:") 
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                            putExtra(Intent.EXTRA_SUBJECT, "RefreshMe App Support")
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                )
            }
            
            item {
                HorizontalDivider()
                SupportItem(
                    title = "Live Chat (Coming Soon)",
                    subtitle = "Chat with a support agent.",
                    onClick = { /* Placeholder for future chat integration */ }
                )
            }
        }
    }
}

@Composable
fun SupportItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}