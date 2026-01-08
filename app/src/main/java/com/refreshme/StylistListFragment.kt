package com.refreshme

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.ui.theme.RefreshMeTheme

class StylistListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RefreshMeTheme {
                    StylistListRoute(
                        onStylistClick = { stylist ->
                            val action =
                                StylistListFragmentDirections.actionStylistListFragmentToStylistProfileFragment(
                                    stylist.id
                                )
                            findNavController().navigate(action)
                        }
                    )
                }
            }
        }
    }
}