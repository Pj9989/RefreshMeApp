package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.refreshme.databinding.FragmentFilterBottomSheetBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StylistListViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupVibeChips()
        setupSpecialtyChips()
        setupRatingFilter()
        setupAvailabilityFilter()
        setupClearButton()
    }

    private fun setupVibeChips() {
        val vibes = listOf("Urban", "Classic", "Luxury", "Quiet", "Trendy", "Hip-Hop", "Executive", "Artistic", "Fast")
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Initial load
                val currentFilters = viewModel.vibeFilters.value
                binding.vibeChipGroup.removeAllViews()
                vibes.forEach { vibe ->
                    val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter).apply {
                        text = vibe
                        isCheckable = true
                        isChecked = currentFilters.contains(vibe)
                        setOnCheckedChangeListener { _, isChecked ->
                            // We need to fetch the latest value inside the listener to avoid stale state
                            val current = viewModel.vibeFilters.value.toMutableList()
                            if (isChecked) {
                                if (!current.contains(vibe)) {
                                    current.add(vibe)
                                    viewModel.filterByVibes(current)
                                }
                            } else {
                                if (current.remove(vibe)) {
                                    viewModel.filterByVibes(current)
                                }
                            }
                        }
                    }
                    binding.vibeChipGroup.addView(chip)
                }
            }
        }
    }

    private fun setupSpecialtyChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // We observe specialties to populate the chips
                viewModel.specialties.collect { specialties ->
                    binding.specialtyChipGroup.removeAllViews()
                    val currentFilters = viewModel.specialtyFilters.value
                    
                    specialties.forEach { specialty ->
                        val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Filter).apply {
                            text = specialty
                            isCheckable = true
                            isChecked = currentFilters.contains(specialty)
                            setOnCheckedChangeListener { _, isChecked ->
                                val current = viewModel.specialtyFilters.value.toMutableList()
                                if (isChecked) {
                                    if (!current.contains(specialty)) {
                                        current.add(specialty)
                                        viewModel.filterBySpecialties(current)
                                    }
                                } else {
                                    if (current.remove(specialty)) {
                                        viewModel.filterBySpecialties(current)
                                    }
                                }
                            }
                        }
                        binding.specialtyChipGroup.addView(chip)
                    }
                }
            }
        }
    }

    private fun setupRatingFilter() {
        binding.ratingBar.rating = viewModel.ratingFilter.value ?: 0f
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            viewModel.setRatingFilter(rating)
        }
    }

    private fun setupAvailabilityFilter() {
        // At Home / Mobile
        // Note: Using launch/collect for StateFlow initial values is safer than .value access in onCreate
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.atHomeService.collect { isChecked ->
                // Avoid infinite loop if the listener triggers the update
                if (binding.chipAtHomeService.isChecked != isChecked) {
                    binding.chipAtHomeService.isChecked = isChecked
                }
            }
        }
        binding.chipAtHomeService.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAtHomeService(isChecked)
        }

        // Flash Deals
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.hasFlashDealFilter.collect { isChecked ->
                if (binding.chipFlashDeals.isChecked != isChecked) {
                    binding.chipFlashDeals.isChecked = isChecked
                }
            }
        }
        binding.chipFlashDeals.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFlashDealFilter(isChecked)
        }

        // Late Night / 24/7
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lateNightService.collect { isChecked ->
                if (binding.chipLateNight.isChecked != isChecked) {
                    binding.chipLateNight.isChecked = isChecked
                }
            }
        }
        binding.chipLateNight.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setLateNightService(isChecked)
        }
    }

    private fun setupClearButton() {
        binding.clearFiltersButton.setOnClickListener {
            viewModel.clearFilters()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}