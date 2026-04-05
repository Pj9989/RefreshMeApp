package com.refreshme.stylist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.refreshme.R

class StylistEarningsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stylist_earnings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the "View Payout History" button
        val viewPayoutHistoryButton = view.findViewById<Button>(R.id.viewPayoutHistoryButton)

        // Set the click listener
        viewPayoutHistoryButton.setOnClickListener {
            // Navigate to the Payouts management screen (ManagePayoutsActivity)
            val intent = Intent(requireContext(), ManagePayoutsActivity::class.java)
            startActivity(intent)
        }
    }
}