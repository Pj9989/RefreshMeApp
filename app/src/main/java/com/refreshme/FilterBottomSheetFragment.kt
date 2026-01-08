package com.refreshme

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.refreshme.databinding.FragmentFilterBottomSheetBinding
import kotlinx.coroutines.launch

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StylistListViewModel

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
        viewModel = ViewModelProvider(requireActivity()).get(StylistListViewModel::class.java)

        setupSpecialtyChips()
        setupRatingFilter()
        setupAvailabilityFilter()
        setupClearButton()
    }

    private fun setupSpecialtyChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.specialties.collect { specialties ->
                    binding.specialtyChipGroup.removeAllViews()
                    specialties.forEach { specialty ->
                        val chip = Chip(requireContext()).apply {
                            text = specialty
                            isCheckable = true
                            isChecked = viewModel.specialtyFilter.value == specialty
                            setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    viewModel.setSpecialtyFilter(specialty)
                                } else if (viewModel.specialtyFilter.value == specialty) {
                                    viewModel.setSpecialtyFilter(null)
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
        binding.chipAtHomeService.isChecked = viewModel.atHomeService.value
        binding.chipAtHomeService.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAtHomeService(isChecked)
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
