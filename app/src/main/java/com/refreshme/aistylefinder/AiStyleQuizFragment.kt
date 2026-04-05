package com.refreshme.aistylefinder

import android.os.Bundle
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
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.refreshme.R
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiStyleQuizFragment : Fragment() {

    private val viewModel: AiStyleQuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener("faceScanResult") { _, bundle ->
            val faceShape = bundle.getString("faceShape")
            if (faceShape != null) {
                viewModel.setFaceShape(faceShape)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                RefreshMeTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AiStyleQuizScreen(
                            viewModel = viewModel,
                            onBack = { findNavController().popBackStack() },
                            onScanFace = {
                                val navController = findNavController()
                                // Safety check: only navigate if we are currently on this screen
                                if (navController.currentDestination?.id == R.id.aiStyleQuizFragment) {
                                    navController.navigate(R.id.faceScanFragment)
                                }
                            },
                            onSubmit = {
                                val navController = findNavController()
                                // Safety check to prevent double-navigation crashes
                                if (navController.currentDestination?.id == R.id.aiStyleQuizFragment) {
                                    val request = viewModel.getQuizRequest()
                                    if (request != null) {
                                        // FIXED: Navigate to destination ID directly instead of Action ID to avoid "Action not found" error
                                        navController.navigate(
                                            R.id.aiStyleResultsFragment,
                                            bundleOf(
                                                "gender" to request.gender,
                                                "vibe" to request.vibe,
                                                "frequency" to request.frequency,
                                                "finish" to request.finish,
                                                "faceShape" to request.faceShape
                                            )
                                        )
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
