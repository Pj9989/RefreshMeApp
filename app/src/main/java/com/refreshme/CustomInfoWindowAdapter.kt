package com.refreshme

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.refreshme.data.Stylist

class CustomInfoWindowAdapter(
    private val context: Context,
    private val stylists: List<Stylist>
) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun getInfoContents(marker: Marker): View {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
        val stylistName = view.findViewById<TextView>(R.id.stylist_name)
        val stylistSpecialty = view.findViewById<TextView>(R.id.stylist_specialty)
        val directionsButton = view.findViewById<Button>(R.id.directions_button)

        val stylist = stylists.find { it.name == marker.title }
        if (stylist != null) {
            stylistName.text = stylist.name
            stylistSpecialty.text = stylist.specialty
        }

        return view
    }
}