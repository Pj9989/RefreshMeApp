package com.refreshme.stylist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.refreshme.databinding.FragmentSubscriptionBinding

/**
 * Subscription system removed. This fragment now redirects stylists to set up
 * their Stripe Connect payout account instead.
 */
class SubscriptionFragment : Fragment() {

    private var _binding: FragmentSubscriptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionBinding.inflate(inflater, container, false)

        binding.manageSubscriptionButton.setOnClickListener {
            startActivity(Intent(requireContext(), ManagePayoutsActivity::class.java))
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}