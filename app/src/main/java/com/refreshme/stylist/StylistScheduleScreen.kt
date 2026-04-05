package com.refreshme.stylist

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Stylist
import com.refreshme.data.WorkingHours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

data class DaySchedule(
    val dayOfWeek: Int, // 1 = Sunday, 2 = Monday, ..., 7 = Saturday
    val dayName: String,
    val isOpen: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "17:00"
)

class ScheduleViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _schedule = MutableStateFlow<List<DaySchedule>>(emptyList())
    val schedule: StateFlow<List<DaySchedule>> = _schedule
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadSchedule()
    }

    private fun loadSchedule() {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("stylists").document(uid).get().await()
                val stylist = snapshot.toObject(Stylist::class.java)
                
                val workingHoursList = stylist?.workingHours ?: emptyList()
                
                val hoursMap = workingHoursList.associate { 
                    it.dayOfWeek to (it.startTime to it.endTime)
                }

                val days = listOf(
                    Calendar.SUNDAY to "Sunday",
                    Calendar.MONDAY to "Monday",
                    Calendar.TUESDAY to "Tuesday",
                    Calendar.WEDNESDAY to "Wednesday",
                    Calendar.THURSDAY to "Thursday",
                    Calendar.FRIDAY to "Friday",
                    Calendar.SATURDAY to "Saturday"
                )

                _schedule.value = days.map { (dayInt, name) ->
                    val existing = hoursMap[dayInt]
                    DaySchedule(
                        dayOfWeek = dayInt,
                        dayName = name,
                        isOpen = existing != null,
                        startTime = existing?.first ?: "09:00",
                        endTime = existing?.second ?: "17:00"
                    )
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateDay(updatedDay: DaySchedule) {
        val currentList = _schedule.value.toMutableList()
        val index = currentList.indexOfFirst { it.dayOfWeek == updatedDay.dayOfWeek }
        if (index != -1) {
            currentList[index] = updatedDay
            _schedule.value = currentList
        }
    }
    
    fun saveSchedule(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val workingHoursToSave = _schedule.value.filter { it.isOpen }.map { day ->
                    WorkingHours(
                        dayOfWeek = day.dayOfWeek,
                        startTime = day.startTime,
                        endTime = day.endTime
                    )
                }
                
                firestore.collection("stylists").document(uid)
                    .update("workingHours", workingHoursToSave)
                    .await()
                
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save schedule")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistScheduleScreen(
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val schedule by viewModel.schedule.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Availability & Hours", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(schedule) { day ->
                            DayScheduleItem(
                                day = day,
                                onUpdate = { updatedDay ->
                                    viewModel.updateDay(updatedDay)
                                }
                            )
                        }
                    }
                    
                    Button(
                        onClick = { 
                            viewModel.saveSchedule(
                                onSuccess = {
                                    Toast.makeText(context, "Schedule saved successfully", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Save Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DayScheduleItem(day: DaySchedule, onUpdate: (DaySchedule) -> Unit) {
    val context = LocalContext.current
    
    fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            false 
        ).show()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day.dayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                Switch(
                    checked = day.isOpen,
                    onCheckedChange = { onUpdate(day.copy(isOpen = it)) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            if (day.isOpen) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimePickerChip(
                        label = "From", 
                        time = day.startTime, 
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTimePicker(day.startTime) { newTime ->
                                onUpdate(day.copy(startTime = newTime))
                            }
                        }
                    )
                    Text("-", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimePickerChip(
                        label = "To", 
                        time = day.endTime, 
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTimePicker(day.endTime) { newTime ->
                                onUpdate(day.copy(endTime = newTime))
                            }
                        }
                    )
                }
            } else {
                Text("Closed", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun TimePickerChip(label: String, time: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(time, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
            }
        }
    }
}