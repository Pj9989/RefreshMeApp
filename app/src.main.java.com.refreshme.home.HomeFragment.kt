package com.refreshme.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.refreshme.R
import com.refreshme.data.Stylist

class HomeFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private val homeViewModel: HomeViewModel by viewModels()

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getUserLocation()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val greetingTextView: TextView = view.findViewById(R.id.greeting_text)
        homeViewModel.userName.observe(viewLifecycleOwner, Observer { name ->
            greetingTextView.text = name
        })

        val stylistsRecyclerView: RecyclerView = view.findViewById(R.id.live_stylists_recycler_view)
        stylistsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        homeViewModel.stylists.observe(viewLifecycleOwner, Observer { stylists ->
            stylistsRecyclerView.adapter = StylistPreviewAdapter(stylists) { stylist ->
                navigateToStylistProfile(stylist.id)
            }
            updateMapMarkers(stylists)
        })

        mapView = view.findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkLocationPermission()
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
                }
            }
        } catch (e: SecurityException) {
            // SecurityException handled
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setOnMarkerClickListener(this)
        homeViewModel.stylists.value?.let { updateMapMarkers(it) }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val stylistId = marker.tag as? String
        stylistId?.let {
            navigateToStylistProfile(it)
        }
        return true // Consume the event
    }

    private fun updateMapMarkers(stylists: List<Stylist>) {
        googleMap?.clear()
        stylists.forEach { stylist ->
            stylist.location?.let { geoPoint ->
                val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                val markerIcon = if (stylist.isAvailable) {
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_available)
                } else {
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_unavailable)
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
    
    private fun navigateToStylistProfile(stylistId: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToStylistProfileFragment(stylistId)
        findNavController().navigate(action)
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}