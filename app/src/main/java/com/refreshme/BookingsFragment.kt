package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BookingsFragment : Fragment() {
    
    // Get the ViewModel
    private val viewModel: BookingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BookingsScreen(
                            viewModel = viewModel,
                            onBack = { findNavController().navigateUp() },
                            onChatStylist = { stylistId ->
                                // Use standardized action ID
                                findNavController().navigate(
                                    R.id.action_bookings_to_chat,
                                    bundleOf("otherUserId" to stylistId)
                                )
                            },
                            onTrackStylist = { bookingId ->
                                try {
                                    findNavController().navigate(
                                        R.id.action_bookings_to_liveTracking,
                                        bundleOf("bookingId" to bookingId, "isStylist" to false)
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to track stylist", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCancelBooking = { bookingId ->
                                // Implement actual booking cancellation logic here using the ViewModel
                                viewModel.cancelBooking(bookingId)
                                Toast.makeText(context, "Cancellation requested for booking $bookingId. Processing refund...", Toast.LENGTH_LONG).show()
                            },
                            onBookAgain = { stylistId ->
                                try {
                                    findNavController().navigate(
                                        R.id.stylistDetailsFragment,
                                        bundleOf("stylistId" to stylistId)
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open stylist profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
