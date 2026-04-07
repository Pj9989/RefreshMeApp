package com.refreshme.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.refreshme.R
import com.refreshme.data.Stylist
import com.refreshme.databinding.FragmentHomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private val homeViewModel: HomeViewModel by viewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var liveAdapter: StylistPreviewAdapter
    private lateinit var recommendedAdapter: StylistPreviewAdapter

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getUserLocation()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupObservers()
        setupListeners()
        
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)
        
        checkLocationPermission()

        // Push map elements (like the Google logo) above our bottom floating UI
        binding.bottomContainer.addOnLayoutChangeListener { v, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom != oldBottom) {
                googleMap?.setPadding(0, 0, 0, v.height)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.userName.collectLatest { name ->
                        binding.greetingText.text = name
                    }
                }

                launch {
                    homeViewModel.isLoading.collectLatest { isLoading ->
                        if (isLoading) {
                            binding.shimmerLayout.startShimmer()
                            binding.shimmerLayout.visibility = View.VISIBLE
                            binding.liveTitle.visibility = View.GONE
                            binding.liveStylistsRecyclerView.visibility = View.GONE
                        } else {
                            binding.shimmerLayout.stopShimmer()
                            binding.shimmerLayout.visibility = View.GONE
                            binding.liveTitle.visibility = View.VISIBLE
                            binding.liveStylistsRecyclerView.visibility = View.VISIBLE
                        }
                    }
                }

                launch {
                    homeViewModel.styleVibe.collectLatest { vibe ->
                        if (vibe != null) {
                            binding.styleFinderSubtitle.text = "Keep it $vibe? View your recommendations or try something new."
                            binding.recommendedTitle.text = "Top Picks for your $vibe Vibe"
                        }
                    }
                }

                launch {
                    homeViewModel.stylists.collectLatest { stylists ->
                        liveAdapter.submitList(stylists)
                        updateMapMarkers(stylists)
                    }
                }

                launch {
                    homeViewModel.recommendedStylists.collectLatest { stylists ->
                        if (stylists.isNotEmpty()) {
                            binding.recommendedTitle.visibility = View.VISIBLE
                            binding.recommendedRecyclerView.visibility = View.VISIBLE
                            recommendedAdapter.submitList(stylists)
                        } else {
                            binding.recommendedTitle.visibility = View.GONE
                            binding.recommendedRecyclerView.visibility = View.GONE
                            recommendedAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Live Stylists
        liveAdapter = StylistPreviewAdapter { stylist, imageView ->
            navigateToStylistProfile(stylist.id, imageView)
        }
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.liveStylistsRecyclerView.layoutManager = layoutManager
        binding.liveStylistsRecyclerView.adapter = liveAdapter
        
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.liveStylistsRecyclerView)

        binding.liveStylistsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val centerView = snapHelper.findSnapView(layoutManager)
                    val pos = centerView?.let { layoutManager.getPosition(it) } ?: -1
                    if (pos != -1) {
                        homeViewModel.stylists.value.getOrNull(pos)?.let { stylist ->
                            stylist.location?.let { geoPoint ->
                                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(geoPoint.latitude, geoPoint.longitude)))
                            }
                        }
                    }
                }
            }
        })

        // Recommended Stylists
        recommendedAdapter = StylistPreviewAdapter { stylist, imageView ->
            navigateToStylistProfile(stylist.id, imageView)
        }
        binding.recommendedRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.recommendedRecyclerView.adapter = recommendedAdapter
    }

    private fun setupListeners() {
        binding.aiStyleFinderCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_quiz)
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getUserLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    homeViewModel.updateUserLocation(it)
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))
                    updateLocationText(it)
                }
            }
        } catch (_: SecurityException) {
        }
    }

    private fun updateLocationText(location: Location) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val locationString = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Current Location"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        binding.locationText.text = locationString
                    }
                }
            } catch (_: IOException) {
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setOnMarkerClickListener(this)
        
        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_gold)
            )
            if (success == false) {
                Log.e("HomeFragment", "Style parsing failed.")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Can't find style. Error: ", e)
        }

        // Push map elements (like the Google logo) above our bottom floating UI
        binding.bottomContainer.height.let { height ->
            if (height > 0) {
                googleMap?.setPadding(0, 0, 0, height)
            }
        }

        updateMapMarkers(homeViewModel.stylists.value)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val stylistId = marker.tag as? String
        stylistId?.let {
            navigateToStylistProfile(it, null)
        }
        return true
    }

    private fun updateMapMarkers(stylists: List<Stylist>) {
        googleMap?.clear()
        stylists.forEach { stylist ->
            stylist.location?.let { geoPoint ->
                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                val markerIcon = if (stylist.isAvailable == true) {
                    bitmapDescriptorFromVector(requireContext(), R.drawable.ic_marker_available)
                } else {
                    bitmapDescriptorFromVector(requireContext(), R.drawable.ic_marker_unavailable)
                }
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(stylist.name)
                        .icon(markerIcon)
                )
                marker?.tag = stylist.id
            }
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable?.intrinsicWidth ?: 0, vectorDrawable?.intrinsicHeight ?: 0, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable?.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun navigateToStylistProfile(stylistId: String, sharedImageView: ImageView?) {
        val trimmedId = stylistId.trim()
        if (trimmedId.isNotBlank()) {
            val extras = if (sharedImageView != null) {
                FragmentNavigatorExtras(sharedImageView to "stylist_image_$trimmedId")
            } else null
            
            if (extras != null) {
                findNavController().navigate(
                    R.id.action_home_to_details,
                    bundleOf("stylistId" to trimmedId),
                    null,
                    extras
                )
            } else {
                findNavController().navigate(
                    R.id.action_home_to_details,
                    bundleOf("stylistId" to trimmedId)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        _binding?.mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding?.mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }
}