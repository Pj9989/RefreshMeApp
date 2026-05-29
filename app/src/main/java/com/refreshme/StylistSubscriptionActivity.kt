package com.refreshme

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.ui.theme.RefreshMeTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StylistSubscriptionActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, StylistSubscriptionActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            RefreshMeTheme {
                PayoutsSetupScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoutsSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var stripeStatus by remember { mutableStateOf("loading") }
    var isActionLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                stripeStatus = "not_connected"
                return@LaunchedEffect
            }
            // Deployed cloud function requires { stylistId } matching caller uid.
            val result = FirebaseFunctions.getInstance()
                .getHttpsCallable("getConnectAccountStatus")
                .call(mapOf("stylistId" to user.uid))
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.getData() as? Map<String, Any>
            stripeStatus = data?.get("status") as? String ?: "not_connected"
        } catch (e: Exception) {
            stripeStatus = "not_connected"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings & Payouts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AccountBalance, 
                contentDescription = null, 
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (stripeStatus) {
                "loading" -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                "active" -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Payouts Connected!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your bank account is connected. Earnings from completed bookings will be automatically deposited.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Back to Dashboard", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    // not_connected or pending
                    Text(
                        text = if (stripeStatus == "pending") "Finish Payout Setup" else "Set up Payouts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connect your bank account securely with Stripe to receive automatic deposits for your completed bookings.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "RefreshMe takes a 10% platform fee per booking.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isActionLoading = true
                                try {
                                    // Deployed cloud function requires { stylistId, email, fullName }
                                    // and verifies stylistId == caller's auth uid.
                                    val user = FirebaseAuth.getInstance().currentUser
                                    if (user == null) {
                                        isActionLoading = false
                                        return@launch
                                    }
                                    val result = FirebaseFunctions.getInstance()
                                        .getHttpsCallable("createConnectAccount")
                                        .call(
                                            mapOf(
                                                "stylistId" to user.uid,
                                                "email" to user.email.orEmpty(),
                                                "fullName" to user.displayName.orEmpty(),
                                            ),
                                        )
                                        .await()
                                    @Suppress("UNCHECKED_CAST")
                                    val data = result.getData() as? Map<String, Any>
                                    val url = data?.get("url") as? String
                                    if (!url.isNullOrBlank()) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                } finally {
                                    isActionLoading = false
                                }
                            }
                        },
                        enabled = !isActionLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (stripeStatus == "pending") "Continue Stripe Onboarding" else "Set up Stripe Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}