package com.refreshme.details

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
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StylistDetailScreen(
                            stylistId = arguments?.getString("stylistId") ?: "",
                            onBack = { findNavController().popBackStack() },
                            onBookClick = { Toast.makeText(context, "Book feature not available from this view.", Toast.LENGTH_SHORT).show() },
                            onChatClick = { Toast.makeText(context, "Chat feature not available from this view.", Toast.LENGTH_SHORT).show() },
                            onServiceClick = { /* No-op in this fragment, as it's not the primary entry point. */ }
                        )
                    }
                }
            }
        }
    }
}