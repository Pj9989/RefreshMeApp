package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.refreshme.ui.theme.RefreshMeTheme

@AndroidEntryPoint
class DashboardFragment : Fragment() {

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
                        DashboardScreen(
                            onFindStylist = {
                                findNavController().navigate(R.id.mapFragment)
                            },
                            onMyBookings = {
                                findNavController().navigate(R.id.bookingsFragment)
                            },
                            onStylistClick = { stylist ->
                                val navController = findNavController()
                                if (navController.currentDestination?.id == R.id.homeFragment ||
                                    navController.currentDestination?.id == R.id.dashboardFragment
                                ) {
                                    navController.navigate(
                                        R.id.stylistDetailsFragment,
                                        bundleOf("stylistId" to stylist.id)
                                    )
                                }
                            },
                            onVirtualTryOn = {
                                findNavController().navigate(R.id.virtualTryOnFragment)
                            }
                        )
                    }
                }
            }
        }
    }
}
