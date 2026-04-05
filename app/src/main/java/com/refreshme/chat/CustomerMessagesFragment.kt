package com.refreshme.chat

import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.refreshme.R
import com.refreshme.data.Conversation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment dedicated for the Customer's message view.
 * It provides the specific navigation logic for the customer's navigation graph.
 */
@AndroidEntryPoint
class CustomerMessagesFragment : MessagesFragment() {

    override fun onConversationClick(conversation: Conversation) {
        // Use standardized ID from XML
        findNavController().navigate(
            R.id.action_messages_to_chat,
            bundleOf("otherUserId" to conversation.id)
        )
    }
}