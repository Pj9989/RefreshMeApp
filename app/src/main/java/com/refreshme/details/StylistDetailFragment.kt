package com.refreshme.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.refreshme.ui.theme.RefreshMeTheme

class StylistDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    StylistDetailScreen(
                        stylistId = arguments?.getString("stylistId") ?: "",
                        onBack = { findNavController().popBackStack() },
                        onBookClick = { /* TODO: Implement navigation */ },
                        onChatClick = { /* TODO: Implement navigation */ }
                    )
                }
            }
        }
    }
}