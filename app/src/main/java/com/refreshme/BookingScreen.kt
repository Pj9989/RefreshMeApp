package com.refreshme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.refreshme.booking.BookingState
import com.refreshme.booking.NewBookingViewModel
import com.refreshme.data.Service
import java.util.Date

@Composable
fun BookingScreen(
    stylistId: String,
    onBack: () -> Unit,
    viewModel: NewBookingViewModel = viewModel()
) {
    val stylist by viewModel.stylist.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val bookingState by viewModel.bookingState.collectAsState()

    LaunchedEffect(stylistId) {
        viewModel.fetchStylist(stylistId)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Book an Appointment", style = MaterialTheme.typography.headlineMedium)

        stylist?.let {
            Text("with ${it.name}", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select a Service", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(it.services ?: emptyList()) { service ->
                    ServiceItem(
                        service = service,
                        isSelected = service == selectedService,
                        onServiceSelected = { viewModel.selectService(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date picker placeholder
            Button(onClick = { viewModel.selectDate(Date()) }) {
                Text(selectedDate?.toString() ?: "Select a Date")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.createBooking() },
                enabled = selectedService != null && selectedDate != null
            ) {
                Text("Confirm Booking")
            }

            when (val state = bookingState) {
                is BookingState.Loading -> CircularProgressIndicator()
                is BookingState.Success -> {
                    Text("Booking successful!")
                    // Consider navigating back or to a confirmation screen
                }
                is BookingState.Error -> Text(state.message)
                else -> {}
            }
        } ?: run {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun ServiceItem(
    service: Service,
    isSelected: Boolean,
    onServiceSelected: (Service) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onServiceSelected(service) }
            .padding(8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onServiceSelected(service) }
        )
        Column {
            Text(service.name)
            Text("$${service.price} (${service.duration} mins)")
        }
    }
}