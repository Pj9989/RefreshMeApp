package com.refreshme

import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.refreshme.details.StylistDetailViewModel
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StylistDetailsFragment : Fragment() {

    private val args: StylistDetailsFragmentArgs by navArgs()
    private val viewModel: StylistDetailViewModel by viewModels() 
    
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    private val stylistId: String by lazy { 
        args.stylistId.ifBlank { auth.currentUser?.uid ?: "" }
    }

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
                        EnhancedStylistProfileScreen(
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
        viewModel.getStylist(stylistId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }
}