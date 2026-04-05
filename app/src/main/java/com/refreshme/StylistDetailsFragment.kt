package com.refreshme

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.data.BookingRepository
import com.refreshme.data.Service
import com.refreshme.details.StylistDetailScreen
import com.refreshme.details.StylistDetailViewModel
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StylistDetailsFragment : Fragment() {

    private val args: StylistDetailsFragmentArgs by navArgs()
    private val viewModel: StylistDetailViewModel by viewModels() 
    
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var composeView: ComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup shared element transition
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val stylistId = if (args.stylistId.isBlank()) {
            auth.currentUser?.uid ?: ""
        } else {
            args.stylistId
        }

        return ComposeView(requireContext()).also { view ->
            composeView = view 
            
            // Set transition name on the entire view for a smooth "pop" effect
            view.transitionName = "stylist_image_$stylistId"

            view.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner.lifecycle)
            )
            view.setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        StylistDetailScreen(
                            stylistId = stylistId,
                            viewModel = viewModel,
                            onBack = { findNavController().popBackStack() },
                            onBookClick = { id ->
                                // Use standardized ID
                                findNavController().navigate(
                                    R.id.action_details_to_booking,
                                    bundleOf("stylistId" to id)
                                )
                            },
                            onChatClick = { id ->
                                // Use standardized ID
                                findNavController().navigate(
                                    R.id.action_details_to_chat,
                                    bundleOf("otherUserId" to id)
                                )
                            },
                            onServiceClick = { service ->
                                // Use standardized ID
                                findNavController().navigate(
                                    R.id.action_details_to_booking,
                                    bundleOf(
                                        "stylistId" to stylistId,
                                        "serviceName" to service.name,
                                        "servicePrice" to service.price.toFloat()
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val stylistId = if (args.stylistId.isBlank()) {
            auth.currentUser?.uid ?: ""
        } else {
            args.stylistId
        }
        
        viewModel.getStylist(stylistId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }
}