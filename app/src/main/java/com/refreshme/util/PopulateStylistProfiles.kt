package com.refreshme.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.refreshme.data.Appointment
import com.refreshme.data.Stylist
import com.refreshme.data.WorkingHours
import java.util.Calendar

object PopulateStylistProfiles {

    fun populateData() {
        val firestore = FirebaseFirestore.getInstance()
        val stylistsCollection = firestore.collection("stylists")

        val stylists = listOf(
            Stylist(
                name = "Jenna 'Cuts' Smith",
                specialty = "Fades & Tapers",
                rating = 4.9,
                reviewCount = 120,
                isVerified = true,
                isOnline = true,
                location = GeoPoint(34.0522, -118.2437),
                profileImageUrl = "https://images.pexels.com/photos/3993324/pexels-photo-3993324.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                workingHours = listOf(
                    WorkingHours(2, "09:00", "17:00"), // Monday
                    WorkingHours(3, "09:00", "17:00"), // Tuesday
                    WorkingHours(4, "09:00", "17:00"), // Wednesday
                    WorkingHours(5, "09:00", "17:00"), // Thursday
                    WorkingHours(6, "09:00", "17:00")  // Friday
                ),
                bookedAppointments = listOf(
                    Appointment(getTodayMillis(10, 0), 60),
                    Appointment(getTodayMillis(14, 0), 90)
                )
            ),
            Stylist(
                name = "Marco 'The Razor' Diaz",
                specialty = "Beard Trims",
                rating = 4.8,
                reviewCount = 85,
                isVerified = false,
                isOnline = true,
                location = GeoPoint(34.0532, -118.2447),
                profileImageUrl = "https://images.pexels.com/photos/1813272/pexels-photo-1813272.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                workingHours = listOf(
                    WorkingHours(2, "10:00", "18:00"),
                    WorkingHours(3, "10:00", "18:00"),
                    WorkingHours(4, "10:00", "18:00"),
                    WorkingHours(5, "10:00", "18:00"),
                    WorkingHours(6, "10:00", "18:00")
                )
            ),
            Stylist(
                name = "Isabella 'Bella' Rossi",
                specialty = "Modern Styles",
                rating = 4.9,
                reviewCount = 150,
                isVerified = true,
                isOnline = true,
                location = GeoPoint(34.0542, -118.2457),
                profileImageUrl = "https://images.pexels.com/photos/3998419/pexels-photo-3998419.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
                workingHours = listOf(
                    WorkingHours(2, "08:00", "16:00"),
                    WorkingHours(3, "08:00", "16:00"),
                    WorkingHours(4, "08:00", "16:00")
                )
            ),
            Stylist(
                name = "Carlos 'The Clipper' Jones",
                specialty = "Classic Cuts",
                rating = 4.7,
                reviewCount = 95,
                isVerified = false,
                isOnline = true,
                location = GeoPoint(34.0552, -118.2467),
                profileImageUrl = "https://images.pexels.com/photos/428364/pexels-photo-428364.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
            )
        )

        stylists.forEach { stylist ->
            stylistsCollection.add(stylist)
                .addOnSuccessListener { Log.d("PopulateData", "Stylist added: ${stylist.name}") }
                .addOnFailureListener { e -> Log.w("PopulateData", "Error adding stylist", e) }
        }
    }

    private fun getTodayMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}