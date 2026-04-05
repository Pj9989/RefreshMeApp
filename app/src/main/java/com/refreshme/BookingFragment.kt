package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.refreshme.booking.BookingScreen
import com.refreshme.booking.NewBookingViewModel
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@AndroidEntryPoint
class BookingFragment : Fragment() {

    private val args: BookingFragmentArgs by navArgs()
    private val viewModel: NewBookingViewModel by viewModels()

    private lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Stripe using the configuration from build.gradle
        PaymentConfiguration.init(
            requireContext(),
            BuildConfig.STRIPE_PUBLISHABLE_KEY
        )
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
                            BookingScreen(
                                stylistId = args.stylistId,
                                serviceName = args.serviceName,
                                servicePrice = args.servicePrice,
                                onBack = { findNavController().popBackStack() },
                                onDateTimeClick = { showDateTimePicker() },
                                onAsapClick = {
                                    val calendar = Calendar.getInstance()
                                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                                    viewModel.selectDate(calendar.time)
                                },
                                onPaymentRequired = { clientSecret, _ ->
                                    val config = PaymentSheet.Configuration(
                                        merchantDisplayName = "RefreshMe",
                                        allowsDelayedPaymentMethods = true
                                    )
                                    paymentSheet.presentWithPaymentIntent(clientSecret, config)
                                },
                                onBookingSuccess = {
                                    Toast.makeText(requireContext(), "Booking Confirmed!", Toast.LENGTH_SHORT).show()
                                    findNavController().popBackStack()
                                    findNavController().navigate(R.id.bookingsFragment)
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showDateTimePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Appointment Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Time")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "Please select a future time", Toast.LENGTH_SHORT).show()
                    return@addOnPositiveButtonClickListener
                }

                viewModel.selectDate(calendar.time)
            }

            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                viewModel.resetState()
                Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                viewModel.resetState()
                Toast.makeText(context, "Payment failed: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
            is PaymentSheetResult.Completed -> {
                Toast.makeText(context, "Payment Done. Finalizing...", Toast.LENGTH_SHORT).show()
                viewModel.finalizeBooking(requireContext())
            }
        }
    }
}
