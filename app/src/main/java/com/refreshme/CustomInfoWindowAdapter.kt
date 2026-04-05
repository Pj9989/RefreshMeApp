package com.refreshme

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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
        // Note: Buttons inside InfoWindows are not clickable by default in Google Maps API.
        // The click event is handled by setOnInfoWindowClickListener in the Fragment.

        val stylist = stylists.find { it.name == marker.title }
        if (stylist != null) {
            stylistName.text = stylist.name
            
            // Show Online status
            if (stylist.isOnline == true) {
                // If the stylist is online, show a green indicator
                stylistSpecialty.text = "🟢 Online Now • ${stylist.specialty ?: ""}"
                stylistSpecialty.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                // If the stylist is offline, show standard text
                stylistSpecialty.text = "Offline • ${stylist.specialty ?: ""}"
                stylistSpecialty.setTextColor(Color.GRAY)
            }
        }

        return view
    }
}