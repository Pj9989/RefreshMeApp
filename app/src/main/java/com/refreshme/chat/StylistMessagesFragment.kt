package com.refreshme.chat

import androidx.navigation.fragment.findNavController
import com.refreshme.data.Conversation
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment dedicated for the Stylist's message view.
 */
@AndroidEntryPoint
class StylistMessagesFragment : MessagesFragment() {

    override fun onConversationClick(conversation: Conversation) {
        // Use navigation action defined in stylist_nav_graph.xml
        // SafeArgs generates the directions based on the XML ID
        val action = StylistMessagesFragmentDirections.actionMessagesFragmentToChatFragment(
            otherUserId = conversation.otherUserId
        )
        findNavController().navigate(action)
    }
}