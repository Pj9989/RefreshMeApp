package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.refreshme.details.StylistDetailScreen
import com.refreshme.ui.theme.RefreshMeTheme

class StylistProfileFragment : Fragment() {

    private val args: StylistProfileFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    StylistDetailScreen(
                        stylistId = args.stylistId,
                        onBack = { findNavController().popBackStack() },
                        onBookClick = { stylistId ->
                            val action = StylistProfileFragmentDirections.actionStylistProfileFragmentToBookingFragment(stylistId)
                            findNavController().navigate(action)
                        },
                        onChatClick = { stylistId ->
                            val action = StylistProfileFragmentDirections.actionStylistProfileFragmentToChatFragment(stylistId)
                            findNavController().navigate(action)
                        }
                    )
                }
            }
        }
    }
}