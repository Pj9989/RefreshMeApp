package com.refreshme.booking

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import com.refreshme.data.Booking
import com.refreshme.data.BookingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActiveMobileBookingScreen(
    bookingId: String,
    isStylist: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var booking by remember { mutableStateOf<Booking?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Fused Location for Stylist tracking
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Camera state for the map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f)
    }

    // Listen to booking document
    LaunchedEffect(bookingId) {
        val docRef = db.collection("bookings").document(bookingId)
        val subscription = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val b = snapshot.toObject(Booking::class.java)?.copy(id = snapshot.id)
                booking = b
                
                // If we are the client and we have a stylist location, move camera to them
                if (!isStylist && b?.stylistLat != null && b.stylistLng != null) {
                    val latLng = LatLng(b.stylistLat, b.stylistLng)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f)
                }
            }
            isLoading = false
        }
    }

    // Stylist Location Updates
    LaunchedEffect(booking?.status, locationPermissionState.status.isGranted) {
        if (!isStylist) return@LaunchedEffect
        
        // If we're on the way, start pushing location
        if (booking?.status == BookingStatus.ON_THE_WAY.name && locationPermissionState.status.isGranted) {
            startLocationUpdates(context, fusedLocationClient, bookingId)
        }
    }
    
    // Update map camera smoothly once the customer's location is known and we are the stylist
    LaunchedEffect(booking?.customerLat, booking?.customerLng, isStylist) {
        if (isStylist && booking?.customerLat != null && booking?.customerLng != null) {
            val customerLoc = LatLng(booking!!.customerLat!!, booking!!.customerLng!!)
            if (cameraPositionState.position.target.latitude == 0.0) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(customerLoc, 14f)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Tracking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (booking == null) {
                Text("Booking not found.", modifier = Modifier.align(Alignment.Center))
            } else {
                val b = booking!!
                
                // Map Layer
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        // Customer Marker
                        if (b.customerLat != null && b.customerLng != null) {
                            Marker(
                                state = MarkerState(position = LatLng(b.customerLat, b.customerLng)),
                                title = "Destination",
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                        
                        // Stylist Live Marker
                        if (b.stylistLat != null && b.stylistLng != null) {
                            Marker(
                                state = MarkerState(position = LatLng(b.stylistLat, b.stylistLng)),
                                title = b.stylistName,
                                icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE)
                            )
                        }
                    }

                    // Bottom Overlay Layer
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        shadowElevation = 16.dp
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Status Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusText = when (b.status) {
                                    BookingStatus.ON_THE_WAY.name -> if (isStylist) "You are on the way" else "${b.stylistName} is on the way!"
                                    BookingStatus.IN_PROGRESS.name -> "Service in Progress"
                                    BookingStatus.COMPLETED.name -> "Appointment Completed"
                                    else -> "Upcoming House Call"
                                }
                                
                                val statusColor = if (b.status == BookingStatus.ON_THE_WAY.name) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                                Spacer(Modifier.width(12.dp))
                                Text(statusText, fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(Modifier.height(20.dp))
                            
                            // User Info Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val photoUrl = if (isStylist) b.customerPhotoUrl else b.stylistPhotoUrl
                                val displayName = if (isStylist) b.customerName else b.stylistName
                                
                                AsyncImage(
                                    model = photoUrl ?: "https://via.placeholder.com/150",
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(b.serviceName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                }
                                IconButton(
                                    onClick = { Toast.makeText(context, "Calling $displayName...", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            
                            Spacer(Modifier.height(24.dp))
                            
                            // Stylist Action Buttons
                            if (isStylist) {
                                when (b.status) {
                                    BookingStatus.DEPOSIT_PAID.name, BookingStatus.ACCEPTED.name -> {
                                        Button(
                                            onClick = {
                                                if (!locationPermissionState.status.isGranted) {
                                                    locationPermissionState.launchPermissionRequest()
                                                } else {
                                                    updateBookingStatus(db, bookingId, BookingStatus.ON_THE_WAY)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Start Travel (On My Way)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    BookingStatus.ON_THE_WAY.name -> {
                                        Button(
                                            onClick = { updateBookingStatus(db, bookingId, BookingStatus.IN_PROGRESS) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("I Have Arrived", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    BookingStatus.IN_PROGRESS.name -> {
                                        Button(
                                            onClick = { 
                                                updateBookingStatus(db, bookingId, BookingStatus.COMPLETED)
                                                onFinish()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Complete Appointment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // Client View
                                if (b.status == BookingStatus.ON_THE_WAY.name) {
                                    Text(
                                        "Sit tight! Your stylist's location is updating live on the map.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                        }
                    }
                }
            }
        }
    }
}

private fun updateBookingStatus(db: FirebaseFirestore, bookingId: String, status: BookingStatus) {
    db.collection("bookings").document(bookingId)
        .update("status", status.name)
        .addOnFailureListener { e ->
            android.util.Log.e("TrackingScreen", "Failed to update status", e)
        }
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    bookingId: String
) {
    val db = FirebaseFirestore.getInstance()
    
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(2000L)
        .build()
        
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                // Push location up to Firestore so client sees it move
                db.collection("bookings").document(bookingId)
                    .update(
                        mapOf(
                            "stylistLat" to location.latitude,
                            "stylistLng" to location.longitude
                        )
                    )
            }
        }
    }

    fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    )
}
