package com.refreshme.stylist

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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.refreshme.R
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StylistDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
                            StylistDashboardScreen(
                                onNavigate = { event ->
                                    val navController = findNavController()
                                    try {
                                        when (event) {
                                            StylistDashboardEvent.UpcomingAppointments -> {
                                                navController.navigate(R.id.action_stylistDashboardFragment_to_stylistBookingsFragment)
                                            }
                                            StylistDashboardEvent.EarningsPayouts, StylistDashboardEvent.Analytics -> {
                                                navController.navigate(R.id.action_stylistDashboardFragment_to_payoutsEarningsFragment)
                                            }
                                            StylistDashboardEvent.ManageServices -> {
                                                navController.navigate(R.id.action_stylistDashboardFragment_to_manageServicesFragment)
                                            }
                                            StylistDashboardEvent.ProfileVerification -> {
                                                navController.navigate(R.id.action_stylistDashboardFragment_to_stylistProfileFragment)
                                            }
                                            StylistDashboardEvent.Availability -> {
                                                navController.navigate(R.id.action_stylistDashboardFragment_to_setAvailabilityFragment)
                                            }
                                            StylistDashboardEvent.Waitlist -> {
                                                navController.navigate(R.id.action_stylistDashboardFragment_to_stylistWaitlistFragment)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("StylistDashboard", "Navigation failed with action: ${e.message}")
                                        // Fallback to direct navigation if action fails for some reason
                                        try {
                                            when (event) {
                                                StylistDashboardEvent.UpcomingAppointments -> navController.navigate(R.id.stylistBookingsFragment)
                                                StylistDashboardEvent.EarningsPayouts, StylistDashboardEvent.Analytics -> navController.navigate(R.id.payoutsEarningsFragment)
                                                StylistDashboardEvent.ManageServices -> navController.navigate(R.id.manageServicesFragment)
                                                StylistDashboardEvent.ProfileVerification -> navController.navigate(R.id.stylistProfileFragment)
                                                StylistDashboardEvent.Availability -> navController.navigate(R.id.setAvailabilityFragment)
                                                StylistDashboardEvent.Waitlist -> navController.navigate(R.id.stylistWaitlistFragment)
                                            }
                                        } catch (e2: Exception) {
                                            android.util.Log.e("StylistDashboard", "Direct navigation also failed", e2)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}