package com.refreshme.chat

import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.refreshme.auth.SignInActivity
import com.refreshme.data.Conversation
import com.refreshme.ui.theme.RefreshMeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Base Fragment to handle data loading for a list of conversations/messages, now using Jetpack Compose.
 */
@AndroidEntryPoint
abstract class MessagesFragment : Fragment() {

    protected val viewModel: MessagesViewModel by viewModels()

    abstract fun onConversationClick(conversation: Conversation)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
                        MessagesScreen(
                            viewModel = viewModel,
                            onConversationClick = { onConversationClick(it) },
                            onNavigateToSignIn = { navigateToSignIn() }
                        )
                    }
                }
            }
        }
    }

    private fun navigateToSignIn() {
        val intent = Intent(requireActivity(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}