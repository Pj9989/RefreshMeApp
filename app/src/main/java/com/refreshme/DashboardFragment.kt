package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DashboardScreen(
                    onFindStylist = {
                        findNavController().navigate(R.id.action_dashboardFragment_to_mapFragment)
                    },
                    onMyBookings = {
                        findNavController().navigate(R.id.bookingsFragment)
                    },
                    onStylistClick = { stylist ->
                        val action =
                            DashboardFragmentDirections.actionDashboardFragmentToStylistProfileFragment(stylist.id)
                        findNavController().navigate(action)
                    },
                    onVirtualTryOn = {
                        findNavController().navigate(R.id.action_dashboardFragment_to_virtualTryOnFragment)
                    }
                )
            }
        }
    }
}
