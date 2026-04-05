package com.refreshme.stylist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.refreshme.ui.theme.RefreshMeTheme

data class BookingRequest(
    val id: String = "",
    val customerName: String = "",
    val serviceName: String = "",
    val price: Double = 0.0,
    val location: String = "",
    val notes: String = ""
)

/**
 * RequestsFragment — shows incoming PENDING booking requests in real time.
 * Stylists can Accept or Decline each request directly from this screen.
 */
class RequestsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    RequestsScreen(
                        onAccept = { requestId -> acceptRequest(requestId) },
                        onDecline = { requestId -> declineRequest(requestId) }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }

    private fun acceptRequest(requestId: String) {
        firestore.collection("bookings").document(requestId)
            .update("status", "CONFIRMED")
            .addOnSuccessListener {
                Toast.makeText(context, "Booking confirmed!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to confirm booking", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineRequest(requestId: String) {
        firestore.collection("bookings").document(requestId)
            .update("status", "CANCELLED")
            .addOnSuccessListener {
                Toast.makeText(context, "Booking declined", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to decline booking", Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun RequestsScreen(
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    var requests by remember { mutableStateOf<List<BookingRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid
        val registration = if (uid != null) {
            firestore.collection("bookings")
                .whereEqualTo("stylistId", uid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, _ ->
                    isLoading = false
                    requests = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            BookingRequest(
                                id = doc.id,
                                customerName = doc.getString("customerName") ?: "Unknown",
                                serviceName = doc.getString("serviceName") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                location = doc.getString("location") ?: "",
                                notes = doc.getString("notes") ?: ""
                            )
                        } catch (e: Exception) { null }
                    } ?: emptyList()
                }
        } else null

        onDispose { registration?.remove() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Incoming Requests",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                requests.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No pending requests",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Go online to start receiving bookings",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(requests) { request ->
                            RequestCard(
                                request = request,
                                onAccept = { onAccept(request.id) },
                                onDecline = { onDecline(request.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: BookingRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = request.customerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "$${String.format("%.2f", request.price)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = request.serviceName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (request.location.isNotBlank()) {
                Text(
                    text = request.location,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (request.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Note: ${request.notes}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Decline")
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Accept", color = Color.White)
                }
            }
        }
    }
}
