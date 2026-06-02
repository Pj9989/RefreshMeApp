package com.refreshme.booking

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.refreshme.data.Service
import com.refreshme.data.Stylist
import com.refreshme.legal.AtHomeWaiverDialog
import com.refreshme.legal.AllergyDisclosureDialog
import com.refreshme.legal.LegalDocKeys
import com.refreshme.legal.LegalRepository
import com.refreshme.legal.LegalVersions
import com.refreshme.legal.serviceRequiresAllergyDisclosure
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    stylistId: String,
    serviceName: String?,
    servicePrice: Float,
    onBack: () -> Unit,
    onDateTimeClick: () -> Unit,
    onAsapClick: () -> Unit,
    onPaymentRequired: (clientSecret: String, depositAmount: Double) -> Unit,
    onBookingSuccess: () -> Unit,
    viewModel: NewBookingViewModel = viewModel()
) {
    val stylist by viewModel.stylist.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedAddOns by viewModel.selectedAddOns.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()
    val waitlistState by viewModel.waitlistState.collectAsState()
    val customerVerificationStatus by viewModel.customerVerificationStatus.collectAsState()
    val isEmergencyAsap by viewModel.isEmergencyAsap.collectAsState()
    val isFirstBookingCustomer by viewModel.isFirstBookingCustomer.collectAsState()
    
    val isEvent by viewModel.isEvent.collectAsState()
    val groupSize by viewModel.groupSize.collectAsState()
    val eventType by viewModel.eventType.collectAsState()
    
    val isMobileBooking by viewModel.isMobileBooking.collectAsState()
    
    val scrollState = rememberScrollState()

    var showWaitlistDialog by remember { mutableStateOf(false) }
    var showIdentityVerificationDialog by remember { mutableStateOf(false) }
    // Legal gating for the Confirm & Pay button.
    var showAtHomeWaiver by remember { mutableStateOf(false) }
    var showAllergyDisclosure by remember { mutableStateOf(false) }
    var acceptedWaiverVersion by remember { mutableStateOf<String?>(null) }
    var acceptedAllergyVersion by remember { mutableStateOf<String?>(null) }
    val legalRepo = remember { LegalRepository() }
    val legalScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchCustomerVerificationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(stylistId) {
        viewModel.fetchStylist(stylistId)
        if (serviceName != null && servicePrice > 0) {
            viewModel.selectService(Service(name = serviceName, price = servicePrice.toDouble()))
        }
    }

    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.RequiresPayment) {
            val state = bookingState as BookingState.RequiresPayment
            val serviceSubtotal = ((selectedService?.price ?: 0.0) + selectedAddOns.sumOf { it.price }) * groupSize
            val travelFee = if (isMobileBooking) stylist?.effectiveAtHomeServiceFee ?: 0.0 else 0.0
            val emergencyFee = if (isEmergencyAsap) stylist?.emergencyFee ?: 0.0 else 0.0
            val subtotal = serviceSubtotal + travelFee + emergencyFee
            val promoDiscount = if (isFirstBookingCustomer) {
                minOf(NewBookingViewModel.FIRST_BOOKING_PROMO_AMOUNT, subtotal)
            } else {
                0.0
            }
            val totalPrice = subtotal - promoDiscount
            onPaymentRequired(state.clientSecret, state.depositAmount ?: (totalPrice * 0.2))
        } else if (bookingState is BookingState.Success) {
            onBookingSuccess()
        }
    }
    
    LaunchedEffect(waitlistState) {
        if (waitlistState is WaitlistState.Success) {
            showWaitlistDialog = false
            viewModel.resetWaitlistState()
        }
    }
    
    // At-Home waiver — shown after the user taps Confirm if mobile booking + not yet accepted.
    if (showAtHomeWaiver) {
        AtHomeWaiverDialog(
            onAccepted = { version ->
                acceptedWaiverVersion = version
                showAtHomeWaiver = false
                legalScope.launch {
                    runCatching {
                        legalRepo.recordAcceptance(
                            docKey = LegalDocKeys.AT_HOME_LIABILITY_WAIVER,
                            docVersion = version,
                        )
                    }
                }
                // Continue into allergy step (if needed) or booking.
                val svc = selectedService?.name
                if (serviceRequiresAllergyDisclosure(svc) && acceptedAllergyVersion == null) {
                    showAllergyDisclosure = true
                } else {
                    viewModel.createBooking(
                        isEmergencyAsap = isEmergencyAsap,
                        waiverAcceptedVersion = version,
                        allergyDisclosureVersion = acceptedAllergyVersion,
                    )
                }
            },
            onDismiss = { showAtHomeWaiver = false }
        )
    }

    // Allergy disclosure — shown for chemical services.
    if (showAllergyDisclosure) {
        AllergyDisclosureDialog(
            onAccepted = { version ->
                acceptedAllergyVersion = version
                showAllergyDisclosure = false
                legalScope.launch {
                    runCatching {
                        legalRepo.recordAcceptance(
                            docKey = LegalDocKeys.ALLERGY_DISCLOSURE,
                            docVersion = version,
                        )
                    }
                }
                viewModel.createBooking(
                    isEmergencyAsap = isEmergencyAsap,
                    waiverAcceptedVersion = acceptedWaiverVersion,
                    allergyDisclosureVersion = version,
                )
            },
            onDismiss = { showAllergyDisclosure = false }
        )
    }

    if (showIdentityVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showIdentityVerificationDialog = false },
            title = { Text("Verification Required", fontWeight = FontWeight.Bold) },
            text = { 
                Text("For the safety of our stylists, you must verify your identity before booking an at-home house call appointment.") 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showIdentityVerificationDialog = false
                        context.startActivity(com.refreshme.identity.IdentityVerificationActivity.newIntent(context))
                    }
                ) {
                    Text("Verify Identity")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIdentityVerificationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWaitlistDialog) {
        val datePickerState = rememberDatePickerState()
        
        DatePickerDialog(
            onDismissRequest = { showWaitlistDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.joinWaitlist(Date(millis))
                        }
                    },
                    enabled = datePickerState.selectedDateMillis != null && waitlistState !is WaitlistState.Loading
                ) {
                    if (waitlistState is WaitlistState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Join Waitlist")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaitlistDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text(text = "Select Date", modifier = Modifier.padding(16.dp)) },
                headline = { Text(text = "Join Waitlist For:", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // 1. Stylist Summary
                stylist?.let { s ->
                    StylistSummaryCard(s)
                    Spacer(Modifier.height(24.dp))
                } ?: Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                // 2. Service Selection
                SectionTitle("Service")
                if (selectedService != null && selectedService?.name?.isNotBlank() == true) {
                    ServiceSummaryCard(selectedService!!) {
                        viewModel.selectService(Service()) 
                    }
                } else {
                    val services = stylist?.services ?: emptyList()
                    val bookableServices = services.filter { !it.isAddOnCandidate() }
                        .ifEmpty { services }
                    if (bookableServices.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(bookableServices) { service ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.selectService(service) },
                                    label = { Text("${service.name} ($${String.format(Locale.US, "%.0f", service.price)})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "No bookable services yet",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "This stylist needs to add services before you can book.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val addOnOptions = stylist?.services.orEmpty()
                    .filter { it.isAddOnCandidate() && it.name != selectedService?.name }
                AnimatedVisibility(visible = selectedService != null && addOnOptions.isNotEmpty()) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        SectionTitle("Add-ons")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            addOnOptions.forEach { addOn ->
                                AddOnOptionRow(
                                    service = addOn,
                                    selected = selectedAddOns.any { it.id == addOn.id || it.name == addOn.name },
                                    onToggle = { viewModel.toggleAddOn(addOn) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // 2.5 Group & Special Event
                SectionTitle("Booking Type")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Groups, 
                                    contentDescription = null, 
                                    tint = if (isEvent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Group or Special Event", fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = isEvent,
                                onCheckedChange = { viewModel.toggleEventBooking(it) }
                            )
                        }
                        
                        AnimatedVisibility(visible = isEvent) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                Text("Group Size: $groupSize", style = MaterialTheme.typography.bodyMedium)
                                Slider(
                                    value = groupSize.toFloat(),
                                    onValueChange = { viewModel.setGroupSize(it.toInt()) },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                                
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = eventType,
                                    onValueChange = { viewModel.setEventType(it) },
                                    label = { Text("Event Type (e.g., Wedding, Party)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        val offersMobile = stylist?.offersAtHomeService == true || 
                                         stylist?.serviceType == com.refreshme.data.ServiceType.AT_HOME || 
                                         stylist?.serviceType == com.refreshme.data.ServiceType.ALL_HOURS
                        
                        if (offersMobile) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        Icons.Default.DirectionsCar, 
                                        contentDescription = null, 
                                        tint = if (isMobileBooking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("At Home Service (House Call)", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Stylist travels to you (+$${String.format(Locale.US, "%.0f", stylist?.effectiveAtHomeServiceFee ?: 0.0)} fee)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isMobileBooking,
                                    onCheckedChange = { checked ->
                                        if (checked && customerVerificationStatus != com.refreshme.identity.VerificationStatus.VERIFIED) {
                                            showIdentityVerificationDialog = true
                                        } else {
                                            viewModel.setMobileBooking(checked)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // 3. Date & Time Selection
                SectionTitle("Appointment Date & Time")
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DateTimeSelectionCard(
                        selectedDate = selectedDate,
                        isAsap = isEmergencyAsap,
                        onSelectClick = onDateTimeClick,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    AsapButton(
                        selected = isEmergencyAsap,
                        onClick = onAsapClick
                    )
                }

                AnimatedVisibility(visible = isEmergencyAsap) {
                    val emergencyFee = stylist?.emergencyFee ?: 0.0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (emergencyFee > 0.0) {
                                    "ASAP priority fee: $${String.format(Locale.US, "%.2f", emergencyFee)}"
                                } else {
                                    "No ASAP priority fee set for this stylist"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                Surface(
                    onClick = { showWaitlistDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No time slot works?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Join Waitlist to be notified of cancellations.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                
                Spacer(Modifier.height(24.dp))

                // 3.5 Modern Salon Experience
                SectionTitle("Preferences")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    val isSilentAppointment by viewModel.isSilentAppointment.collectAsState()
                    val isSensoryFriendly by viewModel.isSensoryFriendly.collectAsState()

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Silent Appointment", fontWeight = FontWeight.Bold)
                                Text("Skip the small talk and just relax.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isSilentAppointment,
                                onCheckedChange = { viewModel.toggleSilentAppointment(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sensory Friendly", fontWeight = FontWeight.Bold)
                                Text("Request lower music/lights if possible.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isSensoryFriendly,
                                onCheckedChange = { viewModel.toggleSensoryFriendly(it) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                // 4. Payment Breakdown
                SectionTitle("Payment Breakdown")
                val serviceSubtotal = ((selectedService?.price ?: 0.0) + selectedAddOns.sumOf { it.price }) * groupSize
                val travelFee = if (isMobileBooking) stylist?.effectiveAtHomeServiceFee ?: 0.0 else 0.0
                val emergencyFee = if (isEmergencyAsap) stylist?.emergencyFee ?: 0.0 else 0.0
                val subtotal = serviceSubtotal + travelFee + emergencyFee
                val promoDiscount = if (isFirstBookingCustomer) {
                    minOf(NewBookingViewModel.FIRST_BOOKING_PROMO_AMOUNT, subtotal)
                } else {
                    0.0
                }
                val totalPrice = subtotal - promoDiscount
                PaymentBreakdownCard(
                    serviceSubtotal = serviceSubtotal,
                    travelFee = travelFee,
                    emergencyFee = emergencyFee,
                    promoDiscount = promoDiscount,
                    totalPrice = totalPrice,
                    isGroup = groupSize > 1,
                    isMobile = isMobileBooking,
                    isAsap = isEmergencyAsap
                )
                Spacer(Modifier.height(32.dp))

                // 5. Confirm Button
                val isOperable = (selectedService != null && selectedService?.name?.isNotBlank() == true) && selectedDate != null
                
                Button(
                    onClick = {
                        // Legal gating: require at-home waiver + allergy disclosure first.
                        val needsWaiver = isMobileBooking && acceptedWaiverVersion == null
                        val svcName = selectedService?.name
                        val needsAllergy = serviceRequiresAllergyDisclosure(svcName) &&
                            acceptedAllergyVersion == null
                        when {
                            needsWaiver -> showAtHomeWaiver = true
                            needsAllergy -> showAllergyDisclosure = true
                            else -> viewModel.createBooking(
                                isEmergencyAsap = isEmergencyAsap,
                                waiverAcceptedVersion = acceptedWaiverVersion,
                                allergyDisclosureVersion = acceptedAllergyVersion,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    enabled = isOperable && bookingState !is BookingState.Loading
                ) {
                    if (bookingState is BookingState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            if (isOperable) "Confirm & Pay Deposit" else "Please Complete Selection", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (bookingState is BookingState.Error) {
                    Text(
                        text = (bookingState as BookingState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (waitlistState is WaitlistState.Error) {
                    Text(
                        text = (waitlistState as WaitlistState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun StylistSummaryCard(stylist: Stylist) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(stylist.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(stylist.specialty ?: "Stylist", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                if (stylist.instantBookingEnabled == true) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Instant — confirmed when you pay", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceSummaryCard(service: Service, onClear: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${service.durationMinutes ?: 30} mins", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            Text(
                "$${String.format(Locale.US, "%.2f", service.price)}",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AddOnOptionRow(
    service: Service,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    service.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                val details = buildList {
                    add("${service.durationMinutes ?: 0} mins")
                    if (service.description.isNotBlank()) add(service.description)
                }.joinToString(" • ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
            }
            Text(
                "+$${String.format(Locale.US, "%.2f", service.price)}",
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun DateTimeSelectionCard(
    selectedDate: Date?,
    isAsap: Boolean,
    onSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onSelectClick,
        modifier = modifier,
        color = if (isAsap) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isAsap) 2.dp else 1.dp,
            color = when {
                isAsap -> MaterialTheme.colorScheme.primary
                selectedDate == null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    isAsap -> Icons.Default.Bolt
                    selectedDate == null -> Icons.AutoMirrored.Filled.EventNote
                    else -> Icons.Default.EventAvailable
                },
                contentDescription = null,
                tint = when {
                    isAsap -> MaterialTheme.colorScheme.primary
                    selectedDate == null -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFF4CAF50)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isAsap -> "ASAP appointment"
                        selectedDate == null -> "Choose Date & Time"
                        else -> {
                            val sdf = java.text.SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                            sdf.format(selectedDate)
                        }
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedDate == null || isAsap) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (selectedDate != null) {
                    val sdf = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
                    Text(
                        text = if (isAsap) "Estimated for ${sdf.format(selectedDate)}" else sdf.format(selectedDate),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Service.isAddOnCandidate(): Boolean =
    isAddOn || name.contains("add-on", ignoreCase = true) || name.contains("addon", ignoreCase = true)

@Composable
fun AsapButton(selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(IntrinsicSize.Min),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 1f else 0.5f)
        ),
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = "ASAP",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PaymentBreakdownCard(
    serviceSubtotal: Double,
    travelFee: Double,
    emergencyFee: Double,
    promoDiscount: Double,
    totalPrice: Double,
    isGroup: Boolean = false,
    isMobile: Boolean = false,
    isAsap: Boolean = false
) {
    val deposit = totalPrice * 0.20
    val remaining = totalPrice - deposit
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            PriceRow(
                if (isGroup) "Services subtotal" else "Service price",
                serviceSubtotal
            )

            if (isMobile) {
                Spacer(Modifier.height(8.dp))
                PriceRow("Travel fee", travelFee)
            }

            if (isAsap) {
                Spacer(Modifier.height(8.dp))
                PriceRow(
                    "ASAP priority fee",
                    emergencyFee,
                    color = MaterialTheme.colorScheme.primary,
                    isBold = true
                )
            }

            if (promoDiscount > 0) {
                Spacer(Modifier.height(8.dp))
                PriceRow("First booking promo", -promoDiscount, color = Color(0xFF4CAF50), isBold = true)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            PriceRow(
                when {
                    isAsap && isGroup -> "ASAP group total"
                    isAsap -> "ASAP appointment total"
                    isGroup -> "Total group price"
                    else -> "Total"
                },
                totalPrice,
                isBold = true
            )
            Spacer(Modifier.height(8.dp))
            PriceRow("Deposit (20% paid now)", deposit, color = Color(0xFF4CAF50), isBold = true)
            Spacer(Modifier.height(8.dp))
            PriceRow("Remaining Balance", remaining)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Deposits are non-refundable unless the stylist cancels the appointment.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun PriceRow(label: String, amount: Double, color: Color = MaterialTheme.colorScheme.onSurface, isBold: Boolean = false) {
    val formattedAmount = if (amount < 0) {
        "-$${String.format(Locale.US, "%.2f", kotlin.math.abs(amount))}"
    } else {
        "$${String.format(Locale.US, "%.2f", amount)}"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isBold) color else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(
            formattedAmount,
            color = color,
            fontSize = 16.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
