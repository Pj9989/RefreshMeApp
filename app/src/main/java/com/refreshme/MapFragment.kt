package com.refreshme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshme.data.Stylist

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val allStylists = mutableListOf<Stylist>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val searchEditText = view.findViewById<TextInputEditText>(R.id.search_edit_text)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterStylists(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val nearMeButton = view.findViewById<Button>(R.id.near_me_button)
        nearMeButton.setOnClickListener {
            findNearbyStylists()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            mMap = googleMap
            val style = MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_gold)
            mMap.setMapStyle(style)
            mMap.uiSettings.isZoomControlsEnabled = true
            checkLocationPermission()
            fetchStylists()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.8283, -98.5795), 3f))

            mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(requireContext(), allStylists))
            mMap.setOnInfoWindowClickListener { marker ->
                val stylist = allStylists.find { it.name == marker.title }
                if (stylist?.location != null) {
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
            mMap.isMyLocationEnabled = true
            getLastKnownLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12f))
                }
            }
    }

    private fun fetchStylists() {
        firestore.collection("stylists").whereEqualTo("isVerified", true).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    return@addOnSuccessListener
                }
                allStylists.clear()
                for (document in documents) {
                    val stylist = document.toObject(Stylist::class.java).copy(id = document.id)
                    allStylists.add(stylist)
                }
                updateMapWithStylists(allStylists)
            }
            .addOnFailureListener { exception ->
                Log.w("MapFragment", "Error getting documents: ", exception)
            }
    }

    private fun filterStylists(query: String) {
        val filteredList = if (query.isEmpty()) {
            allStylists
        } else {
            allStylists.filter {
                it.name?.contains(query, ignoreCase = true) == true ||
                        it.specialty?.contains(query, ignoreCase = true) == true
            }
        }
        updateMapWithStylists(filteredList)
    }

    @SuppressLint("MissingPermission")
    private fun findNearbyStylists() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    val nearbyStylists = allStylists.filter {
                        val stylistLocation = it.location
                        if (stylistLocation != null) {
                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                userLocation.latitude, userLocation.longitude,
                                stylistLocation.latitude, stylistLocation.longitude,
                                distance
                            )
                            distance[0] / 1000 <= 10 // 10km radius
                        } else {
                            false
                        }
                    }
                    updateMapWithStylists(nearbyStylists)
                }
            }
    }

    private fun updateMapWithStylists(stylists: List<Stylist>) {
        mMap.clear()
        if (stylists.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            var hasLocations = false
            for (stylist in stylists) {
                if (stylist.location != null) {
                    hasLocations = true
                    val latLng = LatLng(stylist.location.latitude, stylist.location.longitude)
                    mMap.addMarker(MarkerOptions().position(latLng).title(stylist.name))
                    boundsBuilder.include(latLng)
                }
            }
            if (hasLocations) {
                val bounds = boundsBuilder.build()
                val padding = 100
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}