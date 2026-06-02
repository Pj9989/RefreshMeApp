package com.refreshme.stylist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.refreshme.data.Service
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServicesScreen(
    onBack: () -> Unit,
    viewModel: ManageServicesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var serviceToEdit by remember { mutableStateOf<Service?>(null) }
    var isCreatingBundle by remember { mutableStateOf(false) }
    var isCreatingAddOn by remember { mutableStateOf(false) }

    // Categorised suggestions so the screen caters to all stylists
    data class SuggestionGroup(val label: String, val services: List<Service>)
    val suggestionGroups = listOf(
        SuggestionGroup("Men's", listOf(
            Service(name = "Classic Fade", price = 35.0, durationMinutes = 45),
            Service(name = "Beard Sculpting", price = 20.0, durationMinutes = 20),
            Service(name = "Line-Up / Edge", price = 20.0, durationMinutes = 20),
            Service(name = "Taper Cut", price = 40.0, durationMinutes = 45),
        )),
        SuggestionGroup("Women's", listOf(
            Service(name = "Blow Out", price = 50.0, durationMinutes = 60),
            Service(name = "Silk Press", price = 65.0, durationMinutes = 75),
            Service(name = "Color & Highlights", price = 85.0, durationMinutes = 90),
            Service(name = "Natural Hair Style", price = 70.0, durationMinutes = 75),
        )),
        SuggestionGroup("All Hair", listOf(
            Service(name = "Full Cut & Style", price = 55.0, durationMinutes = 60),
            Service(name = "Kids Cut", price = 25.0, durationMinutes = 30),
            Service(name = "Braids", price = 80.0, durationMinutes = 90),
            Service(name = "Deep Condition", price = 35.0, durationMinutes = 45),
            Service(name = "Twist Locs", price = 90.0, durationMinutes = 120),
        )),
        // Added in 3.0.5 alongside the Makeup category for multi-discipline pros.
        SuggestionGroup("Makeup", listOf(
            Service(name = "Natural / Day Makeup", price = 60.0, durationMinutes = 45),
            Service(name = "Full Glam Makeup", price = 85.0, durationMinutes = 75),
            Service(name = "Bridal Makeup", price = 150.0, durationMinutes = 90),
            Service(name = "Special Event Makeup", price = 100.0, durationMinutes = 60),
            Service(name = "Lashes Application", price = 30.0, durationMinutes = 20),
        )),
        // Added in 3.0.5 alongside the Nails category for multi-discipline pros.
        SuggestionGroup("Nails", listOf(
            Service(name = "Classic Manicure", price = 25.0, durationMinutes = 30),
            Service(name = "Gel Manicure", price = 40.0, durationMinutes = 45),
            Service(name = "Classic Pedicure", price = 35.0, durationMinutes = 45),
            Service(name = "Gel Pedicure", price = 50.0, durationMinutes = 60),
            Service(name = "Acrylic Full Set", price = 65.0, durationMinutes = 90),
            Service(name = "Nail Art", price = 15.0, durationMinutes = 20, isAddOn = true),
        )),
        SuggestionGroup("Special / Add-ons", listOf(
            Service(name = "Travel Prep", price = 15.0, durationMinutes = 15, isAddOn = true),
            Service(name = "Extra Length / Density", price = 25.0, durationMinutes = 30, isAddOn = true),
            Service(name = "After-Hours Appointment", price = 30.0, durationMinutes = 0, isAddOn = true),
            Service(name = "Color Correction Consult", price = 40.0, durationMinutes = 30),
            Service(name = "Bridal Trial", price = 75.0, durationMinutes = 60, isAddOn = true),
        )),
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Manage Menu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Set services, add-ons & packages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            var expanded by remember { mutableStateOf(false) }
            
            Column(horizontalAlignment = Alignment.End) {
                if (expanded) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            expanded = false
                            serviceToEdit = null
                            isCreatingBundle = false
                            isCreatingAddOn = false
                            showEditDialog = true 
                        },
                        icon = { Icon(Icons.Default.ContentCut, "Service") },
                        text = { Text("Service") },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )

                    ExtendedFloatingActionButton(
                        onClick = {
                            expanded = false
                            serviceToEdit = null
                            isCreatingBundle = false
                            isCreatingAddOn = true
                            showEditDialog = true
                        },
                        icon = { Icon(Icons.Default.AddCircleOutline, "Add-on") },
                        text = { Text("Add-on") },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    
                    ExtendedFloatingActionButton(
                        onClick = { 
                            expanded = false
                            serviceToEdit = null
                            isCreatingBundle = true
                            isCreatingAddOn = false
                            showEditDialog = true 
                        },
                        icon = { Icon(Icons.Default.CardGiftcard, "Bundle") },
                        text = { Text("Service Bundle") },
                        modifier = Modifier.padding(bottom = 16.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
                
                FloatingActionButton(
                    onClick = { expanded = !expanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 88.dp) // Lift FAB above bottom navigation and last card
                ) {
                    Icon(
                        if (expanded) Icons.Default.Close else Icons.Default.Add, 
                        contentDescription = "Add"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        // Everything lives inside ONE scrollable LazyColumn so the whole
        // page can scroll as a single unit. Quick-Add suggestion groups
        // are item rows at the top; services follow underneath.
        when (val state = uiState) {
            is ServicesUiState.Loading -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            is ServicesUiState.Error -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
            }
            is ServicesUiState.Success -> {
                val singleServices = state.services.filter { !it.isBundle && !it.isAddOnCandidate() }
                val addOns = state.services.filter { !it.isBundle && it.isAddOnCandidate() }
                val bundles = state.services.filter { it.isBundle }
                val distinctServices = singleServices.distinctBy { "${it.name}_${it.price}_${it.durationMinutes}" }
                val distinctAddOns = addOns.distinctBy { "${it.name}_${it.price}_${it.durationMinutes}" }

                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp), // leave room for the FAB
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ---- Quick Add header ----
                    item {
                        Text(
                            "Quick Add Suggestions",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    // ---- One row per suggestion category ----
                    items(suggestionGroups) { group ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                val groupIcon = when (group.label) {
                                    "Men's"   -> Icons.Default.Man
                                    "Women's" -> Icons.Default.Woman
                                    "Makeup"  -> Icons.Default.Brush
                                    "Nails"   -> Icons.Default.Spa
                                    "Special / Add-ons" -> Icons.Default.Star
                                    else      -> Icons.Default.People
                                }
                                Icon(groupIcon, contentDescription = group.label,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(group.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                items(group.services) { service ->
                                    SuggestionChip(service) { viewModel.addService(service) }
                                }
                            }
                        }
                    }

                    // ---- Your menu section ----
                    item { Spacer(Modifier.height(12.dp)) }

                    if (state.services.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                                EmptyServicesView()
                            }
                        }
                    } else {
                        if (bundles.isNotEmpty()) {
                            item {
                                Text(
                                    "Service Bundles & Packages",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                            items(bundles) { bundle ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                    ServiceManagementItem(
                                        service = bundle,
                                        onEdit = {
                                            serviceToEdit = bundle
                                            isCreatingBundle = true
                                            isCreatingAddOn = false
                                            showEditDialog = true
                                        },
                                        onDelete = { viewModel.deleteService(bundle.id) }
                                    )
                                }
                            }
                        }

                        if (distinctServices.isNotEmpty()) {
                            item {
                                Text(
                                    "A La Carte Services",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                            items(distinctServices) { service ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                    ServiceManagementItem(
                                        service = service,
                                        onEdit = {
                                            serviceToEdit = service
                                            isCreatingBundle = false
                                            isCreatingAddOn = false
                                            showEditDialog = true
                                        },
                                        onDelete = {
                                            viewModel.deleteService(service.id)
                                        }
                                    )
                                }
                            }
                        }

                        if (distinctAddOns.isNotEmpty()) {
                            item {
                                Text(
                                    "Add-ons",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                            items(distinctAddOns) { addOn ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                    ServiceManagementItem(
                                        service = addOn,
                                        onEdit = {
                                            serviceToEdit = addOn
                                            isCreatingBundle = false
                                            isCreatingAddOn = true
                                            showEditDialog = true
                                        },
                                        onDelete = {
                                            viewModel.deleteService(addOn.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            ServiceEditDialog(
                service = serviceToEdit,
                isBundle = isCreatingBundle,
                isAddOn = isCreatingAddOn,
                onDismiss = { showEditDialog = false },
                onConfirm = { service ->
                    if (serviceToEdit == null) {
                        viewModel.addService(service)
                    } else {
                        viewModel.updateService(service.copy(id = serviceToEdit!!.id))
                    }
                    showEditDialog = false
                }
            )
        }
    }
}

@Composable
fun SuggestionChip(service: Service, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (service.isAddOnCandidate()) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (service.isAddOnCandidate()) Icons.Default.AddCircleOutline else Icons.Default.ContentCut,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(service.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ServiceManagementItem(service: Service, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAddOn = service.isAddOnCandidate()
    val containerColor = when {
        service.isBundle -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        isAddOn -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val iconColor = when {
        service.isBundle -> MaterialTheme.colorScheme.tertiary
        isAddOn -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val icon = when {
        service.isBundle -> Icons.Default.CardGiftcard
        isAddOn -> Icons.Default.AddCircleOutline
        else -> Icons.Default.ContentCut
    }

    val titleCasedName = service.name.split(" ").joinToString(" ") { 
        it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } 
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleCasedName, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (service.description.isNotBlank() && (service.isBundle || isAddOn)) {
                    Text(service.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${if (isAddOn) "Add-on • " else ""}${service.durationMinutes} mins • $${String.format(Locale.US, "%.2f", service.price)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyServicesView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("Your Menu is Empty", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Add services so clients can book you", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEditDialog(
    service: Service?,
    isBundle: Boolean,
    isAddOn: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Service) -> Unit
) {
    var name by remember { mutableStateOf(service?.name ?: "") }
    var description by remember { mutableStateOf(service?.description ?: "") }
    var price by remember { mutableStateOf(service?.price?.toString() ?: "") }
    var duration by remember { mutableStateOf(service?.durationMinutes?.toString() ?: "") }

    val title = if (service == null) {
        when {
            isBundle -> "Create Package"
            isAddOn -> "New Add-on"
            else -> "New Service"
        }
    } else {
        when {
            isBundle -> "Edit Package"
            isAddOn -> "Edit Add-on"
            else -> "Edit Service"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        isBundle -> Icons.Default.CardGiftcard
                        isAddOn -> Icons.Default.AddCircleOutline
                        else -> Icons.Default.ContentCut
                    },
                    contentDescription = null,
                    tint = when {
                        isBundle -> MaterialTheme.colorScheme.tertiary
                        isAddOn -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                )
                Text(title)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isBundle) {
                    Text(
                        "Packages help increase your average ticket size by grouping multiple services together at a slightly discounted rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isAddOn) {
                    Text(
                        "Add-ons appear after a client chooses a main service and are priced into the deposit automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = {
                        Text(
                            when {
                                isBundle -> "Package Name (e.g. The Ultimate Refresh)"
                                isAddOn -> "Add-on Name (e.g. Nail Art)"
                                else -> "Service Name"
                            }
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                if (isBundle || isAddOn) {
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { description = it }, 
                        label = {
                            Text(
                                if (isBundle) {
                                    "What's included? (e.g. Cut + Wash + Hot Towel Shave)"
                                } else {
                                    "Short description"
                                }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price, 
                        onValueChange = { price = it }, 
                        label = { Text("Price ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = duration, 
                        onValueChange = { duration = it }, 
                        label = { Text("Mins") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    val d = duration.toIntOrNull() ?: 30
                    if (name.isNotBlank()) {
                        onConfirm(Service(
                            name = name, 
                            description = description,
                            price = p, 
                            durationMinutes = d,
                            isBundle = isBundle,
                            isAddOn = isAddOn
                        ))
                    }
                }
            ) {
                Text(if (service == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

private fun Service.isAddOnCandidate(): Boolean =
    isAddOn || name.contains("add-on", ignoreCase = true) || name.contains("addon", ignoreCase = true)
