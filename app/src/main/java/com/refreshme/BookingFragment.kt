package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.refreshme.databinding.FragmentBookingBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    private val args: BookingFragmentArgs by navArgs()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance() }
    private lateinit var paymentSheet: PaymentSheet

    private val selectedCalendar = Calendar.getInstance()
    private var servicePrice = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)

        PaymentConfiguration.init(
            requireContext(),
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        binding.selectDateButton.setOnClickListener {
            showDatePicker()
        }

        binding.selectTimeButton.setOnClickListener {
            showTimePicker()
        }

        binding.confirmBookingButton.setOnClickListener {
            createBookingPaymentIntent()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener {
            selectedCalendar.timeInMillis = it
            updateSelectedDateText()
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedCalendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedCalendar.set(Calendar.MINUTE, timePicker.minute)
            updateSelectedTimeText()
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun updateSelectedDateText() {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        binding.selectedDateText.text = "Selected Date: ${sdf.format(selectedCalendar.time)}"
    }

    private fun updateSelectedTimeText() {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.selectedTimeText.text = "Selected Time: ${sdf.format(selectedCalendar.time)}"
    }

    private fun createBookingPaymentIntent() {
        lifecycleScope.launch {
            try {
                // For simplicity, let's assume a fixed service price
                servicePrice = 50.0
                val data = hashMapOf(
                    "stylistId" to args.stylistId,
                    "userId" to auth.currentUser?.uid,
                    "amount" to (servicePrice * 100).toLong() // Amount in cents
                )

                val result = functions
                    .getHttpsCallable("createBookingPaymentIntent")
                    .call(data)
                    .await()

                val response = result.data as? HashMap<String, Any>
                val clientSecret = response?.get("clientSecret") as? String

                if (clientSecret != null) {
                    paymentSheet.presentWithPaymentIntent(
                        clientSecret,
                        PaymentSheet.Configuration(
                            merchantDisplayName = "RefreshMe"
                        )
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error creating payment intent: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(context, "Payment failed: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
            is PaymentSheetResult.Completed -> {
                createBooking()
            }
        }
    }

    private fun createBooking() {
        val booking = hashMapOf(
            "stylistId" to args.stylistId,
            "userId" to auth.currentUser?.uid,
            "date" to selectedCalendar.time,
            "status" to "confirmed",
            "price" to servicePrice
        )

        firestore.collection("bookings").add(booking)
            .addOnSuccessListener {
                Toast.makeText(context, "Booking successful!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}