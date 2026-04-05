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

    val suggestedServices = listOf(
        Service(name = "Classic Fade", price = 35.0, durationMinutes = 45),
        Service(name = "Beard Sculpting", price = 20.0, durationMinutes = 20),
        Service(name = "Royal Shave", price = 40.0, durationMinutes = 30),
        Service(name = "Hair Coloring", price = 85.0, durationMinutes = 90)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Manage Menu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Set your services & packages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            showEditDialog = true 
                        },
                        icon = { Icon(Icons.Default.ContentCut, "Service") },
                        text = { Text("Single Service") },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    
                    ExtendedFloatingActionButton(
                        onClick = { 
                            expanded = false
                            serviceToEdit = null
                            isCreatingBundle = true
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            Text(
                "Quick Add Suggestions", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(suggestedServices) { service ->
                    SuggestionChip(service) { viewModel.addService(service) }
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = uiState) {
                    is ServicesUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ServicesUiState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    }
                    is ServicesUiState.Success -> {
                        if (state.services.isEmpty()) {
                            EmptyServicesView()
                        } else {
                            val singleServices = state.services.filter { !it.isBundle }
                            val bundles = state.services.filter { it.isBundle }
                            
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp), // Extra padding for FAB
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (bundles.isNotEmpty()) {
                                    item {
                                        Text("Service Bundles & Packages", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                                    }
                                    items(bundles) { bundle ->
                                        ServiceManagementItem(
                                            service = bundle,
                                            onEdit = {
                                                serviceToEdit = bundle
                                                isCreatingBundle = true
                                                showEditDialog = true
                                            },
                                            onDelete = { viewModel.deleteService(bundle.id) }
                                        )
                                    }
                                }
                                
                                if (singleServices.isNotEmpty()) {
                                    item {
                                        Text("A La Carte Services", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                                    }
                                    // Make sure we filter out true duplicates from the UI side in case db has dupes
                                    val distinctServices = singleServices.distinctBy { "${it.name}_${it.price}_${it.durationMinutes}" }
                                    
                                    items(distinctServices) { service ->
                                        ServiceManagementItem(
                                            service = service,
                                            onEdit = {
                                                serviceToEdit = service
                                                isCreatingBundle = false
                                                showEditDialog = true
                                            },
                                            onDelete = { 
                                                viewModel.deleteService(service.id) 
                                            }
                                        )
                                    }
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
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(service.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ServiceManagementItem(service: Service, onEdit: () -> Unit, onDelete: () -> Unit) {
    val containerColor = if (service.isBundle) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (service.isBundle) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val icon = if (service.isBundle) Icons.Default.CardGiftcard else Icons.Default.ContentCut

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
                if (service.description.isNotBlank() && service.isBundle) {
                    Text(service.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("${service.durationMinutes} mins • $${String.format(Locale.US, "%.2f", service.price)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
    onDismiss: () -> Unit,
    onConfirm: (Service) -> Unit
) {
    var name by remember { mutableStateOf(service?.name ?: "") }
    var description by remember { mutableStateOf(service?.description ?: "") }
    var price by remember { mutableStateOf(service?.price?.toString() ?: "") }
    var duration by remember { mutableStateOf(service?.durationMinutes?.toString() ?: "") }

    val title = if (service == null) {
        if (isBundle) "Create Package" else "New Service"
    } else {
        if (isBundle) "Edit Package" else "Edit Service"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isBundle) Icons.Default.CardGiftcard else Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = if (isBundle) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
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
                }
                
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text(if (isBundle) "Package Name (e.g. The Ultimate Refresh)" else "Service Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                if (isBundle) {
                    OutlinedTextField(
                        value = description, 
                        onValueChange = { description = it }, 
                        label = { Text("What's included? (e.g. Cut + Wash + Hot Towel Shave)") },
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
                            isBundle = isBundle
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
