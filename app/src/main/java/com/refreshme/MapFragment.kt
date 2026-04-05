package com.refreshme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.auth.SignInActivity
import com.refreshme.data.Stylist
import com.refreshme.databinding.FragmentMapBinding

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val allStylists = mutableListOf<Stylist>()
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    
    // Filter state
    private var showOnlineOnly = false
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setupAuthStateListener()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString()
                mMap?.let { applyFilters() }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.nearMeButton.text = "Show Online Now"
        binding.nearMeButton.setOnClickListener {
            showOnlineOnly = !showOnlineOnly
            binding.nearMeButton.text = if (showOnlineOnly) "Show All" else "Show Online Now"
            mMap?.let { applyFilters() }
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                val intent = Intent(requireActivity(), SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            mMap = googleMap
            
            val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isDarkMode) {
                val style = MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_gold)
                mMap?.setMapStyle(style)
            } else {
                mMap?.setMapStyle(null)
            }
            
            mMap?.uiSettings?.isZoomControlsEnabled = true

            if (auth.currentUser != null) {
                loadMapData()
            }

            mMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(requireContext(), allStylists))
            mMap?.setOnInfoWindowClickListener { marker ->
                val stylist = allStylists.find { it.name == marker.title }

                if (stylist?.id != null) {
                    findNavController().navigate(
                        R.id.action_map_to_details,
                        bundleOf("stylistId" to stylist.id)
                    )
                } else if (stylist?.location != null) {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${stylist.location.latitude},${stylist.location.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Error initializing map", e)
        }
    }

    private fun loadMapData() {
        checkLocationPermission()
        fetchStylists()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            mMap?.isMyLocationEnabled = true
            getLastKnownLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        val targetStylistId = arguments?.getString("stylistId")
        if (targetStylistId != null) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) { 
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f))
                }
            }
    }

    private fun fetchStylists() {
        // Only show stylists who are currently online AND verified — the core promise of RefreshMe.
        firestore.collection("stylists")
            .whereEqualTo("online", true)
            .whereEqualTo("verified", true)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }
                allStylists.clear()
                for (document in documents) {
                    val stylist = document.toObject(Stylist::class.java).copy(id = document.id)
                    allStylists.add(stylist)
                }
                applyFilters()
            }
            .addOnFailureListener { exception ->
                Log.w("MapFragment", "Error getting documents: ", exception)
            }
    }

    private fun applyFilters() {
        val query = searchQuery
        var filteredList = allStylists.toList()
        
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.name?.contains(query, ignoreCase = true) == true ||
                        it.specialty?.contains(query, ignoreCase = true) == true
            }
        }
        
        if (showOnlineOnly) {
            filteredList = filteredList.filter { it.isOnline == true }
        }

        updateMapWithStylists(filteredList)
    }

    private fun updateMapWithStylists(stylists: List<Stylist>) {
        val currentMap = mMap ?: return
        currentMap.clear()
        
        val targetStylistId = arguments?.getString("stylistId")
        var targetMarker: LatLng? = null
        
        if (stylists.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            var hasLocations = false
            for (stylist in stylists) {
                if (stylist.location != null) {
                    hasLocations = true
                    val latLng = LatLng(stylist.location.latitude, stylist.location.longitude)
                    val markerOptions = MarkerOptions().position(latLng).title(stylist.name)
                    
                    if (stylist.isOnline == true) {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        markerOptions.zIndex(1.0f)
                    } else {
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        markerOptions.zIndex(0.0f)
                    }
                    
                    val marker = currentMap.addMarker(markerOptions)
                    boundsBuilder.include(latLng)
                    
                    if (stylist.id == targetStylistId) {
                        targetMarker = latLng
                        marker?.showInfoWindow()
                    }
                }
            }
            
            if (hasLocations) {
                if (targetMarker != null) {
                     currentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetMarker, 15f))
                } else if (targetStylistId == null) {
                    try {
                        val bounds = boundsBuilder.build()
                        val padding = 100
                        currentMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mMap = null
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}