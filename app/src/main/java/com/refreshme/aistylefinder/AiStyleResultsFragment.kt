package com.refreshme.aistylefinder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.refreshme.R
import com.refreshme.databinding.FragmentAiStyleResultsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AiStyleResultsFragment : Fragment() {

    private var _binding: FragmentAiStyleResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AiStyleResultsViewModel by viewModels()
    private val args: AiStyleResultsFragmentArgs by navArgs()
    
    private val adapter = AiRecommendationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiStyleResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        
        // Ensure quiz results are processed
        viewModel.processQuizResults(args.gender, args.vibe, args.frequency, args.finish, args.faceShape)

        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvRecommendations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecommendations.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnShare.setOnClickListener {
            shareRecommendations()
        }

        binding.btnGetResults.setOnClickListener {
            val specialtyIds = viewModel.getSpecialtyIdsArray()
            findNavController().navigate(
                R.id.action_results_to_list,
                bundleOf("styleIds" to specialtyIds)
            )
        }
    }

    private fun shareRecommendations() {
        val recommendations = viewModel.recommendations.value
        if (recommendations.isEmpty()) return

        val shareText = StringBuilder()
        shareText.append("Check out my AI-generated style recommendations from RefreshMe!\n\n")
        
        recommendations.forEach { rec ->
            shareText.append("✨ ${rec.styleName}\n")
            shareText.append("${rec.reasoning}\n\n")
        }
        
        shareText.append("Get your own personalized style guide at: https://refreshme-74f79.web.app/")

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "My RefreshMe Style Recommendations")
            putExtra(Intent.EXTRA_TEXT, shareText.toString())
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share my Style Guide"))
    }

    private fun updateResultsUI() {
        val recommendations = viewModel.recommendations.value
        if (recommendations.isNotEmpty()) {
            adapter.updateData(recommendations)
            binding.rvRecommendations.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
            binding.btnShare.visibility = View.VISIBLE
            binding.btnGetResults.visibility = View.VISIBLE
        } else {
            binding.rvRecommendations.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.btnShare.visibility = View.GONE
            binding.btnGetResults.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnGetResults.isEnabled = !isLoading
                if (!isLoading) {
                    updateResultsUI()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                if (error != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.text = "Recommendation Engine (Offline Mode)\nShowing local results."
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.btnGetResults.isEnabled = true
                    updateResultsUI()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.matchingStylists.collect { stylists ->
                binding.btnGetResults.text = if (stylists.isNotEmpty()) {
                    "Find ${stylists.size} Stylists For These"
                } else {
                    "Find Stylists For These"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}