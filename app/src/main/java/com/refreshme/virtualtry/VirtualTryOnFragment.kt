package com.refreshme.virtualtry

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.refreshme.BuildConfig
import com.refreshme.R
import com.refreshme.databinding.FragmentVirtualTryOnBinding
import kotlinx.coroutines.launch
import java.io.File

/**
 * VirtualTryOnFragment
 *
 * Provides the complete UI flow for the AI Virtual Hair Try-On feature:
 *
 * 1. User selects a selfie (camera or gallery)
 * 2. User picks hairstyle, ethnicity, and gender from dropdowns
 * 3. User taps "Generate My Look" → ViewModel converts image to Base64,
 *    builds the AI prompt, and calls Replicate API
 * 4. Result image is displayed side-by-side with the original selfie
 *
 * The Replicate API key is read from BuildConfig.REPLICATE_API_KEY,
 * which is injected at build time from gradle.properties (never hardcoded).
 */
class VirtualTryOnFragment : Fragment() {

    private var _binding: FragmentVirtualTryOnBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VirtualTryOnViewModel by viewModels()

    // ── Camera support ────────────────────────────────────────────────────────
    private var pendingCameraUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "Camera permission is required to take a selfie.", Toast.LENGTH_SHORT).show()
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { viewModel.onImageSelected(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVirtualTryOnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupClickListeners()
        observeUiState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupDropdowns() {
        // Hairstyle spinner
        val hairstyleAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            VirtualTryOnViewModel.HAIRSTYLE_OPTIONS
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerHairstyle.adapter = hairstyleAdapter
        binding.spinnerHairstyle.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    viewModel.onHairstyleSelected(VirtualTryOnViewModel.HAIRSTYLE_OPTIONS[pos])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }

        // Ethnicity spinner
        val ethnicityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            VirtualTryOnViewModel.ETHNICITY_OPTIONS
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerEthnicity.adapter = ethnicityAdapter
        binding.spinnerEthnicity.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    viewModel.onEthnicitySelected(VirtualTryOnViewModel.ETHNICITY_OPTIONS[pos])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }

        // Gender spinner
        val genderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            VirtualTryOnViewModel.GENDER_OPTIONS
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerGender.adapter = genderAdapter
        binding.spinnerGender.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    viewModel.onGenderSelected(VirtualTryOnViewModel.GENDER_OPTIONS[pos])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
    }

    private fun setupClickListeners() {
        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnChooseGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnGenerate.setOnClickListener {
            generateTryOn()
        }

        binding.btnTryAgain.setOnClickListener {
            viewModel.resetToIdle()
        }
    }

    // ── State Observation ─────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: VirtualTryOnViewModel.UiState) {
        when (state) {
            is VirtualTryOnViewModel.UiState.Idle -> {
                binding.groupPhotoActions.visibility = View.VISIBLE
                binding.groupSelections.visibility = View.VISIBLE
                binding.ivSelfiePreview.setImageResource(R.drawable.ic_person_placeholder)
                binding.ivSelfiePreview.visibility = View.VISIBLE
                binding.btnGenerate.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.groupResult.visibility = View.GONE
                binding.tvStatusMessage.visibility = View.GONE
                binding.btnTryAgain.visibility = View.GONE
            }

            is VirtualTryOnViewModel.UiState.ImageSelected -> {
                binding.groupPhotoActions.visibility = View.VISIBLE
                binding.groupSelections.visibility = View.VISIBLE
                binding.ivSelfiePreview.visibility = View.VISIBLE
                Glide.with(this).load(state.uri).centerCrop().into(binding.ivSelfiePreview)
                binding.btnGenerate.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.groupResult.visibility = View.GONE
                binding.tvStatusMessage.visibility = View.GONE
                binding.btnTryAgain.visibility = View.GONE
            }

            is VirtualTryOnViewModel.UiState.Loading -> {
                binding.groupPhotoActions.visibility = View.GONE
                binding.groupSelections.visibility = View.GONE
                binding.btnGenerate.visibility = View.GONE
                binding.progressBar.visibility = View.VISIBLE
                binding.groupResult.visibility = View.GONE
                binding.tvStatusMessage.text = "Generating your look… this takes about 30–60 seconds."
                binding.tvStatusMessage.visibility = View.VISIBLE
                binding.btnTryAgain.visibility = View.GONE
            }

            is VirtualTryOnViewModel.UiState.Success -> {
                binding.groupPhotoActions.visibility = View.GONE
                binding.groupSelections.visibility = View.GONE
                binding.btnGenerate.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.groupResult.visibility = View.VISIBLE
                binding.tvStatusMessage.visibility = View.GONE
                binding.btnTryAgain.visibility = View.VISIBLE

                // Load original selfie into left card
                Glide.with(this).load(state.selfieUri).centerCrop()
                    .into(binding.ivResultBefore)

                // Load AI-generated result into right card
                Glide.with(this).load(state.resultUrl).centerCrop()
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(binding.ivResultAfter)
            }

            is VirtualTryOnViewModel.UiState.Error -> {
                binding.groupPhotoActions.visibility = View.VISIBLE
                binding.groupSelections.visibility = View.VISIBLE
                binding.btnGenerate.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.groupResult.visibility = View.GONE
                binding.tvStatusMessage.text = "Error: ${state.message}"
                binding.tvStatusMessage.visibility = View.VISIBLE
                binding.btnTryAgain.visibility = View.GONE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "tryon_selfie_", ".jpg",
            requireContext().cacheDir
        )
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    private fun generateTryOn() {
        // Read the API key from BuildConfig (injected from gradle.properties at build time)
        // The user must add: REPLICATE_API_KEY=r8_xxxx to their gradle.properties
        val apiKey = BuildConfig.REPLICATE_API_KEY

        if (apiKey.isBlank() || apiKey == "null" || apiKey.startsWith("YOUR_")) {
            Toast.makeText(
                requireContext(),
                "Add your Replicate API key to gradle.properties as REPLICATE_API_KEY=r8_xxxx",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        viewModel.generateTryOn(apiKey)
    }
}
