package com.refreshme

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.auth.SignInActivity

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
                        // Per the design, this is the primary action. It can lead to a map or a list.
                        // The map seems like a good "browse" experience.
                        findNavController().navigate(R.id.action_dashboardFragment_to_mapFragment)
                    },
                    onMyBookings = {
                        // This is the secondary action, leading to the user's appointments.
                        findNavController().navigate(R.id.bookingsFragment)
                    },
                    onStylistClick = { stylist ->
                        val action =
                            DashboardFragmentDirections.actionDashboardFragmentToStylistProfileFragment(stylist.id)
                        findNavController().navigate(action)
                    }
                )
            }
        }
    }
}