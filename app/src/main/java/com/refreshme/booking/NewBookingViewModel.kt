package com.refreshme.booking

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.data.BookingStatus
import com.refreshme.data.Service
import com.refreshme.data.Stylist
import com.refreshme.reminders.SmartRebookWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class NewBookingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    private val _stylist = MutableStateFlow<Stylist?>(null)
    val stylist: StateFlow<Stylist?> = _stylist

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService: StateFlow<Service?> = _selectedService

    private val _selectedAddOns = MutableStateFlow<List<Service>>(emptyList())
    val selectedAddOns: StateFlow<List<Service>> = _selectedAddOns

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate
    
    private val _isMobileBooking = MutableStateFlow(false)
    val isMobileBooking: StateFlow<Boolean> = _isMobileBooking

    // Group & Special Event State
    private val _isEvent = MutableStateFlow(false)
    val isEvent: StateFlow<Boolean> = _isEvent

    private val _groupSize = MutableStateFlow(1)
    val groupSize: StateFlow<Int> = _groupSize

    private val _eventType = MutableStateFlow("")
    val eventType: StateFlow<String> = _eventType

    // The Modern Salon Experience
    private val _isSilentAppointment = MutableStateFlow(false)
    val isSilentAppointment: StateFlow<Boolean> = _isSilentAppointment

    private val _isSensoryFriendly = MutableStateFlow(false)
    val isSensoryFriendly: StateFlow<Boolean> = _isSensoryFriendly

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState
    
    private val _waitlistState = MutableStateFlow<WaitlistState>(WaitlistState.Idle)
    val waitlistState: StateFlow<WaitlistState> = _waitlistState

    private var currentBookingId: String?
        get() = savedStateHandle.get<String>("currentBookingId")
        set(value) {
            savedStateHandle["currentBookingId"] = value
        }

    fun fetchStylist(stylistId: String) {
        if (stylistId.isBlank()) return
        
        viewModelScope.launch {
            try {
                val document = db.collection("stylists").document(stylistId).get().await()
                if (document.exists()) {
                    val s = document.toObject(Stylist::class.java)?.copy(id = document.id)
                    _stylist.value = s
                    
                    if (s?.services.isNullOrEmpty() && _selectedService.value == null) {
                        selectService(Service(name = "General Consultation", price = 45.0, durationMinutes = 30))
                    }
                }
            } catch (e: Exception) {
                _bookingState.value = BookingState.Error("Failed to load stylist details.")
            }
        }
    }

    fun selectService(service: Service) {
        _selectedService.value = service
        // If they pick a new primary service, make sure it's not in the add-ons list anymore
        _selectedAddOns.update { currentAddOns ->
            val updated = currentAddOns.toMutableList()
            updated.removeAll { it.name == service.name }
            updated
        }
        
        if (_bookingState.value is BookingState.Error) {
            _bookingState.value = BookingState.Idle
        }
    }

    fun toggleAddOn(service: Service) {
        _selectedAddOns.update { currentAddOns ->
            val updated = currentAddOns.toMutableList()
            if (updated.any { it.name == service.name }) {
                updated.removeAll { it.name == service.name }
            } else {
                updated.add(service)
            }
            updated
        }
    }

    fun selectDate(date: Date) {
        _selectedDate.value = date
        if (_bookingState.value is BookingState.Error) {
            _bookingState.value = BookingState.Idle
        }
    }
    
    fun setMobileBooking(isMobile: Boolean) {
        _isMobileBooking.value = isMobile
    }

    fun toggleEventBooking(enabled: Boolean) {
        _isEvent.value = enabled
        if (!enabled) {
            _groupSize.value = 1
            _eventType.value = ""
        }
    }

    fun setGroupSize(size: Int) {
        _groupSize.value = size.coerceAtLeast(1)
    }

    fun setEventType(type: String) {
        _eventType.value = type
    }

    fun toggleSilentAppointment(enabled: Boolean) {
        _isSilentAppointment.value = enabled
    }

    fun toggleSensoryFriendly(enabled: Boolean) {
        _isSensoryFriendly.value = enabled
    }

    fun createBooking(isEmergencyAsap: Boolean = false) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            val stylistValue = _stylist.value
            val serviceValue = _selectedService.value
            val addOnsValue = _selectedAddOns.value
            val dateValue = _selectedDate.value
            val mobileBooking = _isMobileBooking.value
            val isEventBooking = _isEvent.value
            val eventGroupSize = _groupSize.value
            val eventTypeStr = _eventType.value
            val silentBooking = _isSilentAppointment.value
            val sensoryBooking = _isSensoryFriendly.value

            if (currentUser == null) {
                _bookingState.value = BookingState.Error("You must be logged in to book.")
                return@launch
            }
            if (stylistValue == null) {
                _bookingState.value = BookingState.Error("Stylist information is missing.")
                return@launch
            }
            if (serviceValue == null || serviceValue.name.isBlank()) {
                _bookingState.value = BookingState.Error("Please select a service first.")
                return@launch
            }
            if (dateValue == null) {
                _bookingState.value = BookingState.Error("Please select a date and time.")
                return@launch
            }
            
            _bookingState.value = BookingState.Loading

            // SECURITY/UX CHECK: Only enforce online status if it's an emergency/ASAP booking
            if (isEmergencyAsap) {
                try {
                    val freshStylist = db.collection("stylists").document(stylistValue.id).get().await()
                    val isOnline = freshStylist.getBoolean("online") == true
                    if (!isOnline) {
                        _bookingState.value = BookingState.Error("This stylist just went offline. Please choose someone else for an ASAP appointment.")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e("NewBookingVM", "Failed to verify online status", e)
                }
            }

            try {
                // Base price calculation: (Service + AddOns) * GroupSize
                val perPersonPrice = serviceValue.price + addOnsValue.sumOf { it.price }
                var finalPrice = perPersonPrice * eventGroupSize
                
                var travelFeeApplied = 0.0
                if (mobileBooking && (stylistValue.atHomeServiceFee ?: 0.0) > 0) {
                    travelFeeApplied = stylistValue.atHomeServiceFee!!
                    finalPrice += travelFeeApplied
                }

                var emergencyFeeApplied = 0.0
                if (isEmergencyAsap && (stylistValue.emergencyFee ?: 0.0) > 0) {
                    emergencyFeeApplied = stylistValue.emergencyFee!!
                    finalPrice += emergencyFeeApplied
                }

                // Construct a nice name that includes the add-ons and group info
                var fullServiceName = serviceValue.name
                if (addOnsValue.isNotEmpty()) {
                    fullServiceName += " + " + addOnsValue.joinToString(", ") { it.name }
                }
                if (isEventBooking && eventGroupSize > 1) {
                    fullServiceName = "Group of $eventGroupSize: $fullServiceName"
                }

                val data = hashMapOf(
                    "userId" to currentUser.uid,
                    "customerId" to currentUser.uid, 
                    "customerName" to (currentUser.displayName ?: "Client"),
                    "customerPhotoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "stylistId" to stylistValue.id,
                    "stylistName" to stylistValue.name,
                    "stripeAccountId" to (stylistValue.stripeAccountId ?: ""),
                    "stylistPhotoUrl" to (stylistValue.profileImageUrl ?: ""),
                    "serviceName" to fullServiceName,
                    "servicePrice" to finalPrice,
                    "emergencyFeeApplied" to emergencyFeeApplied,
                    "travelFeeApplied" to travelFeeApplied,
                    "bookingDate" to dateValue.time, 
                    "date" to dateValue.time,        
                    "currency" to "usd",
                    "isMobile" to mobileBooking,
                    "isEvent" to isEventBooking,
                    "groupSize" to eventGroupSize,
                    "eventType" to eventTypeStr,
                    "isSilentAppointment" to silentBooking,
                    "isSensoryFriendly" to sensoryBooking,
                    "status" to BookingStatus.REQUESTED.name
                )

                val result = functions
                    .getHttpsCallable("createBookingPaymentIntent")
                    .call(data)
                    .await()
                
                val responseData = result.data as? Map<*, *>
                val clientSecret = responseData?.get("clientSecret") as? String
                val bookingId = responseData?.get("bookingId") as? String ?: responseData?.get("id") as? String
                val depositAmount = (responseData?.get("depositAmount") as? Number)?.toDouble() ?: (finalPrice * 0.2)

                if (clientSecret != null) {
                    currentBookingId = bookingId
                    _bookingState.value = BookingState.RequiresPayment(
                        bookingId = bookingId ?: "",
                        clientSecret = clientSecret,
                        depositAmount = depositAmount
                    )
                } else {
                    _bookingState.value = BookingState.Error("Payment setup failed. Please try again.")
                }

            } catch (e: Exception) {
                Log.e("NewBookingVM", "Booking creation failed", e)
                _bookingState.value = BookingState.Error("Booking failed: ${e.localizedMessage}")
            }
        }
    }

    fun finalizeBooking(context: Context) {
        val bookingId = currentBookingId
        if (bookingId == null) {
            _bookingState.value = BookingState.Success
            return
        }
        
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any>(
                    "status" to BookingStatus.DEPOSIT_PAID.name,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                
                db.collection("bookings").document(bookingId)
                    .update(updates)
                    .await()
                
                // Smart Reminder Setup
                scheduleSmartReminder(context)
                
                _bookingState.value = BookingState.Success
            } catch (e: Exception) {
                Log.e("NewBookingVM", "Finalize DB update failed", e)
                _bookingState.value = BookingState.Success
            }
        }
    }

    private fun scheduleSmartReminder(context: Context) {
        val serviceName = _selectedService.value?.name ?: return
        val stylistName = _stylist.value?.name ?: "your stylist"
        val stylistId = _stylist.value?.id ?: return
        val appointmentDate = _selectedDate.value ?: return

        // 1. Calculate how many days UNTIL the appointment
        val daysUntilAppt = ((appointmentDate.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

        // 2. Decide how many days AFTER the appointment they should be reminded based on the service type
        val serviceLower = serviceName.lowercase()
        val daysAfterAppt = when {
            serviceLower.contains("fade") || serviceLower.contains("taper") -> 14L
            serviceLower.contains("line") || serviceLower.contains("edge") -> 10L
            serviceLower.contains("color") -> 45L
            else -> 28L // Standard haircut
        }

        // 3. Total delay = days until appointment + days after appointment
        val totalDelayDays = daysUntilAppt + daysAfterAppt

        // 4. Enqueue the work
        val inputData = Data.Builder()
            .putString(SmartRebookWorker.KEY_STYLIST_NAME, stylistName)
            .putString(SmartRebookWorker.KEY_STYLIST_ID, stylistId)
            .putString(SmartRebookWorker.KEY_SERVICE_NAME, serviceName)
            .build()

        val rebookWork = OneTimeWorkRequestBuilder<SmartRebookWorker>()
            .setInitialDelay(totalDelayDays, TimeUnit.DAYS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(rebookWork)
        Log.d("SmartReminder", "Scheduled rebooking reminder for $totalDelayDays days from now.")
    }

    fun joinWaitlist(targetDate: Date) {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            val stylistValue = _stylist.value

            if (currentUser == null) {
                _waitlistState.value = WaitlistState.Error("You must be logged in to join the waitlist.")
                return@launch
            }
            if (stylistValue == null) {
                _waitlistState.value = WaitlistState.Error("Stylist information is missing.")
                return@launch
            }

            _waitlistState.value = WaitlistState.Loading

            try {
                val waitlistEntry = hashMapOf(
                    "userId" to currentUser.uid,
                    "userName" to (currentUser.displayName ?: "Client"),
                    "stylistId" to stylistValue.id,
                    "targetDate" to targetDate.time,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                // Add to a global 'waitlist' collection (or you can nest under the stylist)
                db.collection("waitlists").add(waitlistEntry).await()

                _waitlistState.value = WaitlistState.Success
            } catch (e: Exception) {
                Log.e("NewBookingVM", "Failed to join waitlist", e)
                _waitlistState.value = WaitlistState.Error("Failed to join waitlist: ${e.localizedMessage}")
            }
        }
    }
    
    fun resetWaitlistState() {
        _waitlistState.value = WaitlistState.Idle
    }

    fun resetState() {
        _bookingState.value = BookingState.Idle
    }
}

sealed class BookingState {
    object Idle : BookingState()
    object Loading : BookingState()
    data class RequiresPayment(val bookingId: String, val clientSecret: String, val depositAmount: Double?) : BookingState()
    object Success : BookingState()
    data class Error(val message: String) : BookingState()
}

sealed class WaitlistState {
    object Idle : WaitlistState()
    object Loading : WaitlistState()
    object Success : WaitlistState()
    data class Error(val message: String) : WaitlistState()
}