package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.refreshme.ui.theme.RefreshMeTheme

/**
 * GoLiveFragment — lets a stylist toggle their online/offline status.
 * Backed by StylistHomeViewModel which writes to Firestore in real time.
 */
class GoLiveFragment : Fragment() {

    private val viewModel: StylistHomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    val isOnline by viewModel.isOnline.collectAsState()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isOnline) "You're Live" else "You're Offline",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isOnline)
                                    "Customers can see you and request your services right now."
                                else
                                    "Go online to start receiving booking requests from nearby customers.",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            Button(
                                onClick = { viewModel.toggleOnlineStatus() },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnline) Color(0xFFE53935) else Color(0xFF4CAF50)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            ) {
                                Text(
                                    text = if (isOnline) "Go Offline" else "Go Live",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isOnline) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isOnline) Color(0xFF4CAF50) else Color.Gray,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Text(
                                        text = if (isOnline) "Status: Online" else "Status: Offline",
                                        fontSize = 14.sp,
                                        color = if (isOnline) Color(0xFF4CAF50)
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
