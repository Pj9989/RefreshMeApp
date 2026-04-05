package com.refreshme

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.refreshme.booking.BookingState
import com.refreshme.booking.NewBookingViewModel
import com.refreshme.data.Service
import com.refreshme.data.ServiceType
import java.text.SimpleDateFormat
import java.util.*

enum class BookingMode {
    ASAP,
    SCHEDULE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingScreen(
    stylistId: String,
    onBack: () -> Unit,
    viewModel: NewBookingViewModel = viewModel(),
    onShowDatePicker: () -> Unit,
    onPresentPayment: (String) -> Unit
) {
    val stylist by viewModel.stylist.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedAddOns by viewModel.selectedAddOns.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isMobileBooking by viewModel.isMobileBooking.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()

    var bookingMode by remember { mutableStateOf(BookingMode.SCHEDULE) }
    var address by remember { mutableStateOf("") }
    var selectedTimeStr by remember { mutableStateOf("") }

    LaunchedEffect(stylistId) {
        viewModel.fetchStylist(stylistId)
    }

    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.RequiresPayment) {
            val secret = (bookingState as BookingState.RequiresPayment).clientSecret
            onPresentPayment(secret)
        }
    }

    if (bookingState is BookingState.Success) {
        BookingSuccessDialog(onDismiss = onBack)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (bookingState is BookingState.Error) {
                        Text(
                            text = (bookingState as BookingState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.createBooking() },
                        enabled = selectedService != null && selectedDate != null && bookingState !is BookingState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    ) {
                        if (bookingState is BookingState.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            var buttonText = "Select a Service"
                            if (selectedService != null && selectedDate != null) {
                                var total = selectedService!!.price + selectedAddOns.sumOf { it.price }
                                if (isMobileBooking) total += (stylist?.atHomeServiceFee ?: 0.0)
                                if (bookingMode == BookingMode.ASAP) total += (stylist?.emergencyFee ?: 0.0)
                                buttonText = "Confirm Booking ($${total})"
                            } else if (selectedService != null) {
                                buttonText = "Select Date & Time"
                            }
                            
                            Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            stylist?.let { s ->
                val offersMobile = s.offersAtHomeService == true || 
                                 s.serviceType == ServiceType.AT_HOME || 
                                 s.serviceType == ServiceType.ALL_HOURS ||
                                 s.serviceType == ServiceType.AFTER_HOURS

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Book an Appointment with", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    s.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Service Selection
                Text("Select a Service", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                
                val services = s.services ?: emptyList()
                if (services.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        services.forEach { service ->
                            ServiceItem(
                                service = service,
                                isSelected = service.name == selectedService?.name,
                                onServiceSelected = { viewModel.selectService(it) }
                            )
                        }
                    }
                }
                
                // Add-ons Section
                if (selectedService != null && services.size > 1) {
                    val addOnOptions = services.filter { it.name != selectedService?.name }
                    if (addOnOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Enhance Your Service (Add-ons)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            addOnOptions.forEach { addOn ->
                                AddOnItem(
                                    service = addOn,
                                    isChecked = selectedAddOns.any { it.name == addOn.name },
                                    onCheckedChange = { viewModel.toggleAddOn(addOn) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // 2. Location section
                if (offersMobile) {
                    Text("Service Location", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LocationOption(
                            title = "In-Salon",
                            isSelected = !isMobileBooking,
                            icon = Icons.Default.Store,
                            onClick = { viewModel.setMobileBooking(false) },
                            modifier = Modifier.weight(1f)
                        )
                        LocationOption(
                            title = "House Call",
                            isSelected = isMobileBooking,
                            icon = Icons.Default.Home,
                            onClick = { viewModel.setMobileBooking(true) },
                            modifier = Modifier.weight(1f),
                            subtitle = if ((s.atHomeServiceFee ?: 0.0) > 0) "+$${s.atHomeServiceFee}" else null
                        )
                    }

                    AnimatedVisibility(visible = isMobileBooking) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            // ID Verification Badge for Safety
                            if (s.requiresIdVerificationForMobile == true) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = "ID Verification Required", tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Identity Verification is required for house calls for the safety of this professional.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Enter your address") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 3. When?
                Text("When?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isAsapSelected = bookingMode == BookingMode.ASAP
                    Button(
                        onClick = { 
                            bookingMode = BookingMode.ASAP
                            viewModel.selectDate(Date()) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp), // slightly taller to fit subtitle
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAsapSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isAsapSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (!isAsapSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Squeeze-In (ASAP)", fontWeight = FontWeight.Bold)
                            if ((s.emergencyFee ?: 0.0) > 0) {
                                Text("+$${s.emergencyFee} Fee", fontSize = 10.sp)
                            }
                        }
                    }

                    val isScheduleSelected = bookingMode == BookingMode.SCHEDULE
                    Button(
                        onClick = { 
                            bookingMode = BookingMode.SCHEDULE
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScheduleSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isScheduleSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (!isScheduleSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                    ) {
                        Text("Schedule", fontWeight = FontWeight.Bold)
                    }
                }

                if (bookingMode == BookingMode.SCHEDULE) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Select Date", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    WeeklyCalendarView(
                        selectedDate = selectedDate,
                        onDateSelected = { 
                            selectedTimeStr = ""
                            viewModel.selectDate(it) 
                        }
                    )

                    if (selectedDate != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Available Times", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val times = listOf("9:00 AM", "10:30 AM", "1:00 PM", "2:30 PM", "4:00 PM", "8:00 PM", "11:00 PM")
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            times.forEach { time ->
                                TimeChip(
                                    time = time,
                                    isSelected = selectedTimeStr == time, 
                                    onClick = { 
                                        selectedTimeStr = time
                                        updateViewModelWithTime(time, selectedDate!!, viewModel)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Deposit Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Deposit Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "A 20% non-refundable deposit is required to secure your spot. The remaining balance is due at service.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun BookingSuccessDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "Booking Confirmed!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Your appointment has been successfully booked. You can view details in your bookings history.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

private fun updateViewModelWithTime(timeStr: String, baseDate: Date, viewModel: NewBookingViewModel) {
    val calendar = Calendar.getInstance()
    calendar.time = baseDate
    
    val isPm = timeStr.contains("PM", ignoreCase = true)
    val timeParts = timeStr.replace(" AM", "", ignoreCase = true)
                          .replace(" PM", "", ignoreCase = true)
                          .split(":")
    
    var hour = timeParts[0].toInt()
    val minute = timeParts[1].toInt()
    
    if (isPm && hour < 12) hour += 12
    if (!isPm && hour == 12) hour = 0
    
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    
    viewModel.selectDate(calendar.time)
}

@Composable
fun WeeklyCalendarView(
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    val dates = (0..13).map {
        val date = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        date
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(dates) { date ->
            val isSelected = selectedDate != null && 
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date) == 
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(selectedDate)
            
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            val dayNumber = SimpleDateFormat("d", Locale.getDefault()).format(date)

            Card(
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onDateSelected(date) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) else null
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName.uppercase(),
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNumber,
                        fontSize = 20.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun TimeChip(
    time: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null
    ) {
        Text(
            text = time,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LocationOption(
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ServiceItem(
    service: Service,
    isSelected: Boolean,
    onServiceSelected: (Service) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onServiceSelected(service) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onServiceSelected(service) },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(service.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("$${service.price} (${service.durationMinutes ?: 0} mins)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddOnItem(
    service: Service,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else Color.Transparent
        ),
        border = if (isChecked) BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onCheckedChange(it) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.tertiary)
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(service.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("+$${service.price} (+${service.durationMinutes ?: 0} mins)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}